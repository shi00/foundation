/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */
package com.silong.foundation.dj.mixmaster.core;

import static com.silong.foundation.dj.mixmaster.utils.SystemInfo.HARDWARE_UUID;
import static com.silong.foundation.dj.mixmaster.utils.SystemInfo.HOST_NAME;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import com.silong.foundation.common.utils.BiConverter;
import com.silong.foundation.dj.hook.auth.JwtAuthenticator;
import com.silong.foundation.dj.hook.event.ChannelClosedEvent;
import com.silong.foundation.dj.hook.event.JoinClusterEvent;
import com.silong.foundation.dj.hook.event.LeftClusterEvent;
import com.silong.foundation.dj.hook.event.ViewChangedEvent;
import com.silong.foundation.dj.mixmaster.*;
import com.silong.foundation.dj.mixmaster.configure.config.MixmasterProperties;
import com.silong.foundation.dj.mixmaster.exception.DistributedEngineException;
import com.silong.foundation.dj.mixmaster.generated.Messages.LocalMetadata;
import com.silong.foundation.dj.mixmaster.message.ProtoBufferMessage;
import com.silong.foundation.dj.mixmaster.message.TimestampHeader;
import com.silong.foundation.dj.mixmaster.utils.Slf4jLogFactory;
import com.silong.foundation.dj.mixmaster.vo.ClusterNodeUUID;
import com.silong.foundation.dj.mixmaster.vo.ClusterView;
import com.silong.foundation.dj.scrapper.PersistStorage;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import java.io.*;
import java.net.*;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jctools.queues.atomic.MpscAtomicArrayQueue;
import org.jgroups.*;
import org.jgroups.auth.AuthToken;
import org.jgroups.logging.LogFactory;
import org.jgroups.protocols.AUTH;
import org.jgroups.protocols.TP;
import org.jgroups.protocols.UDP;
import org.jgroups.protocols.pbcast.GMS;
import org.jgroups.stack.AddressGenerator;
import org.jgroups.stack.MembershipChangePolicy;
import org.jgroups.util.MessageBatch;
import org.jgroups.util.Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 基于jgroups的分布式任务引擎
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-10 00:30
 */
