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
import com.silong.foundation.common.lambda.Consumer3;
import com.silong.foundation.common.lambda.Tuple2;
import com.silong.foundation.common.lambda.Tuple3;
import com.silong.foundation.common.lambda.Tuple4;
import com.silong.foundation.dj.bonecrusher.configure.config.BonecrusherClientProperties;
import com.silong.foundation.dj.bonecrusher.event.ClusterViewChangedEvent;
import com.silong.foundation.dj.bonecrusher.exception.ConcurrentRequestLimitExceededException;
import com.silong.foundation.dj.bonecrusher.exception.RequestResponseException;
import com.silong.foundation.dj.bonecrusher.message.Messages.DataBlockMetadata;
import com.silong.foundation.dj.bonecrusher.message.Messages.Request;
import com.silong.foundation.dj.bonecrusher.message.Messages.ResponseHeader;
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
@Slf4j
@Sharable
public class ClientChannelHandler extends ChannelDuplexHandler {

  /** 集群视图 */
  @Setter
  @Accessors(fluent = true)
  private Supplier<ClusterViewChangedEvent> clusterViewChangedEventSupplier;

  /** 请求缓存信息 */
  @SuppressWarnings("rawtypes")
  private final Cache<
          String,
          Tuple4<
              Request,
              Promise,
              LinkedList<Tuple2<Integer, ByteBuf>>,
              Consumer3<ByteBuf, Integer, Integer>>>
      cache;

  /** 鉴权工具 */
  private final JwtAuthenticator jwtAuthenticator;

