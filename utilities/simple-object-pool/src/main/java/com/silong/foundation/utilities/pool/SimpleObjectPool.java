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
public interface SimpleObjectPool<T extends ObjectPoolable<T>> {

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
}
