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

import static com.silong.foundation.dj.bonecrusher.handler.ServerChannelHandler.CLUSTER_KEY;
import static com.silong.foundation.dj.bonecrusher.handler.ServerChannelHandler.GENERATOR_KEY;

import com.github.benmanes.caffeine.cache.*;
import com.silong.foundation.dj.bonecrusher.configure.config.BonecrusherClientProperties;
import com.silong.foundation.dj.bonecrusher.event.ClusterViewChangedEvent;
import com.silong.foundation.dj.bonecrusher.exception.ConcurrentRequestLimitExceededException;
import com.silong.foundation.dj.bonecrusher.message.Messages;
import com.silong.foundation.lambda.Tuple2;
import com.silong.foundation.utilities.jwt.JwtAuthenticator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.udt.nio.NioUdtProvider;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Promise;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/**
 * Client Channel Handler 收发消息
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-21 16:41
 */
@Sharable
@Slf4j
public class ClientChannelHandler extends ChannelDuplexHandler {

  private static final AttributeKey<Cache<String, Tuple2<Messages.Request, Promise>>>
      REQUEST_CACHE_KEY = AttributeKey.valueOf("bonecrusher.request.promise");

  /** 集群视图 */
  @Setter
  @Accessors(fluent = true)
  private Supplier<ClusterViewChangedEvent> clusterViewChangedEventSupplier;

  /** 鉴权工具 */
  private final JwtAuthenticator jwtAuthenticator;

  /** 配置 */
  private final BonecrusherClientProperties clientProperties;

  /** 请求超时监听器 */
  private final RemovalListener<String, Tuple2<Messages.Request, Promise>> requestTimeoutlListener;

  private final Executor executor;

  private ChannelHandlerContext handlerContext;

  /**
   * 构造方法
   *
   * @param clientProperties 配置
   * @param executor 执行器
   * @param jwtAuthenticator 认证处理器
   */
  public ClientChannelHandler(
      @NonNull BonecrusherClientProperties clientProperties,
      @NonNull Executor executor,
      @NonNull JwtAuthenticator jwtAuthenticator) {
    this.clientProperties = clientProperties;
    this.executor = executor;
    this.jwtAuthenticator = jwtAuthenticator;
    this.requestTimeoutlListener =
        (uuid, tuple2, cause) -> {

          // 只有垃圾收集导致的淘汰promise才会为null，因此过滤此种场景
          if (tuple2 != null) {
            switch (cause) {
              case EXPIRED -> // 请求超时
              tuple2
                  .t2()
                  .setFailure(
                      new TimeoutException(
                          String.format(
                              "Timeout Threshold: %ss, Request: %s",
                              clientProperties.getRequestTimeout().toSeconds(), tuple2.t1())));
              case SIZE -> // 超出并发请求数上限淘汰
              tuple2
                  .t2()
                  .setFailure(
                      new ConcurrentRequestLimitExceededException(
                          String.format(
                              "The number of concurrent requests exceeded the limit of %d. Discard Request: %s",
                              clientProperties.getMaximumConcurrentRequests(), tuple2.t1())));
            }
          }
        };
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    // 创建请求缓存，并缓存
    ctx.channel()
        .attr(REQUEST_CACHE_KEY)
        .set(
            Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(clientProperties.getMaximumConcurrentRequests())
                .executor(executor)
                .expireAfterWrite(clientProperties.getRequestTimeout())
                .scheduler(Scheduler.systemScheduler())
                .evictionListener(requestTimeoutlListener)
                .build());
    handlerContext = ctx;
    ctx.fireChannelActive();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof CompositeByteBuf compositeByteBuf && compositeByteBuf.numComponents() == 2) {
      ByteBuf respBuf = compositeByteBuf.component(0);
      byte[] array;
      int offset;
      int length = respBuf.readableBytes();
      if (respBuf.hasArray()) {
        array = respBuf.array();
        offset = respBuf.arrayOffset() + respBuf.readerIndex();
      } else {
        array = ByteBufUtil.getBytes(respBuf, respBuf.readerIndex(), length, false);
        offset = 0;
      }
      Messages.Response response = Messages.Response.parser().parseFrom(array, offset, length);
      if (response.getResult() != null) {
        Tuple2<Messages.Request, Promise> value =
            ctx.channel().attr(REQUEST_CACHE_KEY).get().getIfPresent(response.getUuid());
      }
    }
    ctx.fireChannelRead(msg);
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
    if (msg instanceof Tuple2 tuple2) {
      Object req = tuple2.t1();
      Promise cPromise = (Promise) tuple2.t2();
      Messages.Request r = null;

      // 为请求添加鉴权token和uuid
      if (req instanceof Messages.Request request) {
        cache(r = buildRequest(request.toBuilder()), cPromise);
      } else if (req instanceof Messages.Request.Builder builder) {
        cache(r = buildRequest(builder), cPromise);
      }
      msg = r == null ? msg : r;
    }
    ctx.write(msg, promise);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    log.error(
        "The channel[id:{}] is closed between client:{} and server:{}. {}details:{}",
        ctx.channel().id(),
        ctx.channel().localAddress(),
        ctx.channel().remoteAddress(),
        System.lineSeparator(),
        NioUdtProvider.socketUDT(ctx.channel()).toStringOptions(),
        cause);
    ctx.close();
  }

  private void cache(Messages.Request request, Promise promise) {
    handlerContext
        .channel()
        .attr(REQUEST_CACHE_KEY)
        .get()
        .put(
            request.getUuid(),
            Tuple2.<Messages.Request, Promise>builder().t1(request).t2(promise).build());
  }

  private Messages.Request buildRequest(Messages.Request.Builder builder) {
    return builder.setToken(getToken()).setUuid(UUID.randomUUID().toString()).build();
  }

  private String getToken() {
    ClusterViewChangedEvent event = clusterViewChangedEventSupplier.get();
    return jwtAuthenticator.generate(
        Map.of(CLUSTER_KEY, event.cluster(), GENERATOR_KEY, event.localAddress().toString()));
  }
}