  /** 数据块序号排序 */
  private final Comparator<Tuple2<Integer, ByteBuf>> dataBlockNoComparing =
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
    this.jwtAuthenticator = jwtAuthenticator;
    this.clientProperties = clientProperties;
    this.executor = executor;
    this.cache =
        Caffeine.newBuilder()
            .initialCapacity(clientProperties.getExpectedConcurrentRequests())
            .maximumSize(clientProperties.getMaximumConcurrentRequests())
            .expireAfterAccess(clientProperties.getRequestTimeout())
            .executor(executor)
            .scheduler(Scheduler.systemScheduler())
            .evictionListener(buildRemovalListener(clientProperties))
            .build();
  }

  @SuppressWarnings("rawtypes")
  private RemovalListener<
          String,
          Tuple4<
              Request,
              Promise,
              LinkedList<Tuple2<Integer, ByteBuf>>,
              Consumer3<ByteBuf, Integer, Integer>>>
      buildRemovalListener(BonecrusherClientProperties clientProperties) {
    return (uuid, tuple4, cause) -> {
      // 只有垃圾收集导致的淘汰tuples才会为null，因此过滤此种场景
      if (tuple4 != null) {
        Request request = tuple4.t1();
        Promise promise = tuple4.t2();
        switch (cause) {
          case EXPIRED -> // 请求超时
          notifyFailure(
              promise,
              new TimeoutException(
                  String.format(
                      "Timeout Threshold: %ss, Request: %s",
                      clientProperties.getRequestTimeout().toSeconds(), request)),
              tuple4);
          case SIZE -> // 超出并发请求数上限淘汰
          notifyFailure(
              promise,
              new ConcurrentRequestLimitExceededException(
                  String.format(
                      "The number of concurrent requests exceeded the limit of %d. Discard Request: %s",
                      clientProperties.getMaximumConcurrentRequests(), request)),
              tuple4);
        }
      }
    };
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    switch (msg) {
      case Tuple2 t -> {
        // 读取响应数据块
        Tuple2<ResponseHeader, ByteBuf> tuple2 = (Tuple2<ResponseHeader, ByteBuf>) t;
        ResponseHeader header = tuple2.t1();
        ByteBuf buf = tuple2.t2();
        try {
          // 处理失败响应
          if (header.hasResult()) {
            Tuple4<
                    Request,
                    Promise,
                    LinkedList<Tuple2<Integer, ByteBuf>>,
                    Consumer3<ByteBuf, Integer, Integer>>
                tuple4 = cache.asMap().remove(header.getUuid());
            if (tuple4 != null) {
              notifyFailure(
                  tuple4.t2(),
                  new RequestResponseException(
                      "Abnormal response: " + header.getResult().getDesc()),
                  tuple4);
            }
          } else {
            // 读取请求记录，并做相应处理
            Tuple4<
                    Request,
                    Promise,
                    LinkedList<Tuple2<Integer, ByteBuf>>,
                    Consumer3<ByteBuf, Integer, Integer>>
                tuple4 = cache.getIfPresent(header.getUuid());
            if (tuple4 != null) {
              DataBlockMetadata metadata = header.getDataBlockMetadata();
              Promise promise = tuple4.t2();
              Consumer3<ByteBuf, Integer, Integer> byteBufConsumer = tuple4.t4();
              if (byteBufConsumer == null) {
                int blockNo = metadata.getBlockNo();
                LinkedList<Tuple2<Integer, ByteBuf>> bufList = tuple4.t3();
                bufList.add(
                    Tuple2.<Integer, ByteBuf>Tuple2Builder()
                        .t1(blockNo)
                        .t2(buf.retain()) // 引用计数+1，避免被netty内存管理回收
                        .build());

                // 所有数据块都收到后合并结果返回
                if (metadata.getTotalBlocks() == bufList.size()) {
                  CompositeByteBuf byteBuf =
                      ctx.alloc()
                          .compositeBuffer(bufList.size())
                          .addComponents(
                              true,
                              bufList.stream()
                                  .sorted(dataBlockNoComparing)
                                  .map(Tuple2::t2)
                                  .toList());
                  promise.trySuccess(byteBuf);
                }
              } else {
                byteBufConsumer.accept(buf, metadata.getTotalBlocks(), metadata.getBlockNo());
                if (metadata.getTotalBlocks() - 1 == metadata.getBlockNo()) {}
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
    switch (msg) {
      case Tuple4 tuple4 -> msg = unpackMsg(msg, tuple4);
      case Tuple3 tuple3 -> msg = unpackMsg(msg, tuple3);
      case null -> throw new IllegalArgumentException("msg must not be null or empty.");
      default -> {}
    }
    ctx.write(msg, promise);
  }

  /**
   * 消息拆包
   *
   * @param msg 请求消息
   * @param tuple3 消息包
   * @return 拆包后的消息
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  private Object unpackMsg(Object msg, Tuple3 tuple3) {
    Object req = tuple3.t1();
    Promise cPromise = (Promise) tuple3.t2();
    String uuid = (String) tuple3.t3();
    Consumer3<ByteBuf, Integer, Integer> consumer =
        tuple3 instanceof Tuple4 tuple4 ? (Consumer3<ByteBuf, Integer, Integer>) tuple4.t4() : null;
    String token = generateToken();

    // 为请求添加鉴权token和uuid
    if (req instanceof Request request) {
      msg = cache(request.toBuilder().setToken(token).setUuid(uuid).build(), cPromise, consumer);
      log.info("Send Request: {}{}", System.lineSeparator(), msg);
    } else if (req instanceof Request.Builder builder) {
      msg = cache(builder.setToken(token).setUuid(uuid).build(), cPromise, consumer);
      log.info("Send Request: {}{}", System.lineSeparator(), msg);
    }
    return msg;
  }

  /**
   * 缓存请求信息
   *
   * @param request 请求
   * @param promise promise
   * @param consumer consumer
   * @return 请求
   */
  @SuppressWarnings("rawtypes")
  private Request cache(
      Request request, Promise promise, Consumer3<ByteBuf, Integer, Integer> consumer) {
    Tuple4.Tuple4Builder<
            Request,
            Promise,
            LinkedList<Tuple2<Integer, ByteBuf>>,
            Consumer3<ByteBuf, Integer, Integer>>
        builder =
            Tuple4
                .<Request, Promise, LinkedList<Tuple2<Integer, ByteBuf>>,
                    Consumer3<ByteBuf, Integer, Integer>>
                    Tuple4Builder()
                .t1(request)
                .t2(promise);

    if (consumer == null) {
      builder.t3(new LinkedList<>());
    } else {
      builder.t4(consumer);
    }

    cache.put(request.getUuid(), builder.build());
    return request;
  }

  /**
   * 释放缓存的结果
   *
   * @param tuple4 缓存记录
   */
  @SuppressWarnings("rawtypes")
  private void release(
      Tuple4<
              Request,
              Promise,
              LinkedList<Tuple2<Integer, ByteBuf>>,
              Consumer3<ByteBuf, Integer, Integer>>
          tuple4) {
    LinkedList<Tuple2<Integer, ByteBuf>> list = tuple4.t3();
    if (list != null && !list.isEmpty()) {
      for (Tuple2<Integer, ByteBuf> tuple2 : list) {
        ReferenceCountUtil.release(tuple2.t2());
        tuple2.t1(null).t2(null);
      }
      list.clear();
    }
    tuple4.t4(null).t3(null).t2(null).t1(null);
  }

  /**
   * 尝试取消请求
   *
   * @param reqUuid 请求uuid
   */
  @SuppressWarnings("rawtypes")
  public void tryCancelRequest(@NonNull String reqUuid) {
    Tuple4<
            Request,
            Promise,
            LinkedList<Tuple2<Integer, ByteBuf>>,
            Consumer3<ByteBuf, Integer, Integer>>
        tuple4 = cache.asMap().remove(reqUuid);
    if (tuple4 != null) {
      Request request = tuple4.t1();
      Promise promise = tuple4.t2();
      log.info("Try to cancel the request: {}{}", System.lineSeparator(), request);
      try {
        promise.tryFailure(
            new CancellationException(String.format("Request canceled: %s", request)));
      } finally {
        // 取消请求，清理已缓存的结果
        release(tuple4);
      }
    } else {
      log.info("Unable to find request[{}] record based on uuid.", reqUuid);
    }
  }

  @SuppressWarnings("rawtypes")
  private void notifyFailure(
      Promise promise,
      Exception e,
      Tuple4<
              Request,
              Promise,
              LinkedList<Tuple2<Integer, ByteBuf>>,
              Consumer3<ByteBuf, Integer, Integer>>
          tuple4) {
    try {
      promise.tryFailure(e);
    } finally {
      release(tuple4);
    }
  }

  /**
   * 生成Token
   *
   * @return token
   */
  private String generateToken() {
    ClusterViewChangedEvent event = clusterViewChangedEventSupplier.get();
    return jwtAuthenticator.generate(
        Map.of(CLUSTER_KEY, event.cluster(), GENERATOR_KEY, event.localAddress().toString()));
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
}
