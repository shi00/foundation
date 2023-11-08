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

package com.silong.foundation.dj.bonecrusher;

import static com.silong.foundation.dj.bonecrusher.enu.NodeClusterState.JOINED;
import static com.silong.foundation.dj.bonecrusher.enu.NodeClusterState.LEFT;
import static com.silong.foundation.dj.bonecrusher.enu.ServerState.*;
import static io.netty.channel.ChannelOption.*;
import static io.netty.channel.udt.UdtChannelOption.*;
import static io.netty.channel.udt.nio.NioUdtProvider.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.silong.foundation.dj.bonecrusher.configure.config.BonecrusherClientProperties;
import com.silong.foundation.dj.bonecrusher.configure.config.BonecrusherProperties;
import com.silong.foundation.dj.bonecrusher.configure.config.BonecrusherServerProperties;
import com.silong.foundation.dj.bonecrusher.enu.NodeClusterState;
import com.silong.foundation.dj.bonecrusher.enu.ServerState;
import com.silong.foundation.dj.bonecrusher.event.JoinClusterEvent;
import com.silong.foundation.dj.bonecrusher.event.LeftClusterEvent;
import com.silong.foundation.dj.bonecrusher.event.ViewChangedEvent;
import com.silong.foundation.dj.bonecrusher.handler.*;
import com.silong.foundation.dj.bonecrusher.utils.FutureCombiner;
import com.silong.foundation.dj.bonecrusher.vo.ClusterInfo;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.udt.UdtChannel;
import io.netty.handler.codec.compression.SnappyFrameDecoder;
import io.netty.handler.codec.compression.SnappyFrameEncoder;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * UDT数据同步平台
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-24 15:48
 */
@Slf4j
@Component
class Bonecrusher implements ApplicationListener<ApplicationEvent>, DataSyncServer {
  BonecrusherClientProperties clientProperties;

  EventExecutor eventExecutor;

  BonecrusherResponseDecoder bonecrusherResponseDecoder;

  ProtobufVarint32LengthFieldPrepender protobufVarint32LengthFieldPrepender;

  ProtobufEncoder protobufEncoder;

  ClientChannelHandler clientChannelHandler;

  LoggingHandler clientLoggingHandler;

  /** 保存当前集群视图变化事件 */
  final AtomicReference<ClusterInfo> clusterInfoRef = new AtomicReference<>(new ClusterInfo());

  private BonecrusherServerProperties serverProperties;

  private BonecrusherProperties properties;

  private EventLoopGroup serverBossGroup;

  private EventLoopGroup serverConnectorsGroup;

  private ServerBootstrap serverBootstrap;

  private LoggingHandler serverLoggingHandler;

  private BonecrusherResponseEncoder bonecrusherResponseEncoder;

  private ProtobufDecoder requestProtobufDecoder;

  private ResourcesTransferHandler resourcesTransferHandler;

  private ServerChannelHandler serverChannelHandler;

  private volatile Channel serverChannel;

  /** 服务器初始状态 */
  private final AtomicReference<ServerState> serverState = new AtomicReference<>(NEW);

  /** 节点集群状态 */
  private final AtomicReference<NodeClusterState> nodeClusterState = new AtomicReference<>(LEFT);

  /** 监听器列表 */
  private final LinkedList<ClusterStateChangeListener> listeners = new LinkedList<>();

  /** 监听器列表 */
  private final LinkedList<LifecycleListener> lifecycleListeners = new LinkedList<>();

  /** 加入集群latch，确保服务只能在节点加入集群后启动 */
  private final Phaser joinClusterPhaser = new Phaser(2);

  /** 构造方法 */
  public Bonecrusher() {
    notifyLifeCycleListener(NEW, null);
  }

  @Override
  public void registerListener(@NonNull LifecycleListener... lifecycleListeners) {
    synchronized (this.lifecycleListeners) {
      this.lifecycleListeners.addAll(Arrays.asList(lifecycleListeners));
    }
  }

  @Override
  public void removeListener(LifecycleListener lifecycleListener) {
    synchronized (lifecycleListeners) {
      lifecycleListeners.remove(lifecycleListener);
    }
  }

  private void notifyLifeCycleListener(ServerState state, @Nullable Exception e) {
    synchronized (lifecycleListeners) {
      switch (state) {
        case ABNORMAL -> lifecycleListeners.forEach(
            lifecycleListener -> lifecycleListener.occurException(this, e));
        case RUNNING -> lifecycleListeners.forEach(
            lifecycleListener -> lifecycleListener.startCompletion(this));
        case INITIALIZED -> lifecycleListeners.forEach(
            lifecycleListener -> lifecycleListener.initializeCompletion(this));
        case SHUTDOWN -> lifecycleListeners.forEach(
            lifecycleListener -> lifecycleListener.shutdownCompletion(this));
        case NEW -> lifecycleListeners.forEach(
            lifecycleListener -> lifecycleListener.createCompletion(this));
      }
    }
  }

