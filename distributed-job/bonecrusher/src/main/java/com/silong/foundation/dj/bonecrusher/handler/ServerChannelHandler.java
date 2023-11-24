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

import static com.silong.foundation.dj.bonecrusher.message.Messages.Type.AUTHENTICATION_FAILED_RESP;

import com.auth0.jwt.interfaces.Claim;
import com.silong.foundation.dj.bonecrusher.configure.config.BonecrusherServerProperties;
import com.silong.foundation.dj.bonecrusher.enu.ErrorCode;
import com.silong.foundation.dj.bonecrusher.message.Messages;
import com.silong.foundation.dj.bonecrusher.message.Messages.Request;
import com.silong.foundation.dj.bonecrusher.message.Messages.ResponseHeader;
import com.silong.foundation.dj.bonecrusher.vo.ClusterInfo;
import com.silong.foundation.dj.hook.auth.JwtAuthenticator;
import com.silong.foundation.dj.hook.auth.JwtAuthenticator.Result;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.udt.nio.NioUdtProvider;
import io.netty.handler.timeout.IdleStateEvent;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/**
 * Server Channel Handler
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-21 16:41
 */
@Slf4j
@Sharable
public class ServerChannelHandler extends ChannelDuplexHandler {
  static final String CLUSTER_KEY = "cluster";

  static final String GENERATOR_KEY = "generator";

  private static final Messages.Result.Builder AUTHENTICATION_FAILED =
      Messages.Result.newBuilder()
          .setCode(ErrorCode.AUTHENTICATION_FAILED.getCode())
          .setDesc(ErrorCode.AUTHENTICATION_FAILED.getDesc());

  /** 鉴权工具 */
  @Getter private final JwtAuthenticator jwtAuthenticator;

  /** 配置 */
  private final BonecrusherServerProperties properties;

  /** 集群信息 */
  @Setter
  @Accessors(fluent = true)
  private Supplier<ClusterInfo> clusterInfoSupplier;

  /**
   * 构造方法
   *
   * @param properties 配置
   * @param jwtAuthenticator 认证处理器
   */
  public ServerChannelHandler(
      @NonNull BonecrusherServerProperties properties, @NonNull JwtAuthenticator jwtAuthenticator) {
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
    switch (msg) {
      case Request request -> {
        // 执行签名认证
        Result result = jwtAuthenticator.verify(request.getToken(), this::checkTokenPayload);

        // 鉴权成功后消息往后续pipeline中的handler传递，处理，否则返回错误响应
        if (!result.isValid()) {
          ctx.writeAndFlush(
              ResponseHeader.newBuilder()
                  .setType(AUTHENTICATION_FAILED_RESP)
                  .setResult(AUTHENTICATION_FAILED)
                  .setTimestamp(System.currentTimeMillis())
                  .setUuid(request.getUuid())
                  .build());
          return;
        }
      }
      case null -> throw new IllegalArgumentException("msg must not be null or empty.");
      default -> {}
    }
    ctx.fireChannelRead(msg);
  }

  private Result checkTokenPayload(Map<String, Claim> claims) {
    ClusterInfo clusterInfo = clusterInfoSupplier.get();
    if (log.isDebugEnabled()) {
      log.debug("clusterInfo: {}, claims: {}", clusterInfo, claims);
    }

    if (clusterInfo.clusterName() == null) {
      log.error("The local node has not joined the cluster or has left the cluster.");
    } else {
      Claim cluster = claims.get(CLUSTER_KEY);
      Claim generator = claims.get(GENERATOR_KEY);
      if (cluster != null
          && generator != null
          && Objects.equals(cluster.asString(), clusterInfo.clusterName())
          && Arrays.stream(clusterInfo.view().getMembersRaw())
              .anyMatch(
                  memberAddress ->
                      memberAddress.equals(clusterInfo.localAddress())) // 确保服务端归属节点在集群内
          && Arrays.stream(clusterInfo.view().getMembersRaw())
              .anyMatch(
                  memberAddress ->
                      memberAddress.toString().equals(generator.asString())) // 确保请求客户端节点在集群内
      ) {
        return Result.VALID;
      }
    }
    return new Result(false, "Invalid token.");
  }
}
