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

import java.util.concurrent.LinkedBlockingQueue;

/**
 * 线程安全的对象池，基于{@link LinkedBlockingQueue}实现，高并发下性能更佳
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-06-28 21:19
 */
class DefaultSimpleObjectPool<T extends ObjectPoolable<T>> extends AbstractSimpleObjectPool<T> {

  /** 容量 */
  private final int capacity;

  /**
   * 构造方法
   *
   * @param poolableObjectFactory 对象工厂
   * @param maxCapcity 对象池最大容量
   */
  public DefaultSimpleObjectPool(PoolableObjectFactory<T> poolableObjectFactory, int maxCapcity) {
    super(new LinkedBlockingQueue<>(maxCapcity), poolableObjectFactory);
    this.capacity = maxCapcity;
  }

  @Override
  public int capcity() {
    return capacity;
  }
}
