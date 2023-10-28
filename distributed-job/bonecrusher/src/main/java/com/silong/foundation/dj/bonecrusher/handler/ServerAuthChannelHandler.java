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

import static com.silong.foundation.dj.bonecrusher.message.ErrorCode.AUTHENTICATION_FAILED;
import static com.silong.foundation.dj.bonecrusher.message.Messages.Type.AUTHENTICATION_FAILED_RESP;

import com.auth0.jwt.interfaces.Claim;
import com.silong.foundation.dj.bonecrusher.configure.config.BonecrusherProperties;
import com.silong.foundation.dj.bonecrusher.event.ClusterViewChangedEvent;
import com.silong.foundation.dj.bonecrusher.message.Messages;
import com.silong.foundation.dj.bonecrusher.message.Messages.Request;
import com.silong.foundation.utilities.jwt.JwtAuthenticator;
import com.silong.foundation.utilities.jwt.Result;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.udt.nio.NioUdtProvider;
import io.netty.handler.timeout.IdleStateEvent;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/**
 * Channel鉴权处理器，接收集群
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-21 16:41
 */
@Slf4j
@Sharable
public class ServerAuthChannelHandler extends ChannelDuplexHandler {
  static final String CLUSTER_KEY = "cluster";

  static final String GENERATOR_KEY = "generator";

  /** 鉴权工具 */
  @Getter private final JwtAuthenticator jwtAuthenticator;

  /** 配置 */
  private final BonecrusherProperties properties;

  /** 集群视图 */
  @Setter
  @Accessors(fluent = true)
  private Supplier<ClusterViewChangedEvent> clusterViewChangedEventSupplier;

  /**
   * 构造方法
   *
   * @param properties 配置
   * @param jwtAuthenticator 认证处理器
   */
  public ServerAuthChannelHandler(
      @NonNull BonecrusherProperties properties, @NonNull JwtAuthenticator jwtAuthenticator) {
    this.properties = properties;
    this.jwtAuthenticator = jwtAuthenticator;
  }

  /**
   * channel空闲事件发生时，关闭channel
   *
   * @param ctx 上下文
   * @param evt 事件
   */
  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
    if (evt instanceof IdleStateEvent e) {
      switch (e.state()) {
        case ALL_IDLE -> log.info(
            "ALL_IDLE[{}] threshold reached, the channel[{}] closed.",
            properties.getIdleState().getAllIdleTime().toSeconds() + "s",
            ctx.channel().id());
        case READER_IDLE -> log.info(
            "READER_IDLE[{}] threshold reached, the channel[{}] closed.",
            properties.getIdleState().getReaderIdleTime().toSeconds() + "s",
            ctx.channel().id());
        case WRITER_IDLE -> log.info(
            "WRITER_IDLE[{}] threshold reached, the channel[{}] closed.",
            properties.getIdleState().getWriterIdleTime().toSeconds() + "s",
            ctx.channel().id());
      }
      ctx.close(); // 服务端发起关闭通道
    }
  }

  /**
   * channel被激活后立即初始化上下文中保存的信息
   *
   * @param ctx 上下文
   */
  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    if (log.isInfoEnabled()) {
      log.info(
          "The channel[id:{}] is activated between client:{} and server:{}. {}details:{}",
          ctx.channel().id(),
          ctx.channel().remoteAddress(),
          ctx.channel().localAddress(),
          System.lineSeparator(),
          NioUdtProvider.socketUDT(ctx.channel()).toStringOptions());
    }
    ctx.fireChannelActive();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    Request request = (Request) msg;

    // 执行签名认证
    Result result = jwtAuthenticator.verify(request.getToken(), this::checkTokenPayload);

    // 鉴权成功后消息往后续pipeline中的handler传递，处理
    if (result.isValid()) {
      ctx.fireChannelRead(request);
    } else {
      ctx.writeAndFlush(
          Messages.Response.newBuilder()
              .setType(AUTHENTICATION_FAILED_RESP)
              .setResult(
                  Messages.Result.newBuilder()
                      .setCode(AUTHENTICATION_FAILED.getCode())
                      .setDesc(AUTHENTICATION_FAILED.getDesc()))
              .build());
    }
  }

  private Result checkTokenPayload(Map<String, Claim> claims) {
    ClusterViewChangedEvent clusterViewChangedEvent = clusterViewChangedEventSupplier.get();
    if (log.isDebugEnabled()) {
      log.debug("clusterViewChangedEvent: {}, claims: {}", clusterViewChangedEvent, claims);
    }

    if (clusterViewChangedEvent == null) {
      log.error("The local node has not joined the cluster or has left the cluster.");
    } else {
      Claim cluster = claims.get(CLUSTER_KEY);
      Claim generator = claims.get(GENERATOR_KEY);
      if (cluster != null
          && generator != null
          && Objects.equals(cluster.asString(), clusterViewChangedEvent.cluster())
          && clusterViewChangedEvent.newView().getMembers().stream()
              .anyMatch(member -> member.toString().equals(generator.asString()))) {
        return Result.VALID;
      }
    }
    return new Result(false, "Invalid token.");
  }
}
