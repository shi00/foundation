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
import com.silong.foundation.dj.bonecrusher.exception.RequestResponseException;
import com.silong.foundation.dj.bonecrusher.message.Messages;
import com.silong.foundation.lambda.Tuple2;
import com.silong.foundation.lambda.Tuple3;
import com.silong.foundation.utilities.jwt.JwtAuthenticator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.udt.nio.NioUdtProvider;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Promise;
import java.util.Map;
import java.util.concurrent.CancellationException;
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

  /** 集群视图 */
  @Setter
  @Accessors(fluent = true)
  private Supplier<ClusterViewChangedEvent> clusterViewChangedEventSupplier;

  /** 鉴权工具 */
  private final JwtAuthenticator jwtAuthenticator;

  /** 配置 */
  private final BonecrusherClientProperties clientProperties;

  private final Executor executor;

  private final Cache<String, Tuple2<Messages.Request, Promise>> cache;

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
    this.cache =
        Caffeine.newBuilder()
            .initialCapacity(clientProperties.getExpectedConcurrentRequests())
            .maximumSize(clientProperties.getMaximumConcurrentRequests())
            .executor(executor)
            .expireAfterWrite(clientProperties.getRequestTimeout())
            .scheduler(Scheduler.systemScheduler())
            .evictionListener(buildRemovalListener(clientProperties))
            .build();
  }

  private RemovalListener<String, Tuple2<Messages.Request, Promise>> buildRemovalListener(
      BonecrusherClientProperties clientProperties) {
    return (uuid, tuple2, cause) -> {
      // 只有垃圾收集导致的淘汰promise才会为null，因此过滤此种场景
      if (tuple2 != null) {
        switch (cause) {
          case EXPLICIT -> {
            log.info("Request canceled: {}", tuple2.t1());
            tuple2.t2().setFailure(new CancellationException());
          }
          case EXPIRED -> // 请求超时
          {
            log.info(
                "Timeout Threshold: {}s, Request: {}",
                clientProperties.getRequestTimeout().toSeconds(),
                tuple2.t1());
            tuple2.t2().setFailure(new TimeoutException());
          }
          case SIZE -> // 超出并发请求数上限淘汰
          {
            log.info(
                "The number of concurrent requests exceeded the limit of {}. Discard Request: {}",
                clientProperties.getMaximumConcurrentRequests(),
                tuple2.t1());
            tuple2.t2().setFailure(new ConcurrentRequestLimitExceededException());
          }
        }
      }
    };
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof CompositeByteBuf compositeByteBuf && compositeByteBuf.numComponents() == 2) {
      try {
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
        Tuple2<Messages.Request, Promise> value = cache.asMap().remove(response.getUuid());
        if (value != null) {
          log.info("Request: {}, Response: {}", value.t1(), response);
          if (response.hasResult()) {
            value.t2().setFailure(new RequestResponseException());
          } else {
            ByteBuf data = compositeByteBuf.component(1);
            array = ByteBufUtil.getBytes(data, data.readerIndex(), data.readableBytes(), true);
            value.t2().setSuccess(array);
          }
        }
      } finally {
        ReferenceCountUtil.release(compositeByteBuf);
      }
      return;
    }
    ctx.fireChannelRead(msg);
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
    if (msg instanceof Tuple3 tuple3) {
      Object req = tuple3.t1();
      Promise<?> cPromise = (Promise<?>) tuple3.t2();
      String uuid = (String) tuple3.t3();
      String token = getToken();

      // 为请求添加鉴权token和uuid
      if (req instanceof Messages.Request request) {
        msg = cache(request.toBuilder().setToken(token).setUuid(uuid).build(), cPromise);
      } else if (req instanceof Messages.Request.Builder builder) {
        msg = cache(builder.setToken(token).setUuid(uuid).build(), cPromise);
      }
    }
    ctx.write(msg, promise);
  }

  /**
   * 取消请求
   *
   * @param reqUuid 请求uuid
   */
  public void cancelRequest(@NonNull String reqUuid) {
    cache.invalidate(reqUuid);
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

  private Messages.Request cache(Messages.Request request, Promise<?> promise) {
    cache.put(
        request.getUuid(),
        Tuple2.<Messages.Request, Promise>builder().t1(request).t2(promise).build());
    return request;
  }

  private String getToken() {
    ClusterViewChangedEvent event = clusterViewChangedEventSupplier.get();
    return jwtAuthenticator.generate(
        Map.of(CLUSTER_KEY, event.cluster(), GENERATOR_KEY, event.localAddress().toString()));
  }
}