  /**
   * 注册监听器
   *
   * @param listener 监听器
   */
  public void registerListener(@NonNull ClusterStateChangeListener listener) {
    synchronized (listeners) {
      listeners.add(listener);
    }
  }

  /**
   * 删除监听器
   *
   * @param listener 监听器
   */
  public void removeListener(@NonNull ClusterStateChangeListener listener) {
    synchronized (listeners) {
      listeners.remove(listener);
    }
  }

  /** 初始化服务 */
  @PostConstruct
  @Override
  public void initialize() {
    // 状态变化
    if (serverState.compareAndSet(NEW, INITIALIZED)) {
      try {
        this.serverBootstrap =
            new ServerBootstrap()
                .group(
                    this.serverBossGroup =
                        new NioEventLoopGroup(
                            serverProperties.getBossGroupThreads(),
                            new DefaultThreadFactory(serverProperties.getBossGroupPoolName()),
                            BYTE_PROVIDER),
                    this.serverConnectorsGroup =
                        new NioEventLoopGroup(
                            serverProperties.getWorkerGroupThreads(),
                            new DefaultThreadFactory(serverProperties.getConnectorGroupPoolName()),
                            BYTE_PROVIDER))
                // 设置服务端通道实现类型
                .channelFactory(BYTE_ACCEPTOR)
                .handler(serverLoggingHandler)
                // 设置子channel的缓冲区分配器
                .option(SO_REUSEADDR, serverProperties.getNetty().isSO_REUSEADDR())
                .option(SO_LINGER, serverProperties.getNetty().getSO_LINGER())
                .option(SO_RCVBUF, (int) serverProperties.getNetty().getSO_RCVBUF().toBytes())
                .option(SO_SNDBUF, (int) serverProperties.getNetty().getSO_SNDBUF().toBytes())
                .option(
                    PROTOCOL_RECEIVE_BUFFER_SIZE,
                    (int) serverProperties.getNetty().getPROTOCOL_RECEIVE_BUFFER_SIZE().toBytes())
                .option(
                    PROTOCOL_SEND_BUFFER_SIZE,
                    (int) serverProperties.getNetty().getPROTOCOL_SEND_BUFFER_SIZE().toBytes())
                .option(
                    SYSTEM_RECEIVE_BUFFER_SIZE,
                    (int) serverProperties.getNetty().getSYSTEM_RECEIVE_BUFFER_SIZE().toBytes())
                .option(
                    SYSTEM_SEND_BUFFER_SIZE,
                    (int) serverProperties.getNetty().getSYSTEM_SEND_BUFFER_SIZE().toBytes())
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childHandler(
                    new ChannelInitializer<UdtChannel>() {
                      @Override
                      protected void initChannel(UdtChannel ch) {
                        ch.pipeline()
                            .addLast("idleStateMonitor", idleStateHandler()) // channel空闲监控
                            .addLast("snappyFrameEncoder", new SnappyFrameEncoder()) // snappy 压缩编码器
                            .addLast(
                                "snappyFrameDecoder", new SnappyFrameDecoder()) // snappy 解压缩解码器
                            //                          .addLast(
                            //                              "protobufVarint32LengthFieldPrepender",
                            //                              protobufVarint32LengthFieldPrepender) //
                            // 用于在序列化的字节数组前加上一个简单的包头，只包含序列化的字节长度
                            .addLast(
                                "protobufVarint32FrameDecoder",
                                new ProtobufVarint32FrameDecoder()) // 用于decode前解决半包和粘包问题（利用包头中的包含数组长度来识别半包粘包）
                            .addLast(
                                "bonecrusherResponseEncoder",
                                bonecrusherResponseEncoder) // bonecrusherResponseEncoder编码器
                            .addLast("protobufDecoder", requestProtobufDecoder) // protobuf请求解码器
                            .addLast("serverLogging", serverLoggingHandler) // 日志打印
                            .addLast(
                                "serverHandler",
                                serverChannelHandler.clusterInfoSupplier(clusterInfoRef::get))
                            .addLast("chunkedWriter", new ChunkedWriteHandler())
                            .addLast("resourcesTransfer", resourcesTransferHandler);
                      }
                    })
                .validate(); // 配置校验

        log.info("The bonecrusher has been initialized successfully.");

        // 通知初始化完成
        notifyLifeCycleListener(INITIALIZED, null);
      } catch (Exception e) {
        serverState.set(ABNORMAL);
        notifyLifeCycleListener(ABNORMAL, e);
        throw e;
      }
    } else {
      throw new IllegalStateException(
          String.format("CurrentState:%s, only the NEW state can be initialized.", state()));
    }
  }

