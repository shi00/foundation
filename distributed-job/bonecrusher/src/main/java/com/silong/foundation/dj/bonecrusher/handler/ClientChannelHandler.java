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
import static io.netty.channel.udt.nio.NioUdtProvider.socketUDT;

import com.github.benmanes.caffeine.cache.*;
import com.silong.foundation.dj.bonecrusher.configure.config.BonecrusherClientProperties;
import com.silong.foundation.dj.bonecrusher.event.ClusterViewChangedEvent;
import com.silong.foundation.dj.bonecrusher.exception.ConcurrentRequestLimitExceededException;
import com.silong.foundation.dj.bonecrusher.exception.RequestResponseException;
import com.silong.foundation.dj.bonecrusher.message.Messages.DataBlockMetadata;
import com.silong.foundation.dj.bonecrusher.message.Messages.Request;
import com.silong.foundation.dj.bonecrusher.message.Messages.ResponseHeader;
import com.silong.foundation.lambda.Tuple2;
import com.silong.foundation.lambda.Tuple3;
import com.silong.foundation.utilities.jwt.JwtAuthenticator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Promise;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.*;
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

  /** 请求缓存 */
  @SuppressWarnings("rawtypes")
  private final Cache<String, Tuple3<Request, Promise, LinkedList<Tuple2<Integer, ByteBuf>>>> cache;

  /** 鉴权工具 */
  private final JwtAuthenticator jwtAuthenticator;

  private final Comparator<Tuple2<Integer, ByteBuf>> dataBlockComparing =
      Comparator.comparing(Tuple2::t1);

  /** 配置 */
  private final BonecrusherClientProperties clientProperties;

  /** 执行器 */
  private final Executor executor;

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
            .expireAfterAccess(clientProperties.getRequestTimeout())
            .scheduler(Scheduler.systemScheduler())
            .evictionListener(buildRemovalListener(clientProperties))
            .build();
  }

  @SuppressWarnings("rawtypes")
  private RemovalListener<String, Tuple3<Request, Promise, LinkedList<Tuple2<Integer, ByteBuf>>>>
      buildRemovalListener(BonecrusherClientProperties clientProperties) {
    return (uuid, tuple3, cause) -> {
      // 只有垃圾收集导致的淘汰tuples才会为null，因此过滤此种场景
      if (tuple3 != null) {
        switch (cause) {
          case EXPIRED -> { // 请求超时
            try {
              tuple3
                  .t2()
                  .tryFailure(
                      new TimeoutException(
                          String.format(
                              "Timeout Threshold: %ss, Request: %s",
                              clientProperties.getRequestTimeout().toSeconds(), tuple3.t1())));
            } finally {
              release(tuple3.t3());
            }
          }
          case SIZE -> // 超出并发请求数上限淘汰
          {
            try {
              tuple3
                  .t2()
                  .tryFailure(
                      new ConcurrentRequestLimitExceededException(
                          String.format(
                              "The number of concurrent requests exceeded the limit of %d. Discard Request: %s",
                              clientProperties.getMaximumConcurrentRequests(), tuple3.t1())));
            } finally {
              release(tuple3.t3());
            }
          }
        }
      }
    };
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    switch (msg) {
      case Tuple2 t -> {
        Tuple2<ResponseHeader, ByteBuf> tuple2 = (Tuple2<ResponseHeader, ByteBuf>) t;
        ResponseHeader header = tuple2.t1();
        ByteBuf buf = tuple2.t2();
        try {
          // 失败响应
          if (header.hasResult()) {
            Tuple3<Request, Promise, LinkedList<Tuple2<Integer, ByteBuf>>> tuple3 =
                cache.asMap().remove(header.getUuid());
            if (tuple3 != null) {
              try {
                tuple3
                    .t2()
                    .tryFailure(
                        new RequestResponseException(
                            "Abnormal response: " + header.getResult().getDesc()));
              } finally {
                release(tuple3.t3());
              }
            }
          } else {
            Tuple3<Request, Promise, LinkedList<Tuple2<Integer, ByteBuf>>> tuple3 =
                cache.getIfPresent(header.getUuid());
            if (tuple3 != null) {
              DataBlockMetadata metadata = header.getDataBlockMetadata();
              LinkedList<Tuple2<Integer, ByteBuf>> bufList = tuple3.t3();
              Promise promise = tuple3.t2();
              int blockNo = metadata.getBlockNo();
              bufList.add(
                  Tuple2.<Integer, ByteBuf>builder()
                      .t1(blockNo)
                      .t2(buf.retain()) // 引用计数+1，避免被netty内存管理回收
                      .build());

              // 所有数据块都收到后设置结果
              if (metadata.getTotalBlocks() == bufList.size()) {
                CompositeByteBuf byteBufs =
                    ctx.alloc()
                        .compositeBuffer(bufList.size())
                        .addComponents(
                            true,
                            bufList.stream().sorted(dataBlockComparing).map(Tuple2::t2).toList());
                promise.trySuccess(byteBufs);
              }
            }
          }
        } finally {
          ReferenceCountUtil.release(buf);
        }
      }
      case null -> throw new IllegalArgumentException("msg must not be null or empty.");
      default -> ctx.fireChannelRead(msg);
    }
  }

  @Override
  @SuppressWarnings("rawtypes")
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
    if (msg instanceof Tuple3 tuple3) {
      Object req = tuple3.t1();
      Promise cPromise = (Promise) tuple3.t2();
      String uuid = (String) tuple3.t3();
      String token = getToken();

      // 为请求添加鉴权token和uuid
      if (req instanceof Request request) {
        msg = cache(request.toBuilder().setToken(token).setUuid(uuid).build(), cPromise);
      } else if (req instanceof Request.Builder builder) {
        msg = cache(builder.setToken(token).setUuid(uuid).build(), cPromise);
      }

      log.info("Send Request: {}{}", System.lineSeparator(), msg);
    }
    ctx.write(msg, promise);
  }

  /**
   * 尝试取消请求
   *
   * @param reqUuid 请求uuid
   */
  @SuppressWarnings("rawtypes")
  public void tryCancelRequest(@NonNull String reqUuid) {
    Tuple3<Request, Promise, LinkedList<Tuple2<Integer, ByteBuf>>> tuple3 =
        cache.asMap().remove(reqUuid);
    if (tuple3 != null) {
      Request request = tuple3.t1();
      Promise promise = tuple3.t2();
      LinkedList<Tuple2<Integer, ByteBuf>> dataBlocks = tuple3.t3();
      log.info("Try to cancel the request: {}{}", System.lineSeparator(), request);
      try {
        promise.tryFailure(
            new CancellationException(String.format("Request canceled: %s", request)));
      } finally {
        // 取消请求，清理已缓存的结果
        release(dataBlocks);
      }
    } else {
      log.info("Unable to find request[{}] record based on uuid.", reqUuid);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    log.error(
        "The channel[id:{}] is closed between client:{} and server:{}. {}details:{}",
        ctx.channel().id(),
        ctx.channel().localAddress(),
        ctx.channel().remoteAddress(),
        System.lineSeparator(),
        socketUDT(ctx.channel()).toStringOptions(),
        cause);
    ctx.close();
  }

  @SuppressWarnings("rawtypes")
  private Request cache(Request request, Promise promise) {
    cache.put(
        request.getUuid(),
        Tuple3.<Request, Promise, LinkedList<Tuple2<Integer, ByteBuf>>>builder()
            .t1(request)
            .t2(promise)
            .t3(new LinkedList<>())
            .build());
    return request;
  }

  private void release(LinkedList<Tuple2<Integer, ByteBuf>> list) {
    if (list != null && !list.isEmpty()) {
      list.stream().map(Tuple2::t2).forEach(ReferenceCountUtil::release);
    }
  }

  private String getToken() {
    ClusterViewChangedEvent event = clusterViewChangedEventSupplier.get();
    return jwtAuthenticator.generate(
        Map.of(CLUSTER_KEY, event.cluster(), GENERATOR_KEY, event.localAddress().toString()));
  }
}
