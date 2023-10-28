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

package com.silong.foundation.dj.bonecrusher.handler;

import static com.silong.foundation.dj.bonecrusher.handler.ServerAuthChannelHandler.CLUSTER_KEY;
import static com.silong.foundation.dj.bonecrusher.handler.ServerAuthChannelHandler.GENERATOR_KEY;

import com.silong.foundation.dj.bonecrusher.configure.config.BonecrusherClientProperties;
import com.silong.foundation.dj.bonecrusher.event.ClusterViewChangedEvent;
import com.silong.foundation.dj.bonecrusher.message.Messages;
import com.silong.foundation.dj.bonecrusher.message.Messages.LoadingClassReq;
import com.silong.foundation.utilities.jwt.JwtAuthenticator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.util.Map;
import java.util.function.Supplier;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jgroups.Address;
import org.jgroups.stack.IpAddress;

/**
 * Channel鉴权处理器，接收集群
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-21 16:41
 */
@Sharable
@Slf4j
public class ClientAuthChannelHandler extends ChannelInboundHandlerAdapter {

  /** 集群视图 */
  @Setter
  @Accessors(fluent = true)
  private Supplier<ClusterViewChangedEvent> clusterViewChangedEventSupplier;

  /** 鉴权工具 */
  private final JwtAuthenticator jwtAuthenticator;

  /** 配置 */
  private final BonecrusherClientProperties clientProperties;

  /**
   * 构造方法
   *
   * @param clientProperties 配置
   * @param jwtAuthenticator 认证处理器
   */
  public ClientAuthChannelHandler(
      @NonNull BonecrusherClientProperties clientProperties,
      @NonNull JwtAuthenticator jwtAuthenticator) {
    this.clientProperties = clientProperties;
    this.jwtAuthenticator = jwtAuthenticator;
  }

  /**
   * channel被激活后立即初始化上下文中保存的信息
   *
   * @param ctx 上下文
   */
  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    ClusterViewChangedEvent event = clusterViewChangedEventSupplier.get();
    Address address = new IpAddress("127.0.0.1:43434");
    ctx.writeAndFlush(
        Unpooled.wrappedBuffer(
            Messages.Request.newBuilder()
                .setType(Messages.Type.LOADING_CLASS_REQ)
                .setToken(
                    jwtAuthenticator.generate(
                        Map.of(CLUSTER_KEY, event.cluster(), GENERATOR_KEY, address.toString())))
                .setLoadingClass(
                    LoadingClassReq.newBuilder()
                        .setClassFqdn("com.silong.foundation.dj.bonecrusher.DataSyncClient"))
                .build()
                .toByteArray()));
    //    if (log.isInfoEnabled()) {
    //      Channel channel = ctx.channel();
    //      SocketAddress clientAddress = channel.remoteAddress();
    //      SocketAddress socketAddress = channel.localAddress();
    //      log.info(
    //          "The channel is activated between client:{} and server:{}. details:{}",
    //          clientAddress,
    //          socketAddress,
    //          NioUdtProvider.socketUDT(channel).toStringOptions());
    //    }
    ctx.fireChannelActive();
  }
}
