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

import static io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS;
import static io.netty.channel.ChannelOption.SO_REUSEADDR;

import com.silong.foundation.dj.bonecrusher.configure.config.BonecrusherClientProperties;
import com.silong.foundation.dj.bonecrusher.configure.config.BonecrusherServerProperties;
import com.silong.foundation.dj.bonecrusher.event.ClusterViewChangedEvent;
import com.silong.foundation.dj.bonecrusher.handler.ClientChannelHandler;
import com.silong.foundation.dj.bonecrusher.handler.ResourcesTransferHandler;
import com.silong.foundation.dj.bonecrusher.handler.ServerChannelHandler;
import com.silong.foundation.dj.bonecrusher.utils.FutureCombiner;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.udt.UdtChannel;
import io.netty.channel.udt.nio.NioUdtProvider;
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
import io.netty.util.concurrent.Promise;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
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
class Bonecrusher implements ApplicationListener<ClusterViewChangedEvent>, DataSyncServer {

  private BonecrusherServerProperties serverProperties;

  private BonecrusherClientProperties clientProperties;

  private EventLoopGroup serverBossGroup;

  private EventLoopGroup serverConnectorsGroup;

  private ServerBootstrap serverBootstrap;

  private LoggingHandler serverLoggingHandler;

  private LoggingHandler clientLoggingHandler;

  private ProtobufDecoder responseProtobufDecoder;

  private ProtobufDecoder requestProtobufDecoder;

  private ProtobufEncoder protobufEncoder;

  private ClientChannelHandler clientChannelHandler;

  private ResourcesTransferHandler resourcesTransferHandler;

  private ProtobufVarint32LengthFieldPrepender protobufVarint32LengthFieldPrepender;

  private ServerChannelHandler serverChannelHandler;

  private EventExecutor eventExecutor;

  private Channel serverChannel;

  /** 保存当前集群视图变化事件 */
  private final AtomicReference<ClusterViewChangedEvent> clusterViewChangedEventRef =
      new AtomicReference<>();

  /** 加入集群latch，确保服务只能在节点加入集群后启动 */
  private final CountDownLatch joinClusterLatch = new CountDownLatch(1);

  private final AtomicReference<ServerState> state = new AtomicReference<>();

  private enum ServerState {
    // 初始化状态
    INIT,
    // 运行中
    RUNNING,
    // 关闭
    SHUTDOWN
  }

  /** 初始化服务 */
  @PostConstruct
  public void initialize() {
    try {
      this.serverBootstrap =
          new ServerBootstrap()
              .group(
                  this.serverBossGroup =
                      new NioEventLoopGroup(
                          serverProperties.getBossGroupThreads(),
                          new DefaultThreadFactory("BS-Acceptor"),
                          NioUdtProvider.BYTE_PROVIDER),
                  this.serverConnectorsGroup =
                      new NioEventLoopGroup(
                          serverProperties.getWorkerGroupThreads(),
                          new DefaultThreadFactory("BS-Connector"),
                          NioUdtProvider.BYTE_PROVIDER))
              // 设置服务端通道实现类型
              .channelFactory(NioUdtProvider.BYTE_ACCEPTOR)
              .handler(serverLoggingHandler)
              // 设置子channel的缓冲区分配器
              .option(SO_REUSEADDR, serverProperties.getNetty().isSO_REUSEADDR())
              .option(
                  CONNECT_TIMEOUT_MILLIS,
                  (int) serverProperties.getNetty().getCONNECT_TIMEOUT_MILLIS().toMillis())
              .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
              .childHandler(
                  new ChannelInitializer<UdtChannel>() {
                    @Override
                    protected void initChannel(UdtChannel ch) {
                      ch.pipeline()
                          .addLast("idleStateMonitor", idleStateHandler()) // channel空闲监控
                          .addLast("snappyFrameEncoder", new SnappyFrameEncoder()) // snappy 压缩编码器
                          .addLast("snappyFrameDecoder", new SnappyFrameDecoder()) // snappy 解压缩解码器
                          .addLast(
                              "protobufVarint32LengthFieldPrepender",
                              protobufVarint32LengthFieldPrepender) // 用于在序列化的字节数组前加上一个简单的包头，只包含序列化的字节长度
                          .addLast(
                              "protobufVarint32FrameDecoder",
                              new ProtobufVarint32FrameDecoder()) // 用于decode前解决半包和粘包问题（利用包头中的包含数组长度来识别半包粘包）
                          .addLast("protobufEncoder", protobufEncoder) // protobuf编码器
                          .addLast("protobufDecoder", requestProtobufDecoder) // protobuf请求解码器
                          .addLast("serverLogging", serverLoggingHandler)
                          .addLast(
                              "authenticator",
                              serverChannelHandler.clusterViewChangedEventSupplier(
                                  clusterViewChangedEventRef::get))
                          .addLast("chunkedWriter", new ChunkedWriteHandler())
                          .addLast("resourcesTransfer", resourcesTransferHandler);
                    }
                  })
              .validate(); // 配置校验
    } finally {
      state.set(ServerState.INIT);
    }
  }

