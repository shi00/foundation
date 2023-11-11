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

import static com.silong.foundation.dj.mixmaster.core.DefaultAddressGenerator.HOST_NAME;
import static com.silong.foundation.dj.mixmaster.vo.EmptyView.EMPTY_VIEW;
import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;

import com.google.protobuf.MessageLite;
import com.silong.foundation.dj.hook.auth.JwtAuthenticator;
import com.silong.foundation.dj.hook.event.ChannelClosedEvent;
import com.silong.foundation.dj.hook.event.JoinClusterEvent;
import com.silong.foundation.dj.hook.event.LeftClusterEvent;
import com.silong.foundation.dj.hook.event.ViewChangedEvent;
import com.silong.foundation.dj.mixmaster.*;
import com.silong.foundation.dj.mixmaster.configure.config.MixmasterProperties;
import com.silong.foundation.dj.mixmaster.exception.DistributedEngineException;
import com.silong.foundation.dj.mixmaster.message.Messages.ClusterConfig;
import com.silong.foundation.dj.mixmaster.message.Messages.ClusterNodeInfo;
import com.silong.foundation.dj.mixmaster.vo.ClusterNodeUUID;
import com.silong.foundation.dj.scrapper.PersistStorage;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import java.io.*;
import java.net.*;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jgroups.*;
import org.jgroups.auth.AuthToken;
import org.jgroups.protocols.AUTH;
import org.jgroups.protocols.TP;
import org.jgroups.protocols.UDP;
import org.jgroups.protocols.pbcast.GMS;
import org.jgroups.stack.AddressGenerator;
import org.jgroups.stack.MembershipChangePolicy;
import org.jgroups.util.MessageBatch;
import org.springframework.beans.factory.annotation.Autowired;
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
    implements DistributedEngine, Receiver, ChannelListener, Serializable, AutoCloseable {

  @Serial private static final long serialVersionUID = -1_291_263_774_210_047_896L;

  /** 事件发布器 */
  private ApplicationEventPublisher publisher;

  /** 集群通信信道 */
  private JChannel jChannel;

  /** 配置 */
  private MixmasterProperties properties;

  /** 分区节点映射器 */
  private Partition2NodesMapping<ClusterNodeUUID> partitionMapping;

  /** 持久化存储 */
  private PersistStorage persistStorage;

  /** 对象分区映射器 */
  private Object2PartitionMapping objectPartitionMapping;

  /** 集群成员变更策略 */
  private MembershipChangePolicy membershipChangePolicy;

  /** 节点地址生成器 */
  private AddressGenerator addressGenerator;

  /** 鉴权处理器 */
  private JwtAuthenticator jwtAuthenticator;

  /** 集群核心配置，以coordinator配置为准 */
  private volatile ClusterConfig clusterConfig;

  /** 最后一个集群视图 */
  private final AtomicReference<View> lastViewRef = new AtomicReference<>(EMPTY_VIEW);

  /** 初始化方法 */
  @PostConstruct
  public void initialize() {
    try {
      // 配置
      configureDistributedEngine(jChannel = buildDistributedEngine(properties.getConfigFile()));

      // 启动
      jChannel.connect(properties.getClusterName());
    } catch (Exception e) {
      throw new DistributedEngineException("Failed to start distributed engine.", e);
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

    // 自定义集群节点策略
    getGmsProtocol(jChannel).setMembershipChangePolicy(membershipChangePolicy);

    // 自定义鉴权token已经鉴权方式
    AuthToken at = getAuthProtocol(jChannel).getAuthToken();
    if (at != null) {
      ((DefaultAuthToken) at).initialize(jwtAuthenticator, properties);
    }
  }

  private String localIdentity() {
    return String.format(
        "(%s:%s|%s:%d)",
        HOST_NAME, properties.getInstanceName(), bindAddress().getHostAddress(), bindPort());
  }

  private String printViewMembers(JChannel channel) {
    return channel.getView().getMembers().stream()
        .map(
            address -> {
              ClusterNodeInfo clusterNodeInfo = ((ClusterNodeUUID) address).clusterNodeInfo();
              return String.format(
                  "(%s:%s)",
                  clusterNodeInfo.getHost().getName(), clusterNodeInfo.getInstanceName());
            })
        .collect(joining(",", "[", "]"));
  }

  /**
   * 接收集群视图变化
   *
   * @param newView 新视图
   */
  @Override
  public void viewAccepted(View newView) {
    if (log.isDebugEnabled()) {
      log.debug("The view of cluster[{}] has changed: {}", properties.getClusterName(), newView);
    }
    publisher.publishEvent(new ViewChangedEvent(lastViewRef.getAndSet(newView), newView));
  }

  @Override
  public void channelConnected(JChannel channel) {
    String clusterName = properties.getClusterName();
    if (log.isDebugEnabled()) {
      log.debug(
          "The node{} has successfully joined the cluster[{}].", localIdentity(), clusterName);
    }
    publisher.publishEvent(new JoinClusterEvent(lastViewRef.get(), clusterName, channel.address()));
  }

  @Override
  public void channelDisconnected(JChannel channel) {
    String clusterName = properties.getClusterName();
    if (log.isDebugEnabled()) {
      log.debug("The node{} has left the cluster[{}].", localIdentity(), clusterName);
    }
    publisher.publishEvent(new LeftClusterEvent(lastViewRef.get(), clusterName, channel.address()));
  }

  @Override
  public void channelClosed(JChannel channel) {
    log.info("The node{} has been closed.", localIdentity());
    publisher.publishEvent(new ChannelClosedEvent(channel));
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
      return new URI(confFile).toURL();
    } catch (Exception e) {
      File file = new File(confFile);
      if (file.exists() && file.isFile()) {
        return file.toURI().toURL();
      } else {
        return getClass().getClassLoader().getResource(confFile);
      }
    }
  }

  /** 向coordinator请求，同步集群状态 */
  public void syncClusterState() throws Exception {
    jChannel.getState(null, properties.getClusterStateSyncTimeout().toMillis());
  }

  /**
   * 获取集群绑定的通讯地址
   *
   * @return 绑定地址
   */
  private InetAddress bindAddress() {
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
  private int bindPort() {
    TP transport = getTransport();
    return transport.getClass() == UDP.class
        ? ((UDP) transport).getMulticastPort()
        : transport.getBindPort();
  }

  String name() {
    return jChannel.name();
  }

  @Override
  public String clusterName() {
    return jChannel.getClusterName();
  }

  @Override
  public DistributedEngine send(byte[] msg, @Nullable ClusterNode<?> dest) throws Exception {
    return send(msg, 0, msg.length, dest);
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
    if (dest == null) {
      if (log.isDebugEnabled()) {
        log.debug(
            "Send message to all cluster members. msg:{}",
            HexFormat.of().formatHex(msg, offset, offset + length));
      }
      jChannel.send(null, msg, offset, length);
    } else {
      if (log.isDebugEnabled()) {
        log.debug(
            "Send message to the cluster members[{}]. msg:{}",
            dest.uuid(),
            HexFormat.of().formatHex(msg, offset, offset + length));
      }
      jChannel.send((Address) dest.uuid(), msg, offset, length);
    }
    return this;
  }

  @Override
  public <T extends MessageLite> DistributedEngine send(
      @NonNull T msg, @Nullable ClusterNode<?> dest) throws Exception {
    if (dest == null) {
      if (log.isDebugEnabled()) {
        log.debug("Send message to all cluster members. {}{}", System.lineSeparator(), msg);
      }
      send(msg.toByteArray(), null);
    } else {
      if (log.isDebugEnabled()) {
        log.debug(
            "Send message to the cluster members[{}]. {}{}",
            dest.uuid(),
            System.lineSeparator(),
            msg);
      }
      send(msg.toByteArray(), dest);
    }
    return this;
  }

  @Override
  public void receive(@NonNull MessageBatch batch) {
    for (Message message : batch) {
      if (log.isDebugEnabled()) {
        log.debug("A message received {}.", message);
      }
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
   * @param address 节点地址
   * @return true or false
   */
  boolean isCoordinator(Address address) {
    return jChannel.view().getCoord().equals(address);
  }

  @Override
  public void getState(OutputStream output) throws Exception {
    if (clusterConfig == null) {
      clusterConfig =
          ClusterConfig.newBuilder()
              .setTotalPartition(properties.getPartitions())
              .setBackupNum(properties.getBackupNum())
              .build();
    }
    clusterConfig.writeTo(output);
    log.info(
        "The node{} sends the config to member of cluster. {}clusterConfig:[{}]",
        localIdentity(),
        lineSeparator(),
        clusterConfig);
  }

  @Override
  public void setState(InputStream input) throws Exception {
    ClusterConfig clusterConfig = ClusterConfig.parseFrom(input);
    log.info(
        "The node{} receives the config from coordinator. {}clusterConfig:[{}]",
        localIdentity(),
        lineSeparator(),
        clusterConfig);
    if (clusterConfig.getBackupNum() != properties.getBackupNum()
        || clusterConfig.getTotalPartition() != properties.getPartitions()) {
      log.error(
          "The number of partitions[{}] and backupNum[{}] configured on the node{} are inconsistent with the cluster coordinator[partitions:{}, backupNum:{}].",
          properties.getPartitions(),
          properties.getBackupNum(),
          localIdentity(),
          clusterConfig.getTotalPartition(),
          clusterConfig.getBackupNum());
    } else {
      this.clusterConfig = clusterConfig;
    }
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
  public void setPartitionMapping(Partition2NodesMapping<ClusterNodeUUID> partitionMapping) {
    this.partitionMapping = partitionMapping;
  }

  @Autowired
  public void setPersistStorage(PersistStorage persistStorage) {
    this.persistStorage = persistStorage;
  }

  @Autowired
  public void setObjectPartitionMapping(Object2PartitionMapping objectPartitionMapping) {
    this.objectPartitionMapping = objectPartitionMapping;
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
  public void setPublisher(ApplicationEventPublisher publisher) {
    this.publisher = publisher;
  }
}
