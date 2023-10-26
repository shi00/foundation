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

import static com.silong.foundation.dj.bonecrusher.message.ErrorCode.*;
import static com.silong.foundation.dj.bonecrusher.message.Messages.Type.LOGIN_REQ;
import static com.silong.foundation.dj.bonecrusher.message.Messages.Type.LOGIN_RESP;

import com.auth0.jwt.interfaces.Claim;
import com.google.protobuf.Message;
import com.silong.foundation.dj.bonecrusher.event.Bonecrusher.SimpleClusterView;
import com.silong.foundation.dj.bonecrusher.message.Messages;
import com.silong.foundation.dj.bonecrusher.message.Messages.LoginReq;
import com.silong.foundation.dj.bonecrusher.message.Messages.LoginResp;
import com.silong.foundation.dj.bonecrusher.message.Messages.Request;
import com.silong.foundation.utilities.jwt.JwtAuthenticator;
import com.silong.foundation.utilities.jwt.Result;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.udt.nio.NioUdtProvider;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Channel鉴权处理器，接收集群
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-21 16:41
 */
@Sharable
@Slf4j
public class AuthChannelHandler extends SimpleChannelInboundHandler<ByteBuf> {
  private static final AttributeKey<JwtAuthenticator> AUTHENTICATOR_KEY =
      AttributeKey.valueOf("jwt-authenticator");

  private static final AttributeKey<Boolean> HAS_LOGGED = AttributeKey.valueOf("has-logged");

  private static final String CLUSTER_NAME_KEY = "cluster";

  private static final String GENERATOR_NAME_KEY = "generator";

  private final Supplier<SimpleClusterView> clusterViewSupplier;

  /** 鉴权工具 */
  private JwtAuthenticator jwtAuthenticator;

  /**
   * 构造方法
   *
   * @param clusterViewSupplier 集群视图提供者
   */
  public AuthChannelHandler(@NonNull Supplier<SimpleClusterView> clusterViewSupplier) {
    super(true);
    this.clusterViewSupplier = clusterViewSupplier;
  }

  /**
   * channel被激活后立即初始化上下文中保存的信息
   *
   * @param ctx 上下文
   */
  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    attr(ctx, AUTHENTICATOR_KEY).set(jwtAuthenticator);
    attr(ctx, HAS_LOGGED).set(Boolean.FALSE);
    log.info("channel is activated. {}", NioUdtProvider.socketUDT(ctx.channel()).toStringOptions());
    ctx.fireChannelActive();
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
    Request request = Request.parseFrom(msg.array());

    // 已登录，并且不是登录请求，则直接跳转至后续handler处理
    if (attr(ctx, HAS_LOGGED).get() && LOGIN_REQ != request.getType()) {
      ctx.fireChannelRead(request);
    } else {
      if (LOGIN_REQ == request.getType()) {
        LoginReq login = request.getLogin();
        String token = login.getToken();
        String generator = login.getGenerator();
        Result result =
            attr(ctx, AUTHENTICATOR_KEY)
                .get()
                .verify(token, claims -> checkTokenPayload(claims, generator));

        // 鉴权成功后设置登录标识
        if (result.isValid()) {
          attr(ctx, HAS_LOGGED).set(Boolean.TRUE);
        }

        writeAndFlush(
            ctx,
            buildLoginResp(
                () ->
                    Messages.Result.newBuilder()
                        .setCode(
                            result.isValid() ? LOGIN_SUCCESSFUL.getCode() : LOGIN_FAILED.getCode())
                        .setDesc(
                            result.isValid()
                                ? LOGIN_SUCCESSFUL.getDesc()
                                : LOGIN_FAILED.getDesc())));
      } else {
        writeAndFlush(
            ctx,
            buildLoginResp(
                () ->
                    Messages.Result.newBuilder()
                        .setCode(PERFORM_OPERATIONS_WITHOUT_LOGGING_IN.getCode())
                        .setDesc(PERFORM_OPERATIONS_WITHOUT_LOGGING_IN.getDesc())));
      }
    }
  }

  private Messages.Response buildLoginResp(Supplier<Messages.Result.Builder> supplier) {
    return Messages.Response.newBuilder()
        .setLogin(LoginResp.newBuilder().setResult(supplier.get()))
        .setType(LOGIN_RESP)
        .build();
  }

  private void writeAndFlush(ChannelHandlerContext ctx, Message response) {
    ctx.writeAndFlush(Unpooled.wrappedBuffer(response.toByteArray()));
  }

  private <T> Attribute<T> attr(ChannelHandlerContext ctx, AttributeKey<T> key) {
    return ctx.channel().attr(key);
  }

  private Result checkTokenPayload(Map<String, Claim> claims, String generator) {
    Claim clusterName = claims.get(CLUSTER_NAME_KEY);
    Claim generatorName = claims.get(GENERATOR_NAME_KEY);
    SimpleClusterView simpleClusterView = clusterViewSupplier.get();
    if (clusterName != null
        && generatorName != null
        && simpleClusterView != null
        && Objects.equals(clusterName.asString(), simpleClusterView.getClusterName())
        && simpleClusterView.getAllMembersNameList().contains(generatorName.asString())
        && Objects.equals(generatorName.asString(), generator)) {
      return Result.VALID;
    }
    return new Result(false, "Invalid token.");
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    log.error(
        "An exception occurs that causes the channel to be closed. {}",
        NioUdtProvider.socketUDT(ctx.channel()).toStringOptions());
    ctx.close();
  }

  @Autowired
  public void setJwtAuthenticator(JwtAuthenticator jwtAuthenticator) {
    this.jwtAuthenticator = jwtAuthenticator;
  }
}
