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

package com.silong.foundation.dj.bonecrusher.utils;

import static java.util.Collections.unmodifiableList;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.silong.foundation.dj.bonecrusher.exception.PartialExecutionFailureException;
import io.netty.util.concurrent.Future;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Supplier;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * ChannelFuture组装器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-31 17:47
 * @param <R> 返回结果
 * @param <T> Future类型
 */
@Slf4j
public class FutureCombiner<R, T extends Future<R>> {

  /** netty future list */
  private final List<T> futures;

  /** 执行失败的future列表 */
  private final AtomicReferenceArray<T> failedFutures;

  private final AtomicInteger index = new AtomicInteger(0);

  /**
   * 构造方法
   *
   * @param futures futures
   */
  public FutureCombiner(T... futures) {
    if (futures == null || futures.length == 0) {
      throw new IllegalArgumentException("futures must not be null or empty.");
    }
    this.futures = List.of(futures);
    this.failedFutures = new AtomicReferenceArray<>(futures.length);
  }

  /**
   * 获取执行失败的future
   *
   * @return 执行失败future列表
   */
  public List<T> getFailedFutures() {
    if (index.get() <= 0) {
      return List.of();
    }
    ArrayList<T> result = new ArrayList<>(index.get());
    for (int i = index.get() - 1; i >= 0; i--) {
      result.add(failedFutures.get(i));
    }
    return unmodifiableList(result);
  }

  /**
   * 所有ChannelFuture都执行成功后执行回调
   *
   * @param callback 回调
   * @param executor 回调执行器
   */
  public CompletableFuture<Optional<R>> whenAllSucceed(
      @NonNull Callable<R> callback, @NonNull Executor executor) {
    return buildCompletableFuture(
        this::getLatchWhenSucceed, () -> wrap2Optional(callback), optionalEmpty(), executor);
  }

  /**
   * 所有ChannelFuture都执行成功后执行回调
   *
   * @param callback 回调
   */
  public CompletableFuture<Optional<R>> whenAllSucceed(@NonNull Callable<R> callback) {
    return buildCompletableFuture(
        this::getLatchWhenSucceed, () -> wrap2Optional(callback), optionalEmpty(), Runnable::run);
  }

  /**
   * 当所有future都执行完毕后执行回调
   *
   * @param runnable 回调
   * @param executor 回调执行器
   * @return CompletableFuture
   */
  public CompletableFuture<Void> whenAllSucceed(
      @NonNull Runnable runnable, @NonNull Executor executor) {
    return buildCompletableFuture(
        this::getLatchWhenSucceed, () -> wrap2Void(runnable), nullValue(), executor);
  }

  /**
   * 当所有future都执行完毕后执行回调
   *
   * @param runnable 回调
   * @return CompletableFuture
   */
  public CompletableFuture<Void> whenAllSucceed(@NonNull Runnable runnable) {
    return buildCompletableFuture(
        this::getLatchWhenSucceed, () -> wrap2Void(runnable), nullValue(), Runnable::run);
  }

  /**
   * 当所有future都执行完毕后，执行回调
   *
   * @param runnable 回调方法
   * @param executor 回调执行器
   */
  public CompletableFuture<Void> whenAllComplete(
      @NonNull Runnable runnable, @NonNull Executor executor) {
    return buildCompletableFuture(
        this::getLatchWhenComplete, () -> wrap2Void(runnable), nullValue(), executor);
  }

  /**
   * 当所有future都执行完毕后，执行回调
   *
   * @param runnable 回调方法
   */
  public CompletableFuture<Void> whenAllComplete(@NonNull Runnable runnable) {
    return buildCompletableFuture(
        this::getLatchWhenComplete, () -> wrap2Void(runnable), nullValue(), Runnable::run);
  }

  /**
   * 当所有future都执行完毕后，执行回调
   *
   * @param callback 回调方法
   */
  public CompletableFuture<Optional<R>> whenAllComplete(@NonNull Callable<R> callback) {
    return whenAllComplete(callback, Runnable::run);
  }

  /**
   * 当所有future都执行完毕后，执行回调
   *
   * @param callback 回调方法
   * @param executor 回调执行器
   */
  public CompletableFuture<Optional<R>> whenAllComplete(
      @NonNull Callable<R> callback, @NonNull Executor executor) {
    return buildCompletableFuture(
        this::getLatchWhenComplete, () -> wrap2Optional(callback), optionalEmpty(), executor);
  }

  private static <R> Supplier<Optional<R>> optionalEmpty() {
    return Optional::empty;
  }

  private Optional<R> wrap2Optional(Callable<R> callback) throws Exception {
    R r = callback.call();
    return r == null ? Optional.empty() : Optional.of(r);
  }

  private static Void wrap2Void(Runnable runnable) {
    runnable.run();
    return null;
  }

  private static Supplier<Void> nullValue() {
    return () -> null;
  }

  private CountDownLatch getLatchWhenSucceed() {
    CountDownLatch latch = new CountDownLatch(futures.size());
    futures.forEach(
        f ->
            f.addListener(
                future -> {
                  // 记录失败future
                  if (!future.isSuccess()) {
                    while (!failedFutures.compareAndSet(index.getAndIncrement(), null, f)) {
                      Thread.onSpinWait();
                    }
                  }
                  latch.countDown();
                }));
    return latch;
  }

  private CountDownLatch getLatchWhenComplete() {
    CountDownLatch latch = new CountDownLatch(futures.size());
    futures.forEach(f -> f.addListener(future -> latch.countDown()));
    return latch;
  }

  private <Q> CompletableFuture<Q> buildCompletableFuture(
      Supplier<CountDownLatch> supplier,
      Callable<Q> callback,
      Supplier<Q> exceptionValueSupply,
      Executor executor) {
    return supplyAsync(supplier, executor)
        .thenApplyAsync(
            latch -> {
              try {
                latch.await(); // 等待所有future执行完毕

                // 如果存在执行失败的future
                if (index.get() != 0) {
                  throw new PartialExecutionFailureException(
                      String.format(
                          "Total Tasks: %d, Failed Tasks: %d", futures.size(), index.get()));
                }

                return callback.call();
              } catch (Exception e) {
                throw new CompletionException(e);
              }
            },
            executor)
        .handleAsync(
            (q, t) -> {
              // 发生异常返回空结果
              if (t != null) {
                log.error("Failed to execute callback: {}.", callback, t);
                return exceptionValueSupply.get();
              }
              return q;
            },
            executor);
  }
}
