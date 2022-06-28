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

import java.util.Queue;

/**
 * 简单对象池基类
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-06-28 21:18
 * @param <T> 缓存对象类型
 */
public abstract class AbstractSimpleObjectPool<T extends ObjectPoolable<T>>
    implements SimpleObjectPool<T> {

  /** 对象队列 */
  final Queue<T> queue;

  final PoolableObjectFactory<T> poolableObjectFactory;

  /**
   * 构造方法
   *
   * @param queue 对象队列
   * @param poolableObjectFactory 对象工厂
   */
  public AbstractSimpleObjectPool(Queue<T> queue, PoolableObjectFactory<T> poolableObjectFactory) {
    if (queue == null) {
      throw new IllegalArgumentException("queue must not be null.");
    }
    if (poolableObjectFactory == null) {
      throw new IllegalArgumentException("poolableObjectFactory must not be null.");
    }
    this.queue = queue;
    this.poolableObjectFactory = poolableObjectFactory;
  }

  @Override
  public int freeObjects() {
    return queue.size();
  }

  @Override
  public void returns(T obj) {
    if (obj != null) {
      queue.offer(obj.reset());
    }
  }

  @Override
  public T obtain() {
    T obj = queue.poll();
    return obj == null ? poolableObjectFactory.create() : obj;
  }

  public void close() {
    queue.clear();
  }
}
