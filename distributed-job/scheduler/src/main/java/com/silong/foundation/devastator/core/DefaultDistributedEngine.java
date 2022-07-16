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

import com.google.protobuf.InvalidProtocolBufferException;
import com.silong.foundation.devastator.Cluster;
import com.silong.foundation.devastator.ClusterNode;
import com.silong.foundation.devastator.DistributedEngine;
import com.silong.foundation.devastator.DistributedJobScheduler;
import com.silong.foundation.devastator.config.DevastatorConfig;
import com.silong.foundation.devastator.config.ScheduledExecutorConfig;
import com.silong.foundation.devastator.exception.DistributedEngineException;
import com.silong.foundation.devastator.message.PooledBytesMessage;
import com.silong.foundation.devastator.message.PooledNioMessage;
import com.silong.foundation.devastator.model.ClusterNodeUUID;
import com.silong.foundation.devastator.model.Devastator.ClusterNodeInfo;
import com.silong.foundation.devastator.model.Devastator.ClusterState;
import com.silong.foundation.devastator.model.Devastator.JobMsgPayload;
import com.silong.foundation.utilities.concurrent.SimpleThreadFactory;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jgroups.*;
import org.jgroups.auth.AuthToken;
import org.jgroups.protocols.AUTH;
import org.jgroups.protocols.TP;
import org.jgroups.protocols.UDP;
import org.jgroups.protocols.pbcast.GMS;
import org.jgroups.util.ByteArrayDataOutputStream;
import org.jgroups.util.MessageBatch;

