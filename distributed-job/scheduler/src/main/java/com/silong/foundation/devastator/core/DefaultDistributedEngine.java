/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.silong.foundation.devastator.core;

import com.silong.foundation.devastator.*;
import com.silong.foundation.devastator.config.DevastatorConfig;
import com.silong.foundation.devastator.exception.InitializationException;
import com.silong.foundation.devastator.handler.ViewChangedHandler;
import com.silong.foundation.devastator.model.Devastator.ClusterNodeInfo;
import com.silong.foundation.devastator.model.Devastator.ClusterState;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jgroups.*;
import org.jgroups.protocols.TP;
import org.jgroups.protocols.UDP;
import org.jgroups.protocols.pbcast.GMS;

import java.io.*;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static com.silong.foundation.devastator.core.DefaultMembershipChangePolicy.INSTANCE;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.SystemUtils.getHostName;

/**
 * 基于jgroups的分布式任务引擎
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-10 00:30
 */
@Slf4j
@Accessors(fluent = true)
@SuppressFBWarnings({"PATH_TRAVERSAL_IN", "URLCONNECTION_SSRF_FD"})
public class DefaultDistributedEngine
    implements DistributedEngine, Receiver, ChannelListener, Serializable {

  @Serial private static final long serialVersionUID = 0L;

  /** 分区到节点映射关系，Collection中的第一个节点为primary，后续为backup */
  private final Map<Integer, Collection<ClusterNode>> partition2ClusterNodes = new HashMap<>();

  /** 集群通信信道 */
  private final JChannel jChannel;

  /** 配置 */
  private final DevastatorConfig config;

  /** 分区节点映射器 */
  private final RendezvousPartitionMapping partitionMapping;

  /** 对象分区映射器 */
  private final ObjectPartitionMapping objectPartitionMapping;

  /** 持久化存储 */
  private final PersistStorage persistStorage;

  /** 集群视图变更处理器 */
  private final ViewChangedHandler viewChangedHandler;

  /** 集群状态 */
  private volatile ClusterState clusterState;

  /** 最后一个集群视图 */
  private volatile View lastView;

  /**
   * 构造方法
   *
   * @param config 引擎配置
   */
  public DefaultDistributedEngine(DevastatorConfig config) {
    if (config == null) {
      throw new IllegalArgumentException("config must not be null.");
    }

    try {
      this.config = config;
      this.partitionMapping = RendezvousPartitionMapping.INSTANCE;
      this.objectPartitionMapping = new DefaultObjectPartitionMapping(config.partitionCount());
      this.persistStorage = new RocksDbPersistStorage(config.persistStorageConfig());
      this.viewChangedHandler = new ViewChangedHandler(this);
      configureDistributedEngine(
          this.jChannel = buildDistributedEngine(config.configFile()), config, persistStorage);

      // 启动引擎
      this.jChannel.connect(config.clusterName());
    } catch (Exception e) {
      throw new InitializationException("Failed to start distributed engine.", e);
    }
  }

  private void configureDistributedEngine(
      JChannel jChannel, DevastatorConfig config, PersistStorage persistStorage) {
    jChannel.setReceiver(this);
    jChannel.addAddressGenerator(new DefaultAddressGenerator(config, persistStorage));
    jChannel.setDiscardOwnMessages(true);
    jChannel.addChannelListener(this);
    jChannel.setName(config.instanceName());

    // 自定义集群节点策略
    ((GMS) jChannel.getProtocolStack().findProtocol(GMS.class)).setMembershipChangePolicy(INSTANCE);
  }

  private JChannel buildDistributedEngine(String configFile) throws Exception {
    URL configUrl;
    try {
      configUrl = locateConfig(configFile);
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException(String.format("Failed to load %s.", configFile), e);
    }
    try (InputStream inputStream = configUrl.openStream()) {
      return new JChannel(inputStream);
    }
  }

  private URL locateConfig(String confFile) throws MalformedURLException {
    try {
      return new URL(confFile);
    } catch (MalformedURLException e) {
      File file = new File(confFile);
      if (file.exists() && file.isFile()) {
        return file.toURI().toURL();
      } else {
        return getClass().getClassLoader().getResource(confFile);
      }
    }
  }

  /**
   * 获取分区数，优先从集群状态取
   *
   * @return 分区数
   */
  public int getPartitionCount() {
    return clusterState == null ? config.partitionCount() : clusterState.getPartitions();
  }

  /**
   * 获取数据备份数
   *
   * @return 数据备份数
   */
  public int getBackupNums() {
    return clusterState == null ? config.backupNums() : clusterState.getBackupNums();
  }

  /**
   * 获取传输协议
   *
   * @return 传输协议
   */
  public TP getTransport() {
    return jChannel.getProtocolStack().getTransport();
  }

  /** 同步集群状态 */
  public void syncClusterState() throws Exception {
    // 向coord请求同步集群状态
    jChannel.getState(null, config.clusterStateSyncTimeout());

    // 如果分区数变化则重置分区映射器
    int partitionCount = getPartitionCount();
    if (partitionMapping.partitions() != partitionCount) {
      partitionMapping.setPartitions(partitionCount);
    }

    // 如果集群分区数量变化，导致当前节点分区数量大于集群分区数量，则先清空，待后续重新映射
    if (partition2ClusterNodes.size() > partitionCount) {
      partition2ClusterNodes.clear();
    }
  }

  /**
   * 根据新的集群视图调整分区表
   *
   * @param view 集群视图
   */
  public void repartition(View view) {
    // 获取集群节点列表
    Collection<ClusterNode> clusterNodes = getClusterNodes(view);

    // 根据集群节点调整分区分布
    IntStream.range(0, getPartitionCount())
        .parallel()
        .forEach(
            partitionNo ->
                partition2ClusterNodes.put(
                    partitionNo,
                    partitionMapping.allocatePartition(
                        partitionNo, getBackupNums(), clusterNodes, null)));
  }

  /**
   * 获取集群绑定的通讯地址
   *
   * @return 绑定地址
   */
  public InetAddress bindAddress() {
    TP transport = getTransport();
    return transport.getClass() == UDP.class
        ? ((UDP) transport).getMulticastAddress()
        : transport.getBindAddress();
  }

  /**
   * 获取绑定端口
   *
   * @return 绑定端口
   */
  public int bindPort() {
    TP transport = getTransport();
    return transport.getClass() == UDP.class
        ? ((UDP) transport).getMulticastPort()
        : transport.getBindPort();
  }

  /**
   * 集群名
   *
   * @return 集群名
   */
  public String clusterName() {
    return config.clusterName();
  }

  @Override
  public void receive(Message msg) {}

  @Override
  public void viewAccepted(View newView) {
    if (log.isDebugEnabled()) {
      log.debug("A new view is received {}.", newView);
    }
    try {
      viewChangedHandler.handle(lastView, newView);
    } finally {
      lastView = newView;
    }
  }

  /**
   * 获取集群节点列表
   *
   * @param view 集群视图
   * @return 集群节点列表
   */
  public List<ClusterNode> getClusterNodes(View view) {
    if (view == null) {
      throw new IllegalArgumentException("view must not be null.");
    }
    return view.getMembers().stream().map(this::buildClusterNode).toList();
  }

  private ClusterNode buildClusterNode(Address address) {
    return new DefaultClusterNode((ClusterNodeUUID) address, jChannel.address());
  }

  @Override
  public void getState(OutputStream output) throws Exception {
    if (clusterState == null) {
      clusterState =
          ClusterState.newBuilder()
              .setPartitions(config.partitionCount())
              .setBackupNums(config.backupNums())
              .build();
    }
    clusterState.writeTo(output);
    log.info("The node{} sends the clusterState:[{}]", localIdentity(), clusterState);
  }

  @Override
  public void setState(InputStream input) throws Exception {
    clusterState = ClusterState.parseFrom(input);
    log.info("The node{} receives the clusterState:[{}]", localIdentity(), clusterState);
  }

  @Override
  public DevastatorConfig config() {
    return config;
  }

  @Override
  public Cluster cluster() {
    return new DefaultCluster(this);
  }

  /**
   * 当前集群视图
   *
   * @return 视图
   */
  public View currentView() {
    return jChannel.view();
  }

  @Override
  public DistributedJobScheduler scheduler(String name) {
    return null;
  }

  @Override
  public void close() {
    if (jChannel != null) {
      jChannel.close();
    }
  }

  private String localIdentity() {
    return String.format(
        "(%s:%s|%s:%d)",
        getHostName(), config.instanceName(), bindAddress().getHostAddress(), bindPort());
  }

  private String printViewMembers(JChannel channel) {
    return channel.getView().getMembers().stream()
        .map(
            address -> {
              ClusterNodeInfo clusterNodeInfo = ((ClusterNodeUUID) address).clusterNodeInfo();
              return String.format(
                  "(%s:%s)", clusterNodeInfo.getHostName(), clusterNodeInfo.getInstanceName());
            })
        .collect(joining(",", "[", "]"));
  }

  @Override
  public void channelConnected(JChannel channel) {
    log.info(
        "The node{} has successfully joined {} with topology:{}.",
        localIdentity(),
        channel.clusterName(),
        printViewMembers(channel));
  }

  @Override
  public void channelDisconnected(JChannel channel) {
    log.info("The node{} has left {}.", localIdentity(), config.clusterName());
  }

  @Override
  @SneakyThrows
  public void channelClosed(JChannel channel) {
    log.info("The node{} has been shutdown.", localIdentity());
    viewChangedHandler.close();
    partition2ClusterNodes.clear();
    if (persistStorage != null) {
      persistStorage.close();
    }
  }
}