@Slf4j
@Component
@Accessors(fluent = true)
@SuppressFBWarnings({"PATH_TRAVERSAL_IN", "URLCONNECTION_SSRF_FD"})
class DefaultDistributedEngine
    implements DistributedEngine, Receiver, ChannelListener, Runnable, Serializable, AutoCloseable {

  @Serial private static final long serialVersionUID = -1_291_263_774_210_047_896L;

  /** 视图变化记录数量 */
  public static final int VIEW_CHANGED_RECORDS =
      Integer.parseInt(System.getProperty("cluster.view.change.records", "5"));

  private static final byte[] LOCAL_METADATA_KEY =
      String.format("%s:local:metadata", HARDWARE_UUID).getBytes(UTF_8);

  /** 元数据转换器 */
  private static final BiConverter<LocalMetadata, byte[]> METADATA_CONVERTER =
      new BiConverter<>() {
        @Override
        public byte[] to(@NonNull LocalMetadata localMetadata) {
          return localMetadata.toByteArray();
        }

        @Override
        @Nullable
        @SneakyThrows
        public LocalMetadata from(byte[] bytes) {
          if (bytes == null || bytes.length == 0) {
            return null;
          }
          return LocalMetadata.parseFrom(bytes);
        }
      };

  /** 事件发布器 */
  private ApplicationEventPublisher eventPublisher;

  /** 集群通信信道 */
  private JChannel jChannel;

  /** 配置 */
  private MixmasterProperties properties;

  /** 持久化存储 */
  private PersistStorage persistStorage;

  /** 集群元数据 */
  private DefaultClusterMetadata clusterMetadata;

  /** 逻辑时钟 */
  private HybridLogicalClock logicalClock;

  /** 消息队列，多生产者单消费者，异步保序处理事件 */
  private MpscAtomicArrayQueue<ApplicationEvent> eventQueue;

  /** 集群成员变更策略 */
  private MembershipChangePolicy membershipChangePolicy;

  /** 节点地址生成器 */
  private AddressGenerator addressGenerator;

  /** 鉴权处理器 */
  private JwtAuthenticator jwtAuthenticator;

  /** 集群视图 */
  final ClusterView clusterView = new ClusterView(VIEW_CHANGED_RECORDS);

  private final CountDownLatch clusterViewLatch = new CountDownLatch(1);

  static {
    // 使用slf4j进行日志打印
    LogFactory.setCustomLogFactory(new Slf4jLogFactory());

    // 注册自定义消息头
    TimestampHeader.register();
  }

  /** 初始化方法 */
  @PostConstruct
  public void initialize() {
    try (InputStream inputStream = properties.getConfigFile().openStream()) {
      // 创建jchannel并配置
      configureDistributedEngine(jChannel = new JChannel(inputStream));

      // 从本地持久化存储加载元数据
      resumeMetadata();

      // 加入集群，同步集群视图
      jChannel
          // 连接集群
          .connect(properties.getClusterName())
          // 从coordinator同步集群视图
          .getState(null, properties.getClusterStateSyncTimeout().toMillis());

      // 启动事件派发线程
      startEventDispatcherThread();
    } catch (Exception e) {
      throw new DistributedEngineException("Failed to start distributed engine.", e);
    }
  }

  /** 从本地存储加载元数据 */
  private void resumeMetadata() {
    byte[] bytes = persistStorage.get(LOCAL_METADATA_KEY);
    LocalMetadata localMetadata = METADATA_CONVERTER.from(bytes);
    if (localMetadata != null) {

      // 分区数量不能变
      if (localMetadata.getTotalPartition() != properties.getPartitions()) {
        throw new IllegalStateException(
            String.format(
                "The number of partitions changes and the service cannot be started. partition:[%d--->%d]",
                localMetadata.getTotalPartition(), properties.getPartitions()));
      }

      // 加载集群视图并恢复数据分区的拓扑图
      clusterView.readFrom(new ByteArrayInputStream(localMetadata.getClusterView().toByteArray()));
      if (!clusterView.isEmpty()) {
        List<View> list = clusterView.toList();
        Collections.reverse(list);
        list.forEach(view -> clusterMetadata.update(view));
      }
    }
  }

  private void startEventDispatcherThread() {
    Thread thread =
        new Thread(
            Thread.currentThread().getThreadGroup(),
            this,
            "Event-Dispatcher-" + properties.getInstanceName());
    thread.setDaemon(true);
    thread.start();
  }

  /** 等待成为连接状态 */
  void waitUntilConnected() {
    while (!jChannel.isConnected()) {
      Thread.onSpinWait();
    }
  }

  /** 事件派发循环 */
  @Override
  @SneakyThrows
  public void run() {
    // 等待jchannel状态为连接时启动事件派发
    waitUntilConnected();

    // 如果本地节点不是coordinator，则等待从coordinator获取当前集群的视图
    if (!isCoordinator()
        && !clusterViewLatch.await(
            properties.getClusterStateSyncTimeout().toMillis(), MILLISECONDS)) {
      throw new TimeoutException("Synchronization of cluster view from coordinator timed out.");
    }

    log.info("Start event dispatch processing......");
    while (!jChannel.isClosed()) {
      ApplicationEvent event;
      while ((event = eventQueue.poll()) != null) {
        if (event instanceof ViewChangedEvent viewChangedEvent) {
          clusterMetadata.update(viewChangedEvent.newView()); // 更新集群元数据
        }
        eventPublisher.publishEvent(event);
      }
      Thread.onSpinWait();
    }
    log.info("End event dispatch processing......");
  }

  /**
   * 获取传输协议
   *
   * @return 传输协议
   */
  private TP getTransport(JChannel jChannel) {
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
   * 获取消息工厂
   *
   * @return 消息工程
   */
  private MessageFactory getMessageFactory(JChannel jChannel) {
    return jChannel.getProtocolStack().getTransport().getMessageFactory();
  }

  private void configureDistributedEngine(JChannel jChannel) {
    jChannel.setReceiver(this);
    jChannel.addAddressGenerator(addressGenerator);
    jChannel.setDiscardOwnMessages(false);
    jChannel.addChannelListener(this);
    jChannel.setName(properties.getInstanceName());

    // 注册自定义消息
    ProtoBufferMessage.register(getMessageFactory(jChannel));

    // 自定义集群节点策略
    getGmsProtocol(jChannel).setMembershipChangePolicy(membershipChangePolicy);

    // 自定义鉴权token已经鉴权方式
    AuthToken at = getAuthProtocol(jChannel).getAuthToken();
    if (at != null) {
      ((DefaultAuthToken) at).initialize(jwtAuthenticator, properties);
    }
  }

  @Override
  public void viewAccepted(View newView) {
    if (log.isDebugEnabled()) {
      log.debug("The view of cluster[{}] has changed: {}", properties.getClusterName(), newView);
    }

    View oldView = clusterView.current();
    clusterView.insert(newView);
    ViewChangedEvent event = new ViewChangedEvent(oldView, newView);
    if (!eventQueue.offer(event)) {
      log.error("The event queue is full and new events are discarded. event:{}.", event);
    }
  }

  @Override
  public void channelConnected(JChannel channel) {
    String clusterName = properties.getClusterName();
    if (log.isDebugEnabled()) {
      log.debug(
          "The node{} has successfully joined the cluster[{}].",
          localIdentity(channel),
          clusterName);
    }
    JoinClusterEvent event =
        new JoinClusterEvent(clusterView.current(), clusterName, channel.address());
    if (!eventQueue.offer(event)) {
      log.error("The event queue is full and new events are discarded. event:{}.", event);
    }
  }

  @Override
  public void channelDisconnected(JChannel channel) {
    String clusterName = properties.getClusterName();
    if (log.isDebugEnabled()) {
      log.debug("The node{} has left the cluster[{}].", localIdentity(channel), clusterName);
    }
    LeftClusterEvent event =
        new LeftClusterEvent(clusterView.current(), clusterName, channel.address());
    if (!eventQueue.offer(event)) {
      log.error("The event queue is full and new events are discarded. event:{}.", event);
    }
  }

  @Override
  public void channelClosed(JChannel channel) {
    log.info("The node{} has been closed.", localIdentity(channel));
    ChannelClosedEvent event = new ChannelClosedEvent(channel);
    if (!eventQueue.offer(event)) {
      log.error("The event queue is full and new events are discarded. event:{}.", event);
    }
  }

  private InetAddress bindAddress(JChannel jChannel) {
    TP transport = getTransport(jChannel);
    return transport.getClass() == UDP.class
        ? ((UDP) transport).getMulticastAddress()
        : transport.getBindAddress();
  }

  private int bindPort(JChannel jChannel) {
    TP transport = getTransport(jChannel);
    return transport.getClass() == UDP.class
        ? ((UDP) transport).getMulticastPort()
        : transport.getBindPort();
  }

  private String localIdentity(JChannel jChannel) {
    return String.format(
        "(%s:%s|%s:%d)",
        HOST_NAME,
        properties.getInstanceName(),
        bindAddress(jChannel).getHostAddress(),
        bindPort(jChannel));
  }

  String name() {
    return jChannel.name();
  }

  @Override
  public String clusterName() {
    return jChannel.getClusterName();
  }

  /**
   * 发送消息时驱动逻辑时钟推进，并在消息头中附带此时间戳供消息接收方更新自己的逻辑时钟
   *
   * @param msg 待发送消息
   * @return this msg
   */
  private Message putTimestampHeader(Message msg) {
    return msg.putHeader(
        TimestampHeader.TYPE, TimestampHeader.builder().timestamp(logicalClock.tick()).build());
  }

  @Override
  public DistributedEngine send(byte[] msg, @Nullable ClusterNode<?> dest) throws Exception {
    return send(msg, 0, msg.length, dest);
  }

  @Override
  public DistributedEngine send(@NonNull Message msg) throws Exception {
    jChannel.send(putTimestampHeader(msg));
    return this;
  }

  @Override
  public DistributedEngine send(byte[] msg, int offset, int length, @Nullable ClusterNode<?> dest)
      throws Exception {
    if (msg == null) {
      throw new IllegalArgumentException("msg must not be null or empty.");
    }
    if (offset >= msg.length || offset < 0) {
      throw new ArrayIndexOutOfBoundsException("offset:" + offset);
    }
    if (offset + length > msg.length) {
      throw new ArrayIndexOutOfBoundsException("length:" + length);
    }

    Address target = null;
    if (dest == null) {
      if (log.isDebugEnabled()) {
        log.debug(
            "Send message to all cluster members. msg:{}",
            HexFormat.of().formatHex(msg, offset, offset + length));
      }
    } else {
      if (log.isDebugEnabled()) {
        log.debug(
            "Send message to the cluster members[{}]. msg:{}",
            dest.uuid(),
            HexFormat.of().formatHex(msg, offset, offset + length));
      }
      target = (Address) dest.uuid();
    }

    jChannel.send(putTimestampHeader(new BytesMessage(target, msg, offset, length)));
    return this;
  }

  @Override
  public <T extends MessageLite> DistributedEngine send(
      @NonNull T msg, @Nullable ClusterNode<?> dest) throws Exception {
    Address target = null;
    if (dest == null) {
      if (log.isDebugEnabled()) {
        log.debug("Send message to all cluster members. {}{}", System.lineSeparator(), msg);
      }
    } else {
      if (log.isDebugEnabled()) {
        log.debug(
            "Send message to the cluster members[{}]. {}{}",
            dest.uuid(),
            System.lineSeparator(),
            msg);
      }
      target = (Address) dest.uuid();
    }
    jChannel.send(
        putTimestampHeader(
            new ProtoBufferMessage<T>(target)
                .parser((Parser<T>) msg.getParserForType())
                .payload(msg)));
    return this;
  }

  @Override
  public void receive(@NonNull MessageBatch batch) {
    for (Message message : batch) {
      if (log.isDebugEnabled()) {
        log.debug("A message received {}.", message);
      }

      // 接收消息，更新逻辑时钟
      logicalClock.update(message.<TimestampHeader>getHeader(TimestampHeader.TYPE).timestamp());
    }
  }

  /**
   * 获取集群节点列表
   *
   * @param view 集群视图
   * @return 集群节点列表
   */
  List<DefaultClusterNode> getClusterNodes(@NonNull View view) {
    return view.getMembers().stream().map(this::buildClusterNode).toList();
  }

  /**
   * 获取本地节点地址，关闭后返回null
   *
   * @return 节点地址
   */
  @Override
  public ClusterNodeUUID localAddress() {
    return (ClusterNodeUUID) jChannel.address();
  }

  /**
   * 当前集群视图
   *
   * @return 视图
   */
  View currentView() {
    return jChannel.view();
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
    return new DefaultClusterNode((ClusterNodeUUID) address, this);
  }

  /**
   * 给定节点是否为coordinator节点
   *
   * @return true or false
   */
  boolean isCoordinator() {
    return Util.isCoordinator(jChannel);
  }

  /**
   * local节点作为集群coordinator时，把自己保存的最新集群视图传递给新加入集群的节点
   *
   * @param output The OutputStream
   * @throws Exception 异常
   */
  @Override
  public void getState(OutputStream output) throws Exception {
    clusterView.writeTo(output);
    log.info("The node{} sends the {} to member of cluster.", localIdentity(jChannel), clusterView);
  }

  /**
   * 从集群coordinator获取最新集群视图
   *
   * @param input The InputStream
   */
  @Override
  public void setState(InputStream input) {
    ClusterView cView = new ClusterView(0);
    cView.readFrom(input);
    if (log.isDebugEnabled()) {
      log.debug(
          "{} [coordinatorView:{}, localView:{}]", localIdentity(jChannel), cView, clusterView);
    }
    clusterView.merge(cView); // 合并集群视图
    if (log.isDebugEnabled()) {
      log.debug("{} [mergedView: {}]", localIdentity(jChannel), clusterView);
    }
    clusterViewLatch.countDown();
    log.info("The node{} receives the {} from coordinator.", localIdentity(jChannel), clusterView);
  }

  @Override
  public Cluster cluster() {
    return new DefaultCluster(this);
  }

  @Override
  public DistributedJobScheduler scheduler(String name) {
    return null;
  }

  @Override
  public void close() throws Exception {
    if (this.jChannel != null) {
      this.jChannel.close();
    }

    if (this.persistStorage != null) {
      this.persistStorage.close();
    }
  }

  @Autowired
  public void setMixmasterProperties(MixmasterProperties properties) {
    this.properties = properties;
  }

  @Autowired
  public void setPersistStorage(
      @Qualifier("mixmasterPersistStorage") PersistStorage persistStorage) {
    this.persistStorage = persistStorage;
  }

  @Autowired
  public void setAddressGenerator(AddressGenerator addressGenerator) {
    this.addressGenerator = addressGenerator;
  }

  @Autowired
  public void setMembershipChangePolicy(MembershipChangePolicy membershipChangePolicy) {
    this.membershipChangePolicy = membershipChangePolicy;
  }

  @Autowired
  public void setJwtAuthenticator(JwtAuthenticator jwtAuthenticator) {
    this.jwtAuthenticator = jwtAuthenticator;
  }

  @Autowired
  public void setEventQueue(
      @Qualifier("mixmasterEventQueue")
          MpscAtomicArrayQueue<ApplicationEvent> mpscAtomicArrayQueue) {
    this.eventQueue = mpscAtomicArrayQueue;
  }

  @Autowired
  public void setClusterMetadata(DefaultClusterMetadata clusterMetadata) {
    this.clusterMetadata = clusterMetadata;
    clusterMetadata.setEngine(this);
  }

  @Autowired
  public void setLogicalClock(HybridLogicalClock logicalClock) {
    this.logicalClock = logicalClock;
  }

  @Autowired
  public void setEventPublisher(ApplicationEventPublisher publisher) {
    this.eventPublisher = publisher;
  }
}
