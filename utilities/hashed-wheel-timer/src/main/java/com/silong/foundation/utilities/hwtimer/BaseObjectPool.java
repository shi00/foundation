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
package com.silong.foundation.utilities.hwtimer;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 对象池基类，类似Kryo自带的Pool
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-06-27 22:52
 * @param <T> 缓存对象类型
 */
abstract class BaseObjectPool<T> {

  private final Queue<T> freeObjects;

  private int peak;

  /**
   * 构造方法
   *
   * @param threadSafe 是否线程安全
   * @param maximumCapacity 缓存对象最大容量
   */
  public BaseObjectPool(boolean threadSafe, final int maximumCapacity) {
    if (threadSafe) {
      freeObjects =
          new LinkedBlockingQueue<>(maximumCapacity) {
            @Override
            public boolean add(T o) {
              return super.offer(o);
            }
          };
    } else {
      freeObjects =
          new ArrayDeque<>() {
            public boolean offer(T o) {
              if (size() >= maximumCapacity) {
                return false;
              }
              super.offer(o);
              return true;
            }
          };
    }
  }

  /**
   * create object
   *
   * @return object
   */
  protected abstract T create();

  /**
   * Returns an object from this pool. The object may be new (from {@link #create()}) or reused
   * (previously {@link #free(Object) freed}).
   */
  public T obtain() {
    T object = freeObjects.poll();
    return object != null ? object : create();
  }

  /**
   * Puts the specified object in the pool, making it eligible to be returned by {@link #obtain()}.
   * If the pool already contains the maximum number of free objects, the specified object is reset
   * but not added to the pool.
   */
  public void free(T object) {
    if (object == null) {
      throw new IllegalArgumentException("object cannot be null.");
    }
    reset(object);
    freeObjects.offer(object);
    peak = Math.max(peak, freeObjects.size());
  }

  /**
   * Called when an object is freed to clear the state of the object for possible later reuse. The
   * default implementation calls {@link Poolable#reset()} if the object is {@link Poolable}.
   */
  protected void reset(T object) {
    if (object instanceof Poolable) {
      ((Poolable) object).reset();
    }
  }

  /** Removes all free objects from this pool. */
  public void clear() {
    freeObjects.clear();
  }

  /** The number of objects available to be obtained. */
  public int getFree() {
    return freeObjects.size();
  }

  /**
   * The all-time highest number of free objects. This can help determine if a pool's maximum
   * capacity is set appropriately. It can be reset any time with {@link #resetPeak()}.
   *
   * <p>If using soft references, this number may include objects that have been garbage collected.
   */
  public int getPeak() {
    return peak;
  }

  public void resetPeak() {
    peak = 0;
  }

  /** 缓存对象实现 */
  public interface Poolable {
    /**
     * Resets the object for reuse. Object references should be nulled and fields may be set to
     * default values.
     */
    void reset();
  }
}
