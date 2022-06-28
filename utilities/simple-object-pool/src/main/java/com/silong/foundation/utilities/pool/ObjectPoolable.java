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
 * 可缓存对象接口
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-06-28 21:06
 * @param <T> 对象类型
 */
@FunctionalInterface
public interface ObjectPoolable<T> {
  /**
   * 重置对象，准备入池后复用
   *
   * @return 重置后对象
   */
  T reset();
}
