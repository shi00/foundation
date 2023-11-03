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

import static com.silong.foundation.dj.bonecrusher.enu.ClientState.CLOSED;
import static com.silong.foundation.dj.bonecrusher.enu.ClientState.CONNECTED;
import static com.silong.foundation.dj.bonecrusher.enu.ServerState.*;
import static io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS;
import static io.netty.channel.ChannelOption.SO_REUSEADDR;

import com.silong.foundation.dj.bonecrusher.configure.config.BonecrusherClientProperties;
import com.silong.foundation.dj.bonecrusher.configure.config.BonecrusherServerProperties;
import com.silong.foundation.dj.bonecrusher.enu.ClientState;
import com.silong.foundation.dj.bonecrusher.enu.ServerState;
import com.silong.foundation.dj.bonecrusher.event.ClusterViewChangedEvent;
import com.silong.foundation.dj.bonecrusher.handler.*;
import com.silong.foundation.dj.bonecrusher.utils.FutureCombiner;
import com.silong.foundation.lambda.Tuple3;
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
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import jakarta.annotation.PostConstruct;
import java.util.UUID;
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

  private BonecrusherResponseEncoder bonecrusherResponseEncoder;

  private BonecrusherResponseDecoder bonecrusherResponseDecoder;

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

  /** 服务器状态 */
  private final AtomicReference<ServerState> serverState = new AtomicReference<>(CREATED);

  /** 初始化服务 */
  @PostConstruct
  public void initialize() {
    if (serverState.compareAndSet(CREATED, INITIALIZED)) {
      this.serverBootstrap =
          new ServerBootstrap()
              .group(
                  this.serverBossGroup =
                      new NioEventLoopGroup(
                          serverProperties.getBossGroupThreads(),
                          new DefaultThreadFactory(serverProperties.getBossGroupPoolName()),
                          NioUdtProvider.BYTE_PROVIDER),
                  this.serverConnectorsGroup =
                      new NioEventLoopGroup(
                          serverProperties.getWorkerGroupThreads(),
                          new DefaultThreadFactory(serverProperties.getConnectorGroupPoolName()),
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
                          .addLast("serverLogging", serverLoggingHandler)
                          .addLast(
                              "serverHandler",
                              serverChannelHandler.clusterViewChangedEventSupplier(
                                  clusterViewChangedEventRef::get))
                          .addLast("chunkedWriter", new ChunkedWriteHandler())
                          .addLast("resourcesTransfer", resourcesTransferHandler);
                    }
                  })
              .validate(); // 配置校验
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

  @Override
  public ServerState state() {
    return serverState.get();
  }

  @Override
  public void start(boolean block) throws InterruptedException {
    if (serverState.compareAndSet(INITIALIZED, RUNNING)) {
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
      if (block) {
        serverChannel.closeFuture().sync();
      }
    } else {
      throw new IllegalStateException(
          String.format("The current status of the server is not %s.", INITIALIZED));
    }
  }

  /** 异步关闭服务 */
  public void shutdown() {
    // 只有初始化状态或运行状态才能关闭
    if (serverState.compareAndSet(INITIALIZED, SHUTDOWN)
        || serverState.compareAndSet(RUNNING, SHUTDOWN)) {
      ChannelId id = serverChannel.id();
      FutureCombiner<?, Future<?>> futureCombiner =
          new FutureCombiner(
              serverBossGroup.shutdownGracefully(), serverConnectorsGroup.shutdownGracefully());
      futureCombiner.whenAllComplete(
          () ->
              log.info(
                  "The server[{}:{}--->id:{}] of bonecrusher has been shutdown.",
                  serverProperties.getAddress(),
                  serverProperties.getPort(),
                  id));
    }
  }

  /** 客户端 */
  @Getter
  public class BonecrusherClient implements DataSyncClient {

    private final EventLoopGroup clientConnectorsGroup;

    private final Bootstrap bootstrap;

    private Channel clientChannel;

    /** 客户端状态 */
    private final AtomicReference<ClientState> clientState =
        new AtomicReference<>(ClientState.INITIALIZED);

    @Override
    public ClientState state() {
      return clientState.get();
    }

    @Override
    public DataSyncClient connect(String remoteAddress, int remotePort) throws Exception {
      if (clientState.compareAndSet(ClientState.INITIALIZED, CONNECTED)) {
        joinClusterLatch.await(); // 加入集群后才能创建客户端
        this.clientChannel = bootstrap.connect(remoteAddress, remotePort).sync().channel();
        return this;
      } else {
        throw new IllegalStateException(
            String.format("The current status of the client is not %s.", ClientState.INITIALIZED));
      }
    }

    @Override
    public <T, R> R sendSync(T req) throws Exception {
      return this.<T, R>sendAsync(req).get();
    }

    @Override
    public <T, R> Future<R> sendAsync(@NonNull T req) {
      if (clientState.get() == CONNECTED) {
        Promise<R> promise = eventExecutor.newPromise();
        Tuple3<T, Promise<R>, String> tuple3 =
            Tuple3.<T, Promise<R>, String>builder()
                .t1(req)
                .t2(promise)
                .t3(UUID.randomUUID().toString())
                .build();

        // 异步发送请求
        ChannelFuture channelFuture =
            clientChannel
                .writeAndFlush(tuple3)
                .addListener(
                    future -> {
                      // 取消或者失败时通知
                      if (!future.isSuccess()) {
                        clientChannelHandler.cancelRequest(tuple3.t3());
                      }
                    });
        return promise.addListener(
            future -> {
              // 第三方通过promise执行取消时，执行取消
              if (future.isCancelled()) {
                channelFuture.cancel(true);
              }
            });
      } else {
        throw new IllegalStateException(
            String.format("The current status of the client is not %s.", CONNECTED));
      }
    }

    @Override
    public void close() {
      if (clientState.compareAndSet(CONNECTED, CLOSED)
          || clientState.compareAndSet(ClientState.INITIALIZED, CLOSED)) {
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
      this.bootstrap =
          new Bootstrap()
              .group(
                  this.clientConnectorsGroup =
                      new NioEventLoopGroup(
                          clientProperties.getConnectorGroupThreads(),
                          new DefaultThreadFactory(clientProperties.getConnectorGroupPoolName()),
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
                          //                          .addLast(
                          //                              "protobufVarint32FrameDecoder",
                          //                              new ProtobufVarint32FrameDecoder()) //
                          // 用于decode前解决半包和粘包问题（利用包头中的包含数组长度来识别半包粘包）
                          .addLast("protobufEncoder", protobufEncoder)
                          .addLast("compositeMessageDecoder", bonecrusherResponseDecoder)
                          .addLast("clientLogging", clientLoggingHandler)
                          .addLast(
                              "clientHandler",
                              clientChannelHandler.clusterViewChangedEventSupplier(
                                  clusterViewChangedEventRef::get));
                    }
                  })
              .validate();
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
}
