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

import com.silong.foundation.dj.bonecrusher.configure.config.BonecrusherProperties;
import com.silong.foundation.dj.bonecrusher.event.Bonecrusher.SimpleClusterView;
import com.silong.foundation.dj.bonecrusher.handler.AuthChannelHandler;
import com.silong.foundation.dj.bonecrusher.handler.FileServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.udt.UdtChannel;
import io.netty.channel.udt.nio.NioUdtProvider;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.scheduling.annotation.Async;

/**
 * UDT数据同步平台
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-24 15:48
 */
@Slf4j
public class Bonecrusher implements ApplicationListener<PayloadApplicationEvent<byte[]>> {

  private final BonecrusherProperties properties;

  private final EventLoopGroup bossGroup;

  private final EventLoopGroup workersGroup;

  private final ServerBootstrap serverBootstrap;

  /** 保存当前集群视图 */
  private final AtomicReference<SimpleClusterView> simpleClusterViewRef = new AtomicReference<>();

  /** 加入集群latch，确保服务只能在节点加入集群后启动 */
  private final CountDownLatch joinClusterLatch = new CountDownLatch(1);

  /**
   * 构造方法
   *
   * @param properties 配置
   * @param loggingHandler 日志打印处理器
   * @param authChannelHandler 鉴权处理器
   * @param fileServerHandler 文件下载处理器
   */
  public Bonecrusher(
      @NonNull BonecrusherProperties properties,
      @NonNull LoggingHandler loggingHandler,
      @NonNull AuthChannelHandler authChannelHandler,
      @NonNull FileServerHandler fileServerHandler) {
    this.properties = properties;
    this.serverBootstrap =
        new ServerBootstrap()
            .group(
                this.bossGroup =
                    new NioEventLoopGroup(
                        properties.getBossGroupThreads(),
                        new DefaultThreadFactory("Acceptor"),
                        NioUdtProvider.BYTE_PROVIDER),
                this.workersGroup =
                    new NioEventLoopGroup(
                        properties.getWorkerGroupThreads(),
                        new DefaultThreadFactory("Connector"),
                        NioUdtProvider.BYTE_PROVIDER))
            // 设置服务端通道实现类型
            .channelFactory(NioUdtProvider.BYTE_ACCEPTOR)
            .handler(loggingHandler)
            // 设置子channel的缓冲区分配器
            .option(ChannelOption.SO_BACKLOG, properties.getNetty().getSO_BACKLOG())
            .option(ChannelOption.SO_REUSEADDR, properties.getNetty().isSO_REUSEADDR())
            .option(
                ChannelOption.CONNECT_TIMEOUT_MILLIS,
                (int) properties.getNetty().getCONNECT_TIMEOUT_MILLIS().toMillis())
            .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
            .childHandler(
                new ChannelInitializer<UdtChannel>() {
                  @Override
                  protected void initChannel(UdtChannel ch) {
                    ch.pipeline()
                        .addLast(loggingHandler)
                        .addLast(authChannelHandler.clusterViewSupplier(simpleClusterViewRef::get))
                        .addLast(new ChunkedWriteHandler())
                        .addLast(fileServerHandler);
                  }
                });
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
    ChannelFuture channelFuture =
        serverBootstrap.bind(properties.getAddress(), properties.getPort()).sync();

    log.info("The bonecrusher has been started successfully.");

    // 对关闭通道进行监听
    channelFuture.channel().closeFuture().sync();
  }

  /** 异步关闭服务 */
  public void shutdown() {
    bossGroup.shutdownGracefully();
    workersGroup.shutdownGracefully();
    log.info("The bonecrusher has been shutdown gracefully.");
  }

  /**
   * 接收集群视图变化事件
   *
   * @param event the event to respond to 集群视图变化
   */
  @Async
  @Override
  public void onApplicationEvent(PayloadApplicationEvent<byte[]> event) {
    try {
      SimpleClusterView simpleClusterView = SimpleClusterView.parseFrom(event.getPayload());
      log.info("receive: {}", simpleClusterView);
      simpleClusterViewRef.set(simpleClusterView);
      joinClusterLatch.countDown();
    } catch (Exception e) {
      log.error("Failed to parse {}", event.getPayload(), e);
    }
  }
}