  private IdleStateHandler idleStateHandler() {
    return new IdleStateHandler(
        serverProperties.getIdleState().isObserveOutput(),
        serverProperties.getIdleState().getReaderIdleTime().toSeconds(),
        serverProperties.getIdleState().getWriterIdleTime().toSeconds(),
        serverProperties.getIdleState().getAllIdleTime().toSeconds(),
        SECONDS);
  }

  @Override
  public ServerState state() {
    return serverState.get();
  }

  @Override
  public NodeClusterState clusterState() {
    return nodeClusterState.get();
  }

  @Override
  public void start(boolean block) throws Exception {
    // 状态变化
    if (serverState.compareAndSet(INITIALIZED, WAITING)) {
      try {

        // 只有本地节点加入集群后才能启动
        waitJoiningCluster(properties.getJoinClusterTimeout());

        // 绑定地址，端口号，启动服务端
        String listenAddress = serverProperties.getAddress();
        int listenPort = serverProperties.getPort();
        serverChannel = serverBootstrap.bind(listenAddress, listenPort).sync().channel();
        log.info(
            "The bonecrusher[{}:{}--->id:{}] has been started successfully.",
            listenAddress,
            listenPort,
            serverChannel.id());

        if (!serverState.compareAndSet(WAITING, RUNNING)) {
          throw new IllegalStateException(
              String.format("CurrentStatus:[%s], ExpectedStatus:[%s].", state(), WAITING));
        }

        notifyLifeCycleListener(RUNNING, null);

        // 对关闭通道进行监听
        if (block) {
          serverChannel.closeFuture().sync();
        }
      } catch (Exception e) {
        serverState.set(ABNORMAL);
        notifyLifeCycleListener(ABNORMAL, e);
        throw e;
      }
    } else {
      throw new IllegalStateException(
          String.format("CurrentState:%s, only the INITIALIZED state can be started.", state()));
    }
  }

  /** 异步关闭服务 */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void shutdown() {
    if (serverState.compareAndSet(RUNNING, SHUTDOWN)
        || serverState.compareAndSet(INITIALIZED, SHUTDOWN)
        || serverState.compareAndSet(WAITING, SHUTDOWN)
        || serverState.compareAndSet(ABNORMAL, SHUTDOWN)) {
      ChannelId id = serverChannel.id();
      FutureCombiner<?, Future<?>> futureCombiner =
          serverBossGroup != null && serverConnectorsGroup != null
              ? new FutureCombiner(
                  serverBossGroup.shutdownGracefully(), serverConnectorsGroup.shutdownGracefully())
              : serverBossGroup != null
                  ? new FutureCombiner(serverBossGroup.shutdownGracefully())
                  : new FutureCombiner(serverConnectorsGroup.shutdownGracefully());
      futureCombiner.whenAllComplete(
          () -> {
            log.info(
                "The server[{}:{}--->id:{}] of bonecrusher has been shutdown.",
                serverProperties.getAddress(),
                serverProperties.getPort(),
                id);

            notifyLifeCycleListener(SHUTDOWN, null);
          });
    }
  }

  @Override
  public DataSyncClient newClient() {
    BonecrusherClient client = new BonecrusherClient(this);
    registerListener(client);
    return client;
  }

  /** 等待local节点加入集群，如果当前已经加入集群则直接返回，否则当前线程阻塞，等待加入集群 */
  void waitJoiningCluster(Duration timeout) throws InterruptedException, TimeoutException {
    joinClusterPhaser.awaitAdvanceInterruptibly(
        joinClusterPhaser.arrive(), timeout.toMillis(), MILLISECONDS);
  }

