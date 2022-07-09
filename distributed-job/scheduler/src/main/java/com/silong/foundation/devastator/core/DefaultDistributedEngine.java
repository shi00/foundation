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
import com.silong.foundation.devastator.*;
import com.silong.foundation.devastator.config.DevastatorConfig;
import com.silong.foundation.devastator.config.ScheduledExecutorConfig;
import com.silong.foundation.devastator.exception.DistributedEngineException;
import com.silong.foundation.devastator.message.PooledBytesMessage;
import com.silong.foundation.devastator.message.PooledNioMessage;
import com.silong.foundation.devastator.model.ClusterNodeUUID;
import com.silong.foundation.devastator.model.Devastator.ClusterNodeInfo;
import com.silong.foundation.devastator.model.Devastator.ClusterState;
import com.silong.foundation.devastator.model.Devastator.JobMsgPayload;
import com.silong.foundation.devastator.model.KvPair;
import com.silong.foundation.devastator.model.Tuple;
import com.silong.foundation.devastator.utils.KryoUtils;
import com.silong.foundation.utilities.concurrent.SimpleThreadFactory;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jgroups.*;
import org.jgroups.protocols.TP;
import org.jgroups.protocols.UDP;
import org.jgroups.protocols.pbcast.GMS;
import org.jgroups.util.ByteArrayDataOutputStream;
import org.jgroups.util.MessageBatch;