  private IdleStateHandler idleStateHandler() {
    return new IdleStateHandler(
        serverProperties.getIdleState().isObserveOutput(),
        serverProperties.getIdleState().getReaderIdleTime().toSeconds(),
        serverProperties.getIdleState().getWriterIdleTime().toSeconds(),
        serverProperties.getIdleState().getAllIdleTime().toSeconds(),
        TimeUnit.SECONDS);
  }

  /**
   * 启动服务<br>
   * 注意：阻塞调用start方法的当前线程
   */
  public void start() throws InterruptedException {
    if (state.compareAndSet(ServerState.INIT, ServerState.RUNNING)) {
      // 只有节点加入集群后才能启动
      joinClusterLatch.await();

      // 绑定地址，端口号，启动服务端
      String listenAddress = serverProperties.getAddress();
      int listenPort = serverProperties.getPort();
      serverChannel = serverBootstrap.bind(listenAddress, listenPort).sync().channel();

      log.info(
          "The bonecrusher[{}:{}--->id:{}] has been started successfully.",
          listenAddress,
          listenPort,
          serverChannel.id());

      // 对关闭通道进行监听
      //    channel.closeFuture().sync();
    }
  }

  /** 异步关闭服务 */
  public void shutdown() {
    ServerState serverState = state.get();
    if (serverState != null && serverState != ServerState.SHUTDOWN) {
      state.set(ServerState.SHUTDOWN);
      ChannelId id = serverChannel.id();
      String address = serverProperties.getAddress();
      int port = serverProperties.getPort();

      FutureCombiner<?, io.netty.util.concurrent.Future<?>> futureCombiner =
          new FutureCombiner(
              serverBossGroup.shutdownGracefully(), serverConnectorsGroup.shutdownGracefully());
      futureCombiner.whenAllComplete(
          () ->
              log.info(
                  "The server[{}:{}--->id:{}] of bonecrusher has been shutdown.",
                  address,
                  port,
                  id));
    }
  }

  /** 客户端 */
  @Getter
  public class BonecrusherClient implements DataSyncClient {

    private final EventLoopGroup clientConnectorsGroup;

    private final Bootstrap bootstrap;

    private Channel clientChannel;

    private final AtomicReference<ClientState> state = new AtomicReference<>();

    /** 客户端状态 */
    private enum ClientState {
      // 初始化
      INIT,
      // 已连接
      CONNECTED,
      // 已关闭
      CLOSED
    }

    @Override
    public DataSyncClient connect(String remoteAddress, int remotePort) throws Exception {
      if (state.compareAndSet(ClientState.INIT, ClientState.CONNECTED)) {
        joinClusterLatch.await(); // 加入集群后才能创建客户端
        this.clientChannel = bootstrap.connect(remoteAddress, remotePort).sync().channel();
        return this;
      }
      throw new IllegalStateException(
          String.format("The current status of the client is not %s.", ClientState.INIT));
    }

