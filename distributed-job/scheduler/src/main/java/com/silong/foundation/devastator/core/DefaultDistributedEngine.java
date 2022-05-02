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
import com.silong.foundation.devastator.message.PooledBytesMessage;
import com.silong.foundation.devastator.message.PooledNioMessage;
import com.silong.foundation.devastator.model.Devastator.ClusterNodeInfo;
import com.silong.foundation.devastator.model.Devastator.ClusterState;
import com.silong.foundation.devastator.model.SimpleClusterNode;
import com.silong.foundation.devastator.model.Tuple;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import static com.silong.foundation.devastator.core.DefaultMembershipChangePolicy.INSTANCE;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.SystemUtils.getHostName;
import static org.rocksdb.util.SizeUnit.KB;

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

  /** 集群通信信道 */
  private final JChannel jChannel;

  /** 配置 */
  private final DevastatorConfig config;

  /** 分区节点映射器 */
  private final RendezvousPartitionMapping partitionMapping;

  /** 持久化存储 */
  private final PersistStorage persistStorage;

  /** 集群视图变更处理器 */
  private final ViewChangedHandler viewChangedHandler;

  /** 分区到节点映射关系，Collection中的第一个节点为primary，后续为backup */
  private Map<Integer, Collection<SimpleClusterNode>> partition2ClusterNodes;

  /** 数据uuid到分区的映射关系 */
  private final Map<Object, Integer> uuid2Partitions;

  /** 对象分区映射器 */
  private ObjectPartitionMapping objectPartitionMapping;

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
      this.uuid2Partitions = new ConcurrentHashMap<>((int) KB);
      this.partitionMapping = RendezvousPartitionMapping.INSTANCE;
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

    // 注册定制消息
    registerMessages(jChannel);

    // 自定义集群节点策略
    ((GMS) jChannel.getProtocolStack().findProtocol(GMS.class)).setMembershipChangePolicy(INSTANCE);
  }

  /** 注册自定义消息类型 */
  private void registerMessages(JChannel jChannel) {
    MessageFactory messageFactory = getMessageFactory(jChannel);
    PooledNioMessage.register(messageFactory);
    PooledBytesMessage.register(messageFactory);
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
  private TP getTransport() {
    return jChannel.getProtocolStack().getTransport();
  }

  /**
   * 获取消息工厂
   *
   * @return 消息工程
   */
  private MessageFactory getMessageFactory(JChannel jChannel) {
    return jChannel.getProtocolStack().getTransport().getMessageFactory();
  }

  /** 同步集群状态 */
  public void syncClusterState() throws Exception {
    // 向coord请求同步集群状态
    jChannel.getState(null, config.clusterStateSyncTimeout());
  }

  /**
   * 数据到分区映射
   *
   * @param obj 数据对象
   * @return {@code this}
   * @param <T> 数据类型
   */
  public synchronized <T extends Comparable<T>> DefaultDistributedEngine partition(
      ObjectIdentity<T> obj) {
    assert obj != null;
    T key = obj.uuid();
    uuid2Partitions.put(key, objectPartitionMapping.partition(key));
    return this;
  }

  /**
   * 根据新的集群视图调整分区表
   *
   * @param view 集群视图
   */
  public synchronized void repartition(View view) {
    Map<Integer, Collection<SimpleClusterNode>> oldPartition2ClusterNodes = partition2ClusterNodes;

    int partitionCount = getPartitionCount();
    if (partition2ClusterNodes == null) {
      // 分区表key数量在没有集群节点变化时是固定的，并且每个分区号都不同，此处设置初始容量大于最大容量，配合负载因子为1，避免rehash
      partition2ClusterNodes = new ConcurrentHashMap<>(partitionCount, 1.0f);
      objectPartitionMapping = new DefaultObjectPartitionMapping(partitionCount);
    }

    // 如果分区数变化则重置分区映射器
    if (objectPartitionMapping.partitions() != partitionCount) {
      objectPartitionMapping.partitions(partitionCount);
      partition2ClusterNodes = new ConcurrentHashMap<>(partitionCount, 1.0f);
    }

    // 获取集群节点列表
    Collection<SimpleClusterNode> clusterNodes =
        view.getMembers().stream().map(SimpleClusterNode::new).toList();

    // 根据集群节点调整分区分布
    IntStream.range(0, partitionCount)
        .parallel()
        .forEach(
            partitionNo ->
                partition2ClusterNodes.put(
                    partitionNo,
                    partitionMapping.allocatePartition(
                        partitionNo, getBackupNums(), clusterNodes, null)));

    if (oldPartition2ClusterNodes != null) {
      SimpleClusterNode localNode = new SimpleClusterNode(jChannel.getAddress());
      LinkedList<Tuple<Integer, Boolean>> newLocal = new LinkedList<>();
      partition2ClusterNodes.forEach(
          (k, v) -> {
            if (Collections.binarySearch(v, localNode)) {}
          });
    }
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

  /**
   * Sends a message. The message contains
   *
   * <ol>
   *   <li>a destination address (Address). A {@code null} address sends the message to all cluster
   *       members.
   *   <li>a source address. Can be left empty as it will be assigned automatically
   *   <li>a byte buffer. The message contents.
   *   <li>several additional fields. They can be used by application programs (or patterns). E.g. a
   *       message ID, flags etc
   * </ol>
   *
   * @param message the message to be sent. Destination and buffer should be set. A null destination
   *     means to send to all group members.
   * @return {@code this}
   * @exception Exception thrown if the channel is disconnected or closed
   */
  public DistributedEngine send(Message message) throws Exception {
    assert message != null;
    jChannel.send(message);
    return this;
  }

  @Override
  public void receive(Message message) {
    assert message != null;
    if (message instanceof BytesMessage) {

    } else if (message instanceof NioMessage) {

    } else if (message instanceof ObjectMessage) {

    }
  }

  @Override
  public void viewAccepted(View newView) {
    assert newView != null;
    if (log.isDebugEnabled()) {
      log.debug("A new view is received {}.", newView);
    }
    try {
      // 异步处理集群视图变更
      viewChangedHandler.handle(lastView, newView);
    } finally {
      // 更新视图
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
    assert view != null;
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
    if (viewChangedHandler != null) {
      viewChangedHandler.close();
    }
    if (uuid2Partitions != null) {
      uuid2Partitions.clear();
    }
    if (partition2ClusterNodes != null) {
      partition2ClusterNodes.clear();
    }
    if (persistStorage != null) {
      persistStorage.close();
    }
  }
}
