/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.silong.foundation.devastator.utils;

import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.function.*;

/**
 * Lambda表达式可序列化定义
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-07-03 21:52
 */
public interface LambdaSerializable {
  /**
   * 可序列化Supplier
   *
   * @param <R> 结果类型
   */
  @FunctionalInterface
  interface SerializableSupplier<R> extends Supplier<R>, Serializable {}

  /** 可序列化Runnable */
  @FunctionalInterface
  interface SerializableRunnable extends Runnable, Serializable {}

  /**
   * 可序列化BinaryOperator
   *
   * @param <T> 参数类型
   */
  @FunctionalInterface
  interface SerializableBinaryOperator<T> extends BinaryOperator<T>, Serializable {}

  /**
   * 可序列化Callable
   *
   * @param <R> 结果类型
   */
  @FunctionalInterface
  interface SerializableCallable<R> extends Callable<R>, Serializable {}

  /**
   * 可序列化Consumer
   *
   * @param <T> 结果类型
   */
  @FunctionalInterface
  interface SerializableConsumer<T> extends Consumer<T>, Serializable {}

  /**
   * 可序列化Predicate
   *
   * @param <T> 结果类型
   */
  @FunctionalInterface
  interface SerializablePredicate<T> extends Predicate<T>, Serializable {}

  /**
   * 可序列化Function
   *
   * @param <T> 参数类型
   * @param <R> 结果类型
   */
  @FunctionalInterface
  interface SerializableFunction<T, R> extends Function<T, R>, Serializable {}

  /**
   * 可序列化BiFunction
   *
   * @param <T> 参数类型
   * @param <U> 参数类型
   * @param <R> 结果类型
   */
  @FunctionalInterface
  interface SerializableBiFunction<T, U, R> extends BiFunction<T, U, R>, Serializable {}

  /**
   * 可序列化BiConsumer
   *
   * @param <T> 参数类型
   * @param <U> 参数类型
   */
  @FunctionalInterface
  interface SerializableBiConsumer<T, U> extends BiConsumer<T, U>, Serializable {}

  /**
   * 可序列化BiPredicate
   *
   * @param <T> 参数类型
   * @param <U> 参数类型
   */
  @FunctionalInterface
  interface SerializableBiPredicate<T, U> extends BiPredicate<T, U>, Serializable {}
}
