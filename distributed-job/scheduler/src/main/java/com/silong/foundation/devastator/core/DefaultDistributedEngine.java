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

import com.lmax.disruptor.LiteBlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.silong.foundation.devastator.*;
import com.silong.foundation.devastator.config.DevastatorConfig;
import com.silong.foundation.devastator.config.DevastatorProperties;
import com.silong.foundation.devastator.event.ViewChangedEvent;
import com.silong.foundation.devastator.exception.InitializationException;
import com.silong.foundation.devastator.handler.ViewChangedEventHandler;
import com.silong.foundation.devastator.model.Devastator.ClusterNodeInfo;
import com.silong.foundation.devastator.model.Devastator.ClusterState;
import com.silong.foundation.devastator.model.Devastator.HardwareInfo;
import com.silong.foundation.devastator.model.Devastator.JvmInfo;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jgroups.*;
import org.jgroups.protocols.TP;
import org.jgroups.protocols.UDP;
import org.jgroups.protocols.pbcast.GMS;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;

import static com.lmax.disruptor.dsl.ProducerType.MULTI;
import static com.silong.foundation.devastator.core.ClusterNodeUUID.deserialize;
import static com.silong.foundation.devastator.core.DefaultMembershipChangePolicy.INSTANCE;
import static com.silong.foundation.devastator.utils.TypeConverter.STRING_TO_BYTES;
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
@SuppressFBWarnings({"PATH_TRAVERSAL_IN", "URLCONNECTION_SSRF_FD"})
public class DefaultDistributedEngine
    implements DistributedEngine, Receiver, ChannelListener, Serializable {

  @Serial private static final long serialVersionUID = 0L;

  /** 分区到节点映射关系，Collection中的第一个节点为primary，后续为backup */
  @Getter
  private final Map<Integer, Collection<ClusterNode>> partition2ClusterNodes =
      new ConcurrentHashMap<>();

  /** 集群通信信道 */
  private final JChannel jChannel;

  /** 配置 */
  private final DevastatorConfig config;

  /** 数据分配器 */
  @Getter private final RendezvousAllocator allocator;

  /** 持久化存储 */
  private final PersistStorage persistStorage;

  /** 事件变更消息队列 */
  private final Disruptor<ViewChangedEvent> viewChangedEventDisruptor;

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
      this.allocator = new RendezvousAllocator(getPartitionCount());
      this.persistStorage = new RocksDbPersistStorage(config.persistStorageConfig());
      this.viewChangedEventDisruptor =
          buildViewChangedEventDisruptor(config.viewChangedEventQueueSize());
      this.viewChangedEventDisruptor.start();
      configureDistributedEngine(
          this.jChannel = buildDistributedEngine(config.configFile()), config);

      // 启动引擎
      this.jChannel.connect(config.clusterName());
    } catch (Exception e) {
      throw new InitializationException("Failed to start distributed engine.", e);
    }
  }

  private void configureDistributedEngine(JChannel jChannel, DevastatorConfig config) {
    jChannel.setReceiver(this);
    jChannel.addAddressGenerator(this::buildAddressGenerator);
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

  private ThreadFactory viewChangedEventThreadFactory() {
    return r -> new Thread(r, "view-changed-event-processor");
  }

  private Disruptor<ViewChangedEvent> buildViewChangedEventDisruptor(int queueSize) {
    Disruptor<ViewChangedEvent> disruptor =
        new Disruptor<>(
            ViewChangedEvent::new,
            queueSize,
            viewChangedEventThreadFactory(),
            MULTI,
            new LiteBlockingWaitStrategy());
    disruptor.handleEventsWith(new ViewChangedEventHandler(this));
    return disruptor;
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

  @SneakyThrows
  private ClusterNodeUUID buildAddressGenerator() {
    byte[] key = STRING_TO_BYTES.to(getClusterNodeUuidKey());
    byte[] value = persistStorage.get(key);
    ClusterNodeUUID uuid;
    if (value != null) {
      uuid = deserialize(value);
    } else {
      uuid = ClusterNodeUUID.random();

      // 保存uuid
      persistStorage.put(key, uuid.serialize());
    }
    // 更新节点附加信息
    return uuid.clusterNodeInfo(buildClusterNodeInfo());
  }

  private ClusterNodeInfo buildClusterNodeInfo() {
    RuntimeMXBean mxbean = ManagementFactory.getRuntimeMXBean();
    return ClusterNodeInfo.newBuilder()
        .setJgVersion(Version.version)
        .setPid(mxbean.getPid())
        .setClusterName(clusterName())
        .setStartTime(mxbean.getStartTime())
        .setJvmInfo(buildJvmInfo(mxbean))
        .setHardwareInfo(buildHardwareInfo())
        .setDevastatorVersion(DevastatorProperties.version().getVersion())
        .putAllAttributes(config.clusterNodeAttributes())
        .setInstanceName(config.instanceName())
        .setHostName(getHostName())
        .setRole(config.clusterNodeRole().getValue())
        .addAllIpAddresses(getLocalAllAddresses())
        .build();
  }

  private HardwareInfo.Builder buildHardwareInfo() {
    Runtime runtime = Runtime.getRuntime();
    return HardwareInfo.newBuilder()
        .setAvailableProcessors(runtime.availableProcessors())
        .setTotalMemory(runtime.totalMemory());
  }

  private JvmInfo.Builder buildJvmInfo(RuntimeMXBean mxbean) {
    return JvmInfo.newBuilder()
        .setVmVendor(mxbean.getVmVendor())
        .setVmVersion(mxbean.getVmVersion())
        .setVmName(mxbean.getVmName())
        .setClassPath(String.join(";", mxbean.getClassPath()))
        .setVmArgs(String.join(" ", mxbean.getInputArguments()));
  }

  @SneakyThrows
  private Collection<String> getLocalAllAddresses() {
    List<InetAddress> list = new LinkedList<>();
    Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
    while (interfaces.hasMoreElements()) {
      NetworkInterface networkInterface = interfaces.nextElement();
      Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
      while (inetAddresses.hasMoreElements()) {
        list.add(inetAddresses.nextElement());
      }
    }
    return list.stream().map(InetAddress::getHostAddress).toList();
  }

  /**
   * 构造节点持久化key
   *
   * @return key
   */
  private String getClusterNodeUuidKey() {
    return String.format(
        "%s:%s:%s:node:uuid", config.clusterName(), getHostName(), config.instanceName());
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
      log.debug("topology: {}", newView);
    }
    RingBuffer<ViewChangedEvent> ringBuffer = viewChangedEventDisruptor.getRingBuffer();
    long seq = ringBuffer.next();
    try {
      ringBuffer.get(seq).oldView(lastView).newview(newView);
    } finally {
      lastView = newView;
      ringBuffer.publish(seq);
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
    viewChangedEventDisruptor.shutdown();
    partition2ClusterNodes.clear();
    if (persistStorage != null) {
      persistStorage.close();
    }
  }
}
