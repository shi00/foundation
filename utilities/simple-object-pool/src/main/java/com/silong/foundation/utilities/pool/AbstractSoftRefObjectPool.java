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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.Queue;

/**
 * 软引用对象池基类
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-06-28 21:18
 * @param <T> 缓存对象类型
 */
public abstract class AbstractSoftRefObjectPool<T extends ObjectPoolable<T>>
    implements SimpleObjectPool<T> {

  /** 对象队列 */
  final Queue<SoftReference<T>> queue;

  final PoolableObjectFactory<T> poolableObjectFactory;

  /** 引用队列 */
  final ReferenceQueue<T> referenceQueue;

  /**
   * 构造方法
   *
   * @param queue 软引用对象队列
   * @param poolableObjectFactory 对象工厂
   */
  public AbstractSoftRefObjectPool(
      Queue<SoftReference<T>> queue, PoolableObjectFactory<T> poolableObjectFactory) {
    if (queue == null) {
      throw new IllegalArgumentException("queue must not be null.");
    }
    if (poolableObjectFactory == null) {
      throw new IllegalArgumentException("poolableObjectFactory must not be null.");
    }
    this.queue = queue;
    this.poolableObjectFactory = poolableObjectFactory;
    this.referenceQueue = new ReferenceQueue<>();
  }

  private boolean valid(SoftReference<?> softReference) {
    return softReference.get() != null;
  }

  @Override
  public int freeObjects() {
    return (int) queue.stream().filter(this::valid).count();
  }

  @Override
  public void returns(T obj) {
    if (obj != null) {
      SoftReference<T> ref = new SoftReference<>(obj.reset(), referenceQueue);
      // 先插入一次，如果失败则检查引用队列是否有被释放掉的对象，如果有则从缓存队列中移除后重试一次
      Reference<?> reference;
      if (!queue.offer(ref)
          && (reference = referenceQueue.poll()) != null
          && queue.remove(reference)) {
        queue.offer(ref);
      }
    }
  }

  @Override
  public T obtain() {
    T obj;
    SoftReference<T> ref = queue.poll();
    return ref == null
        ? poolableObjectFactory.create()
        : (obj = ref.get()) == null ? poolableObjectFactory.create() : obj;
  }

  @Override
  public void close() {
    queue.clear();
  }
}