import java.io.*;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static com.silong.foundation.devastator.config.DevastatorConfig.DEFAULT_PARTITION_SIZE;
import static com.silong.foundation.devastator.core.RocksDbPersistStorage.DEFAULT_COLUMN_FAMILY_NAME;
import static com.silong.foundation.devastator.utils.TypeConverter.Long2Bytes.INSTANCE;
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
  final JChannel jChannel;

  /** 配置 */
  final DevastatorConfig config;

  /** 分区节点映射器 */
  final RendezvousPartitionMapping partitionMapping;

  /** 持久化存储 */
  final RocksDbPersistStorage persistStorage;

  /** 分区到节点映射关系，Collection中的第一个节点为primary，后续为backup */
  Map<Integer, List<DefaultClusterNode>> partition2ClusterNodes;

  /** 数据uuid到分区的映射关系 */
  final Map<Object, Integer> uuid2Partitions;

  /** 对象分区映射器 */
  DefaultObjectPartitionMapping objectPartitionMapping;

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
      this.uuid2Partitions = new ConcurrentHashMap<>(DEFAULT_PARTITION_SIZE);
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
   * 数据到分区映射并持久化
   *
   * @param obj 数据对象
   * @return 对象映射分区
   */
  @SneakyThrows
  public synchronized int partitionAndPersistence(Identity<Address> obj) {
    if (obj == null) {
      throw new IllegalArgumentException("obj must not be null.");
    }

    // 计算对象对应的分区
    Address key = obj.uuid();
    int partition = objectPartitionMapping.partition(key);
    uuid2Partitions.put(key, partition);

    // 从默认列族查询key对应的版本，如果新数据版本大于当前已有版本则更新数据到partition对应的列族
    if (isPartition2LocalNode(partition)) {
      byte[] keyBytes = address2Bytes(key);
      byte[] versionBytes = persistStorage.get(keyBytes);
      long version =
          //              obj.objectVersion()
          1;
      if (versionBytes == null || INSTANCE.from(versionBytes) < version) {
        persistStorage.putAllWith(
            List.of(
                new Tuple<>(
                    DEFAULT_COLUMN_FAMILY_NAME, new KvPair<>(keyBytes, INSTANCE.to(version))),
                new Tuple<>(
                    String.valueOf(partition), new KvPair<>(keyBytes, KryoUtils.serialize(obj)))));
      }
    }
    return partition;
  }

  private boolean isPartition2LocalNode(int partition) {
    Address local = jChannel.address();
    for (DefaultClusterNode node : partition2ClusterNodes.get(partition)) {
      if (local.equals(node.uuid())) {
        return true;
      }
    }
    return false;
  }

  private byte[] address2Bytes(Address address) throws IOException {
    ByteArrayDataOutputStream outputStream = ADDRESS_BUFFER.get();
    address.writeTo(outputStream);
    return outputStream.buffer();
  }

  /**
   * 根据新的集群视图调整分区表
   *
   * @param oldView 旧集群视图
   * @param newView 新集群视图
   */
  public synchronized void repartition(View oldView, View newView) {
    assert newView != null;

    Map<Integer, List<DefaultClusterNode>> oldPartition2ClusterNodes = partition2ClusterNodes;
    int partitionCount = getPartitionCount();
    if (partition2ClusterNodes == null) {
      // 分区表key数量在没有集群节点变化时是固定的，并且每个分区号都不同，此处设置初始容量大于最大容量，配合负载因子为1，避免rehash
      objectPartitionMapping = new DefaultObjectPartitionMapping(partitionCount);
      partition2ClusterNodes = new ConcurrentHashMap<>(partitionCount, 1.0f);
    }
    // 如果分区数变化则重置分区映射器
    else if (objectPartitionMapping.partitions() != partitionCount) {
      objectPartitionMapping.partitions(partitionCount);
      partition2ClusterNodes = new ConcurrentHashMap<>(partitionCount, 1.0f);
    }

    Address localAddress = jChannel.getAddress();

    // 获取集群节点列表
    List<DefaultClusterNode> newClusterNodes =
        newView.getMembers().stream()
            .map(address -> new DefaultClusterNode((ClusterNodeUUID) address, localAddress))
            .toList();

    // 根据集群节点调整分区分布
    refreshPartitionTable(partitionCount, newClusterNodes);

    List<Tuple<Integer, Boolean>> oldLocalPartitions = null;
    if (oldPartition2ClusterNodes != null) {
      oldLocalPartitions = getLocalPartitions(localAddress, oldPartition2ClusterNodes);
    }

    List<Tuple<Integer, Boolean>> newLocalPartitions =
        getLocalPartitions(localAddress, partition2ClusterNodes);
  }

  private void refreshPartitionTable(int partitionCount, List<DefaultClusterNode> clusterNodes) {
    IntStream.range(0, partitionCount)
        .parallel()
        .forEach(
            partitionNo ->
                partition2ClusterNodes.put(
                    partitionNo,
                    partitionMapping.allocatePartition(
                        partitionNo, getBackupNums(), clusterNodes, null)));
  }

  private List<Tuple<Integer, Boolean>> getLocalPartitions(
      Address local, Map<Integer, List<DefaultClusterNode>> p2n) {
    if (p2n == null) {
      return List.of();
    }
    List<Tuple<Integer, Boolean>> list = new ArrayList<>(p2n.size());
    partition2ClusterNodes.forEach(
        (k, v) -> {
          int index = indexOf(v, local);
          if (index > 0) {
            list.add(new Tuple<>(k, false));
          } else if (index == 0) {
            list.add(new Tuple<>(k, true));
          }
        });
    list.sort(Comparator.comparingInt(Tuple::t1));
    return list;
  }

  private int indexOf(Collection<DefaultClusterNode> nodes, Address address) {
    int i = 0;
    for (DefaultClusterNode node : nodes) {
      if (node.uuid().equals(address)) {
        return i;
      }
    }
    return -1;
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
  DistributedEngine send(@NonNull Message message) throws Exception {
    jChannel.send(message);
    return this;
  }

  @Override
  public <T extends Comparable<T>> DistributedEngine send(
      @NonNull byte[] msg, @Nullable ClusterNode<T> dest) throws Exception {
    return send(msg, 0, msg.length, dest);
  }

  @Override
  public <T extends Comparable<T>> DistributedEngine send(
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
      if (message instanceof PooledBytesMessage msg) {
        try {
          byte[] bytes = msg.getArray();
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
  public List<ClusterNode<Address>> getClusterNodes(View view) {
    if (view == null) {
      throw new IllegalArgumentException("view must not be null.");
    }
    return view.getMembers().stream().map(this::buildClusterNode).toList();
  }

  /**
   * 获取本地节点
   *
   * @return 本地节点
   */
  public ClusterNode<Address> getLocalNode() {
    return buildClusterNode(jChannel.address());
  }

  private ClusterNode<Address> buildClusterNode(Address address) {
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

    if (this.uuid2Partitions != null) {
      this.uuid2Partitions.clear();
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

    if (this.partition2ClusterNodes != null) {
      this.partition2ClusterNodes.clear();
    }

    if (this.distributedJobSchedulerMap != null) {
      this.distributedJobSchedulerMap.clear();
    }

    ADDRESS_BUFFER.remove();
  }
}
