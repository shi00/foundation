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

package com.silong.foundation.dj.longhaul;

/**
 * 对象访问接口
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-26 0:57
 */
public interface ObjectAccessor {

  /**
   * 在default ColumnFamily删除key对应的value
   *
   * @param key key
   * @exception Exception 异常
   */
  <K extends TypeConverter<K>> void remove(K key) throws Exception;

  /**
   * 根据key查询value，默认列族：default
   *
   * @param key key
   * @param vClass 值类型
   * @return value
   * @param <K> key类型
   * @param <V> value类型
   * @exception Exception 异常
   */
  <K extends TypeConverter<K>, V extends TypeConverter<V>> V get(K key, Class<V> vClass)
      throws Exception;

  /**
   * 保存KV，使用默认列族：default
   *
   * @param key k
   * @param value v
   * @param <K> key type
   * @param <V> value type
   * @throws Exception 异常
   */
  <K extends TypeConverter<K>, V extends TypeConverter<V>> void put(K key, V value)
      throws Exception;
}
