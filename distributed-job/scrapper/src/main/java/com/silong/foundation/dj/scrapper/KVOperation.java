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

package com.silong.foundation.dj.scrapper;

import com.silong.foundation.rocksdbffm.RocksDbException;
import com.silong.foundation.rocksdbffm.config.RocksDbConfig;

/**
 * kv对象存储接口
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-01-09 20:04
 */
public interface KVOperation {

  /**
   * 获取实例
   *
   * @param config 配置
   * @return 实例
   */
  static KVOperation getInstance(RocksDbConfig config) {
    return new KVOpImpl(config);
  }

  /**
   * 根据K查询V
   *
   * @param key key
   * @param vClass value class
   * @return value
   * @param <K> key类型
   * @param <V> value类型
   * @throws RocksDbException 异常
   */
  <K extends Serializer<K>, V extends Serializer<V>> V get(K key, Class<V> vClass)
      throws RocksDbException;

  /**
   * 存储指定KV
   *
   * @param key key
   * @param val value
   * @param <K> key类型
   * @param <V> value类型
   * @throws RocksDbException 异常
   */
  <K extends Serializer<K>, V extends Serializer<V>> void put(K key, V val) throws RocksDbException;
}