import java.io.*;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.silong.foundation.devastator.utils.Utilities.powerOf2;
import static java.lang.System.lineSeparator;
import static java.lang.ThreadLocal.withInitial;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.SystemUtils.getHostName;
import static org.jgroups.Global.LONG_SIZE;

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
class DefaultDistributedEngine
    implements DistributedEngine, Receiver, ChannelListener, Serializable, AutoCloseable {

  @Serial private static final long serialVersionUID = 1258279194145465487L;

  /** 分区数据同步线程名前缀 */
  private static final String PARTITION_SYNC_THREAD_NAME_PREFIX = "partition-sync-processor";

  /** address缓存 */
  private static final ThreadLocal<ByteArrayDataOutputStream> ADDRESS_BUFFER =
      withInitial(() -> new ByteArrayDataOutputStream(LONG_SIZE * 2));

  /** 集群视图变更处理器 */
  private final DefaultViewChangedHandler defaultViewChangedHandler;

  /** 集群胸袭处理器 */
  private final DefaultMessageHandler[] defaultMessageHandlers;

  /** 分布式任务调度器 */
  private final Map<String, DistributedJobScheduler> distributedJobSchedulerMap;

  /** 集群通信信道 */
  private final JChannel jChannel;

  /** 分区同步任务执行器 */
  final Executor partitionSyncExecutor;

  /** 配置 */
  final DevastatorConfig config;

  /** 分区节点映射器 */
  final RendezvousPartitionMapping partitionMapping;

  /** 持久化存储 */
  final RocksDbPersistStorage persistStorage;

  /** 分布式元数据 */
  final DistributedDataMetadata metadata;

  /** 对象分区映射器 */
  final DefaultObject2PartitionMapping objectPartitionMapping;

  /** 集群状态 */
  private volatile ClusterState clusterState;

  /** 最后一个集群视图 */
  private volatile View lastView;

  private final int messageEventQueueMark;

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
      this.partitionSyncExecutor =
          Executors.newFixedThreadPool(
              powerOf2(config.partitionSyncThreadCount()),
              new SimpleThreadFactory(PARTITION_SYNC_THREAD_NAME_PREFIX));
      this.objectPartitionMapping = new DefaultObject2PartitionMapping(config.partitionCount());
      // 此处设置初始容量大于最大容量，配合负载因子为1，避免rehash
      this.metadata = new DistributedDataMetadata(this);
      this.distributedJobSchedulerMap =
          new ConcurrentHashMap<>(config.scheduledExecutorConfigs().size());
      this.partitionMapping = RendezvousPartitionMapping.INSTANCE;
      this.persistStorage = new RocksDbPersistStorage(config.persistStorageConfig());
      this.defaultViewChangedHandler = new DefaultViewChangedHandler(this);
      this.defaultMessageHandlers =
          new DefaultMessageHandler[powerOf2(config.messageEventQueueCount())];
      this.messageEventQueueMark = defaultMessageHandlers.length - 1;
      for (int i = 0; i < defaultMessageHandlers.length; i++) {
        defaultMessageHandlers[i] = new DefaultMessageHandler(this);
      }
      configureDistributedEngine(this.jChannel = buildDistributedEngine(config.configFile()));

      // 启动引擎
      this.jChannel.connect(config.clusterName());
    } catch (Exception e) {
      throw new DistributedEngineException("Failed to start distributed engine.", e);
    }
  }

  private void configureDistributedEngine(JChannel jChannel) {
    Objects.requireNonNull(jChannel, "jChannel must not be null.");
    jChannel.setReceiver(this);
    jChannel.addAddressGenerator(new DefaultAddressGenerator(config, persistStorage));
    jChannel.setDiscardOwnMessages(true);
    jChannel.addChannelListener(this);
    jChannel.setName(config.instanceName());

    // 注册定制消息
    registerMessages(jChannel);

    // 自定义集群节点策略
    getGmsProtocol(jChannel).setMembershipChangePolicy(DefaultMembershipChangePolicy.INSTANCE);

    // 自定义鉴权token已经鉴权方式
    AuthToken at = getAuthProtocol(jChannel).getAuthToken();
    if (at != null) {
      ((JwtAuthToken) at).setConfig(config);
    }
  }

  /** 注册自定义消息类型 */
  private void registerMessages(JChannel jChannel) {
    MessageFactory messageFactory = getMessageFactory(jChannel);
    PooledNioMessage.register(messageFactory);
    PooledBytesMessage.register(messageFactory);
  }

  /**
   * 加载jgroups配置文件给jchannel使用
   *
   * @param configFile 配置文件路径
   * @return jchannel
   * @throws Exception 异常
   */
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
   * 获取传输协议
   *
   * @return 传输协议
   */
  private TP getTransport() {
    return jChannel.getProtocolStack().getTransport();
  }

  /**
   * 获取AUTH协议
   *
   * @param jChannel jchannel
   * @return AUTH
   */
  private AUTH getAuthProtocol(JChannel jChannel) {
    return jChannel.getProtocolStack().findProtocol(AUTH.class);
  }

  /**
   * 获取GMS协议
   *
   * @param jChannel jchannel
   * @return GMS
   */
  private GMS getGmsProtocol(JChannel jChannel) {
    return jChannel.getProtocolStack().findProtocol(GMS.class);
  }

  /**
   * 根据分区号生成存储列族名称
   *
   * @param partitionNo 分区号
   * @return 保存任务数据使用的列族名
   */
  String getPartitionCf(int partitionNo) {
    if (partitionNo > 0) {
      throw new IllegalArgumentException("partitionNo must be greater than or equals 0.");
    }
    String cf = clusterName() + partitionNo;
    persistStorage.createColumnFamily(cf);
    return cf;
  }

  /**
   * 获取消息工厂
   *
   * @return 消息工程
   */
  private MessageFactory getMessageFactory(JChannel jChannel) {
    return jChannel.getProtocolStack().getTransport().getMessageFactory();
  }

  /** 向coordinator请求，同步集群状态 */
  public void syncClusterState() throws Exception {
    jChannel.getState(null, config.clusterStateSyncTimeout());
  }

  /**
   * 获取集群绑定的通讯地址
   *
   * @return 绑定地址
   */
  InetAddress bindAddress() {
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
  int bindPort() {
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
   * 异步消息发送
   *
   * @param message 消息
   * @return this
   * @throws Exception 异常
   */
  DistributedEngine asyncSend(@NonNull Message message) throws Exception {
    jChannel.send(message);
    return this;
  }

  @Override
  public <T extends Comparable<T>> DistributedEngine asyncSend(
      @NonNull byte[] msg, @Nullable ClusterNode<T> dest) throws Exception {
    return asyncSend(msg, 0, msg.length, dest);
  }

  @Override
  public <T extends Comparable<T>> DistributedEngine asyncSend(
      @NonNull byte[] msg, int offset, int length, @Nullable ClusterNode<T> dest) throws Exception {
    if (dest == null) {
      jChannel.send(null, msg, offset, length);
    } else {
      jChannel.send((Address) dest.uuid(), msg, offset, length);
    }
    return this;
  }

  @Override
  public void receive(@NonNull MessageBatch batch) {
    for (Message message : batch) {
      if (log.isDebugEnabled()) {
        log.debug("A message received {}.", message);
      }

      // 处理任务消息
      if (message instanceof PooledBytesMessage) {
        try {
          byte[] bytes = message.getArray();
          JobMsgPayload jobMsgPayload = JobMsgPayload.parseFrom(bytes);

          // 根据任务计算其归属的任务消息处理队列
          int index = (int) (jobMsgPayload.getJob().getJobId() & messageEventQueueMark);
          defaultMessageHandlers[index].handle(jobMsgPayload, bytes);
        } catch (InvalidProtocolBufferException e) {
          log.error("Unknown job.", e);
        }
      } else {
        if (log.isDebugEnabled()) {
          log.debug("Ignores received message {}.", message);
        }
      }
    }
  }

  @Override
  public void viewAccepted(View newView) {
    if (log.isDebugEnabled()) {
      log.debug("A newView is received {}.", newView);
    }
    try {
      // 异步处理集群视图变更
      defaultViewChangedHandler.handle(lastView, newView);
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
  public List<DefaultClusterNode> getClusterNodes(View view) {
    if (view == null) {
      throw new IllegalArgumentException("view must not be null.");
    }
    return view.getMembers().stream().map(this::buildClusterNode).toList();
  }

  /**
   * 获取本地节点地址
   *
   * @return 节点地址
   */
  public ClusterNodeUUID getLocalAddress() {
    return (ClusterNodeUUID) jChannel.address();
  }

  /**
   * 获取本地节点
   *
   * @return 本地节点
   */
  public ClusterNode<Address> getLocalNode() {
    return buildClusterNode(jChannel.address());
  }

  private DefaultClusterNode buildClusterNode(Address address) {
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
    log.info(
        "The node{} sends the clusterState:{}[{}]", localIdentity(), lineSeparator(), clusterState);
  }

  @Override
  public void setState(InputStream input) throws Exception {
    clusterState = ClusterState.parseFrom(input);
    log.info(
        "The node{} receives the clusterState:{}[{}]",
        localIdentity(),
        lineSeparator(),
        clusterState);
  }

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
    if (isEmpty(name)) {
      throw new IllegalArgumentException("name must not be null.");
    }
    return distributedJobSchedulerMap.computeIfAbsent(
        name,
        key -> {
          ScheduledExecutorConfig seConfig =
              config.scheduledExecutorConfigs().stream()
                  .filter(c -> StringUtils.equals(c.name(), name))
                  .findAny()
                  .orElse(null);
          if (seConfig == null) {
            return null;
          }
          return new DefaultDistributedJobScheduler(
              this,
              name,
              Executors.newScheduledThreadPool(
                  seConfig.threadCoreSize(), new SimpleThreadFactory(seConfig.threadNamePrefix())));
        });
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
  }

  @Override
  public void close() {
    if (this.jChannel != null) {
      this.jChannel.close();
    }

    if (this.persistStorage != null) {
      this.persistStorage.close();
    }

    if (this.defaultViewChangedHandler != null) {
      this.defaultViewChangedHandler.close();
    }

    if (this.defaultMessageHandlers != null) {
      for (DefaultMessageHandler handler : defaultMessageHandlers) {
        if (handler != null) {
          handler.close();
        }
      }
    }

    if (this.metadata != null) {
      this.metadata.close();
    }

    if (this.distributedJobSchedulerMap != null) {
      this.distributedJobSchedulerMap.clear();
    }

    ADDRESS_BUFFER.remove();
  }
}