  /**
   * 接收集群视图变化事件
   *
   * @param event the event to respond to 集群视图变化
   */
  @Override
  public void onApplicationEvent(@NonNull ApplicationEvent event) {
    switch (event) {
      case ViewChangedEvent viewChangedEvent -> {
        log.info("The cluster view has changed: {}", viewChangedEvent.newView());
        // 更新集群视图
        ClusterInfo clusterInfo = clusterInfoRef.get();
        clusterInfo.view(viewChangedEvent.newView());
        // 通知所有注册的监听器
        synchronized (listeners) {
          listeners.forEach(listener -> listener.viewChanged(clusterInfo));
        }
      }
      case LeftClusterEvent leftClusterEvent -> {
        String clusterName = leftClusterEvent.cluster();
        log.info(
            "The node{} has left the cluster of {}.", leftClusterEvent.localAddress(), clusterName);

        // 离开集群，清空集群信息
        if (nodeClusterState.compareAndSet(JOINED, LEFT)) {
          clusterInfoRef.set(new ClusterInfo()); // 清空集群信息
          joinClusterPhaser.register(); // 脱离集群后，增加是否已经加入集群的控制条件
        } else {
          throw new IllegalStateException(
              String.format("CurrentStatus:[%s], ExpectedStatus:[%s].", state(), JOINED));
        }

        // 通知所有注册的监听器
        synchronized (listeners) {
          listeners.forEach(listener -> listener.left(clusterName));
        }
      }
      case JoinClusterEvent joinClusterEvent -> {
        log.info(
            "The node{} has successfully joined the cluster of {}.",
            joinClusterEvent.localAddress(),
            joinClusterEvent.cluster());

        ClusterInfo clusterInfo =
            new ClusterInfo(
                joinClusterEvent.localAddress(),
                joinClusterEvent.cluster(),
                clusterInfoRef.get().view());

        // 加入集群，更新集群信息
        if (nodeClusterState.compareAndSet(LEFT, JOINED)) {
          clusterInfoRef.set(clusterInfo);
          joinClusterPhaser.arriveAndDeregister(); // 加入集群后，是否加入集群条件不再影响此phaser
        } else {
          throw new IllegalStateException(
              String.format("CurrentStatus:[%s], ExpectedStatus:[%s].", state(), LEFT));
        }

        // 通知所有注册的监听器
        synchronized (listeners) {
          listeners.forEach(listener -> listener.joined(clusterInfo));
        }
      }
      default -> {}
    }
  }

  @Autowired
  public void setServerLoggingHandler(
      @Qualifier("serverLoggingHandler") LoggingHandler serverLoggingHandler) {
    this.serverLoggingHandler = serverLoggingHandler;
  }

  @Autowired
  public void setClientLoggingHandler(
      @Qualifier("clientLoggingHandler") LoggingHandler clientLoggingHandler) {
    this.clientLoggingHandler = clientLoggingHandler;
  }

  @Autowired
  public void setFileLoaderHandler(ResourcesTransferHandler resourcesTransferHandler) {
    this.resourcesTransferHandler = resourcesTransferHandler;
  }

  @Autowired
  public void setServerAuthChannelHandler(ServerChannelHandler serverChannelHandler) {
    this.serverChannelHandler = serverChannelHandler;
  }

  @Autowired
  public void setClientChannelHandler(ClientChannelHandler clientChannelHandler) {
    this.clientChannelHandler = clientChannelHandler;
  }

  @Autowired
  public void setProtobufEncoder(ProtobufEncoder protobufEncoder) {
    this.protobufEncoder = protobufEncoder;
  }

  @Autowired
  public void setRequestProtobufDecoder(
      @Qualifier("requestProtobufDecoder") ProtobufDecoder requestProtobufDecoder) {
    this.requestProtobufDecoder = requestProtobufDecoder;
  }

  @Autowired
  public void setServerProperties(BonecrusherServerProperties serverProperties) {
    this.serverProperties = serverProperties;
  }

  @Autowired
  public void setClientProperties(BonecrusherClientProperties clientProperties) {
    this.clientProperties = clientProperties;
  }

  @Autowired
  public void setEventExecutor(EventExecutor eventExecutor) {
    this.eventExecutor = eventExecutor;
  }

  @Autowired
  public void setProtobufVarint32LengthFieldPrepender(
      ProtobufVarint32LengthFieldPrepender protobufVarint32LengthFieldPrepender) {
    this.protobufVarint32LengthFieldPrepender = protobufVarint32LengthFieldPrepender;
  }

  @Autowired
  public void setBonecrusherResponseEncoder(BonecrusherResponseEncoder bonecrusherResponseEncoder) {
    this.bonecrusherResponseEncoder = bonecrusherResponseEncoder;
  }

  @Autowired
  public void setBonecrusherResponseDecoder(BonecrusherResponseDecoder bonecrusherResponseDecoder) {
    this.bonecrusherResponseDecoder = bonecrusherResponseDecoder;
  }

  @Autowired
  public void setProperties(BonecrusherProperties properties) {
    this.properties = properties;
  }
}
