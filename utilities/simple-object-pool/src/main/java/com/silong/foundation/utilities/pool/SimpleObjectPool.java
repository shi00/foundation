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
package com.silong.foundation.utilities.pool;

/**
 * 简单对象池接口
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-06-28 21:08
 * @param <T> 缓存对象类型
 */
public interface SimpleObjectPool<T extends ObjectPoolable<T>> extends AutoCloseable {

  /**
   * 构建支持软引用的对象池，非线程安全
   *
   * @param maxCapacity 对象池最大容量
   * @param poolableObjectFactory 对象构建工厂
   * @return 对象池
   * @param <T> 缓存对象类型
   */
  static <T extends ObjectPoolable<T>> SimpleObjectPool<T> buildLinkedListSoftRefObjectPool(
      int maxCapacity, PoolableObjectFactory<T> poolableObjectFactory) {
    return new LinkedListSoftRefObjectPool<>(poolableObjectFactory, maxCapacity);
  }

  /**
   * 构建支持软引用的线程安全对象池
   *
   * @param maxCapacity 对象池最大容量
   * @param poolableObjectFactory 对象构建工厂
   * @return 对象池
   * @param <T> 缓存对象类型
   */
  static <T extends ObjectPoolable<T>> SimpleObjectPool<T> buildSoftRefObjectPool(
      int maxCapacity, PoolableObjectFactory<T> poolableObjectFactory) {
    return new DefaultSoftRefObjectPool<>(poolableObjectFactory, maxCapacity);
  }

  /**
   * 构建线程安全的对象池
   *
   * @param maxCapacity 对象池最大容量
   * @param poolableObjectFactory 对象构建工厂
   * @return 对象池
   * @param <T> 缓存对象类型
   */
  static <T extends ObjectPoolable<T>> SimpleObjectPool<T> buildSimpleObjectPool(
      int maxCapacity, PoolableObjectFactory<T> poolableObjectFactory) {
    return new DefaultSimpleObjectPool<>(poolableObjectFactory, maxCapacity);
  }

  /**
   * 对象池容量
   *
   * @return 对象池容量
   */
  int capcity();

  /**
   * 对象池内的可用对象数
   *
   * @return 可用缓存对象数量
   */
  int freeObjects();

  /**
   * 从缓存池获取对象
   *
   * @return 对象
   */
  T obtain();

  /**
   * 归还对象入池
   *
   * @param obj 对象
   */
  void returns(T obj);

  /** 释放对象池资源 */
  default void close() {}
}
