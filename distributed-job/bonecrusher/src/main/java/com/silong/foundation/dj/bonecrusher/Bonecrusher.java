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
import com.silong.foundation.dj.bonecrusher.configure.config.BonecrusherProperties;
import com.silong.foundation.dj.bonecrusher.event.ClusterViewChangedEvent;
import com.silong.foundation.dj.bonecrusher.handler.ClientAuthChannelHandler;
import com.silong.foundation.dj.bonecrusher.handler.FileServerHandler;
import com.silong.foundation.dj.bonecrusher.handler.ServerAuthChannelHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.udt.UdtChannel;
import io.netty.channel.udt.nio.NioUdtProvider;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

  private BonecrusherProperties serverProperties;

  private BonecrusherClientProperties clientProperties;

  private EventLoopGroup serverBossGroup;

  private EventLoopGroup serverConnectorsGroup;

  private ServerBootstrap serverBootstrap;

  private LoggingHandler loggingHandler;

  private ProtobufDecoder protobufDecoder;

  private ProtobufEncoder protobufEncoder;

  private FileServerHandler fileServerHandler;

  private ServerAuthChannelHandler serverAuthChannelHandler;

  private Channel serverChannel;

  /** 保存当前集群视图变化事件 */
  private final AtomicReference<ClusterViewChangedEvent> clusterViewChangedEventRef =
      new AtomicReference<>();

  /** 加入集群latch，确保服务只能在节点加入集群后启动 */
  private final CountDownLatch joinClusterLatch = new CountDownLatch(1);

  /** 初始化服务 */
  @PostConstruct
  public void initialize() {
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
            .handler(loggingHandler)
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
                        .addLast("protobufEncoder", protobufEncoder)
                        .addLast("protobufDecoder", protobufDecoder)
                        .addLast("logging", loggingHandler)
                        .addLast(
                            "authenticator",
                            serverAuthChannelHandler.clusterViewChangedEventSupplier(
                                clusterViewChangedEventRef::get))
                        .addLast("chunkedWriter", new ChunkedWriteHandler())
                        .addLast("fileServer", fileServerHandler);
                  }
                })
            .validate(); // 配置校验
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
  @SneakyThrows
  public void start() {
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

  /** 异步关闭服务 */
  public void shutdown() {
    ChannelId id = serverChannel.id();
    serverBossGroup
        .shutdownGracefully()
        .addListener(
            future -> {
              if (future.isDone()) {
                log.info(
                    "The bossGroup of bonecrusher[{}:{}--->id:{}] has been shutdown {}.",
                    serverProperties.getAddress(),
                    serverProperties.getPort(),
                    id,
                    future.isSuccess() ? "successfully" : "unsuccessfully");
              }
            });
    serverConnectorsGroup
        .shutdownGracefully()
        .addListener(
            future -> {
              if (future.isDone()) {
                log.info(
                    "The workersGroup of bonecrusher[{}:{}--->id:{}] has been shutdown {}.",
                    serverProperties.getAddress(),
                    serverProperties.getPort(),
                    id,
                    future.isSuccess() ? "successfully" : "unsuccessfully");
              }
            });
  }

  @Getter
  public class BonecrusherClient implements DataSyncClient {

    private EventLoopGroup clientConnectorsGroup;

    private Bootstrap bootstrap;

    private Channel clientChannel;

    @Override
    public DataSyncClient connect(String remoteAddress, int remotePort) throws Exception {
      joinClusterLatch.await(); // 加入集群后才能创建客户端
      this.clientChannel = bootstrap.connect(remoteAddress, remotePort).sync().channel();
      return this;
    }

    @Override
    public <T, R> R sendSync(T req) throws Exception {
      return null;
    }

    @Override
    public void close() {
      ChannelId id = clientChannel.id();
      clientConnectorsGroup
          .shutdownGracefully()
          .addListener(
              future -> {
                if (future.isDone()) {
                  log.info(
                      "The client[{}--->id:{}] of bonecrusher has been shutdown {}.",
                      clientChannel.localAddress(),
                      id,
                      future.isSuccess() ? "successfully" : "unsuccessfully");
                }
              });
    }

    /** 构造方法 */
    public BonecrusherClient() {
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
              .option(SO_REUSEADDR, serverProperties.getNetty().isSO_REUSEADDR())
              .option(
                  CONNECT_TIMEOUT_MILLIS,
                  (int) serverProperties.getNetty().getCONNECT_TIMEOUT_MILLIS().toMillis())
              .handler(
                  new ChannelInitializer<UdtChannel>() {
                    @Override
                    public void initChannel(UdtChannel ch) {
                      ch.pipeline()
                          .addLast(loggingHandler)
                          .addLast(
                              new ClientAuthChannelHandler(
                                      clientProperties,
                                      serverAuthChannelHandler.getJwtAuthenticator())
                                  .clusterViewChangedEventSupplier(
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
  public void setLoggingHandler(LoggingHandler loggingHandler) {
    this.loggingHandler = loggingHandler;
  }

  @Autowired
  public void setFileServerHandler(FileServerHandler fileServerHandler) {
    this.fileServerHandler = fileServerHandler;
  }

  @Autowired
  public void setAuthChannelHandler(ServerAuthChannelHandler serverAuthChannelHandler) {
    this.serverAuthChannelHandler = serverAuthChannelHandler;
  }

  @Autowired
  public void setProtobufDecoder(ProtobufDecoder protobufDecoder) {
    this.protobufDecoder = protobufDecoder;
  }

  @Autowired
  public void setProtobufEncoder(ProtobufEncoder protobufEncoder) {
    this.protobufEncoder = protobufEncoder;
  }

  @Autowired
  public void setServerProperties(BonecrusherProperties serverProperties) {
    this.serverProperties = serverProperties;
  }

  @Autowired
  public void setClientProperties(BonecrusherClientProperties clientProperties) {
    this.clientProperties = clientProperties;
  }
}