    @Override
    public <T, R> R sendSync(T req) throws Exception {
      return this.<T, R>sendAsync(req).get();
    }

    @Override
    public <T, R> io.netty.util.concurrent.Future<R> sendAsync(@NonNull T req) throws Exception {
      if (state.get() == ClientState.CONNECTED) {
        Promise<R> promise = eventExecutor.newPromise();

        clientChannel.writeAndFlush(req);
        return promise;
      }
      throw new IllegalStateException(
          String.format("The current status of the client is not %s.", ClientState.CONNECTED));
    }

    @Override
    public void close() {
      ClientState clientState = state.get();
      if (clientState != null && clientState != ClientState.CLOSED) {
        state.set(ClientState.CLOSED);
        ChannelId id = clientChannel.id();
        clientConnectorsGroup
            .shutdownGracefully()
            .addListener(
                future ->
                    log.info(
                        "The client[{}--->id:{}] of bonecrusher has been shutdown {}.",
                        clientChannel.localAddress(),
                        id,
                        future.isSuccess() ? "successfully" : "unsuccessfully"));
      }
    }

    /** 构造方法 */
    public BonecrusherClient() {
      try {
        this.bootstrap =
            new Bootstrap()
                .group(
                    this.clientConnectorsGroup =
                        new NioEventLoopGroup(
                            clientProperties.getConnectorGroupThreads(),
                            new DefaultThreadFactory("BC-Connector"),
                            NioUdtProvider.BYTE_PROVIDER))
                // 设置服务端通道实现类型
                .channelFactory(NioUdtProvider.BYTE_CONNECTOR)
                // 设置子channel的缓冲区分配器
                .option(SO_REUSEADDR, clientProperties.getNetty().isSO_REUSEADDR())
                .option(
                    CONNECT_TIMEOUT_MILLIS,
                    (int) clientProperties.getNetty().getCONNECT_TIMEOUT_MILLIS().toMillis())
                .handler(
                    new ChannelInitializer<UdtChannel>() {
                      @Override
                      public void initChannel(UdtChannel ch) {
                        ch.pipeline()
                            .addLast("snappyFrameEncoder", new SnappyFrameEncoder())
                            .addLast("snappyFrameDecoder", new SnappyFrameDecoder())
                            .addLast(
                                "protobufVarint32LengthFieldPrepender",
                                protobufVarint32LengthFieldPrepender) // 用于在序列化的字节数组前加上一个简单的包头，只包含序列化的字节长度
                            .addLast(
                                "protobufVarint32FrameDecoder",
                                new ProtobufVarint32FrameDecoder()) // 用于decode前解决半包和粘包问题（利用包头中的包含数组长度来识别半包粘包）
                            .addLast("protobufEncoder", protobufEncoder)
                            .addLast("protobufDecoder", responseProtobufDecoder)
                            .addLast("clientLogging", clientLoggingHandler)
                            .addLast(
                                "messagesHandler",
                                clientChannelHandler.clusterViewChangedEventSupplier(
                                    clusterViewChangedEventRef::get));
                      }
                    })
                .validate();
      } finally {
        state.set(ClientState.INIT);
      }
    }
  }

  @Override
  public DataSyncClient client() {
    return new BonecrusherClient();
  }

  /**
   * 接收集群视图变化事件
   *
   * @param event the event to respond to 集群视图变化
   */
  @Async
  @Override
  public void onApplicationEvent(ClusterViewChangedEvent event) {
    log.info("{}The cluster view has changed: {}", System.lineSeparator(), event);
    clusterViewChangedEventRef.set(event);
    joinClusterLatch.countDown();
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
  public void setResponseProtobufDecoder(
      @Qualifier("responseProtobufDecoder") ProtobufDecoder responseProtobufDecoder) {
    this.responseProtobufDecoder = responseProtobufDecoder;
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
}
