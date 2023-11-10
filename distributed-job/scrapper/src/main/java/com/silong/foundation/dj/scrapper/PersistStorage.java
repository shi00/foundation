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

import com.silong.foundation.common.lambda.Tuple2;
import com.silong.foundation.dj.scrapper.config.PersistStorageProperties;
import com.silong.foundation.dj.scrapper.vo.KvPair;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import lombok.NonNull;

/**
 * 持久化存储接口
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-10 17:14
 */
public interface PersistStorage {

  /** 默认列族 */
  String DEFAULT_COLUMN_FAMILY_NAME = "default";

  /**
   * 根据配置创建持久化存储
   *
   * @param properties 配置
   * @return 持久化存储
   */
  static PersistStorage getInstance(@NonNull PersistStorageProperties properties) {
    return new RocksDbPersistStorage(properties);
  }

  /**
   * 创建列族，如果已经存在则直接返回
   *
   * @param columnFamilyName 列族名
   */
  void createColumnFamily(String columnFamilyName);

  /**
   * 删除列族以及其数据，如果列族不存在则直接返回
   *
   * @param columnFamilyName 列族名
   */
  void deleteColumnFamily(String columnFamilyName);

  /**
   * 在default ColumnFamily删除key
   *
   * @param key key
   */
  void remove(byte[] key);

  /**
   * 在指定列族中删除key
   *
   * @param columnFamilyName 列族名
   * @param key key
   */
  void remove(String columnFamilyName, byte[] key);

  /**
   * 在default ColumnFamily批量删除key
   *
   * @param keys key列表
   */
  void multiRemove(List<byte[]> keys);

  /**
   * 在指定列族批量删除key
   *
   * @param columnFamilyName 列族名
   * @param keys key列表
   */
  void multiRemove(String columnFamilyName, List<byte[]> keys);

  /**
   * 删除指定列族中的key对应的值
   *
   * @param keys 列族和key的Tuple列表
   */
  void multiRemoveAll(List<Tuple2<String, byte[]>> keys);

  /**
   * 在default ColumnFamily根据key查询value
   *
   * @param key key
   * @return value`
   */
  byte[] get(byte[] key);

  /**
   * 在指定列族根据key查询value
   *
   * @param columnFamilyName 列族名
   * @param key key
   * @return value`
   */
  byte[] get(String columnFamilyName, byte[] key);

  /**
   * 在default ColumnFamily查询多Key
   *
   * @param keys key列表
   * @return kvPairs
   */
  List<KvPair<byte[], byte[]>> multiGet(List<byte[]> keys);

  /**
   * 在指定列族查询多Key
   *
   * @param columnFamilyName 列族名
   * @param keys key列表
   * @return kvPairs
   */
  List<KvPair<byte[], byte[]>> multiGet(String columnFamilyName, List<byte[]> keys);

  /**
   * 从不同列族查询结果
   *
   * @param keys 列族与key列表
   * @return 查询结果
   */
  List<KvPair<Tuple2<String, byte[]>, byte[]>> multiGetAll(List<Tuple2<String, byte[]>> keys);

  /**
   * 在default ColumnFamily保存kv
   *
   * @param key key
   * @param value value
   */
  void put(byte[] key, byte[] value);

  /**
   * 在指定列族保存kv
   *
   * @param columnFamilyName 列族名
   * @param key key
   * @param value value
   */
  void put(String columnFamilyName, byte[] key, byte[] value);

  /**
   * 在default ColumnFamily批量保存kv
   *
   * @param kvPairs kvpair列表
   */
  void putAll(List<KvPair<byte[], byte[]>> kvPairs);

  /**
   * 在指定列族批量保存kv
   *
   * @param columnFamilyName 列族名
   * @param kvPairs kvpair列表
   */
  void putAll(String columnFamilyName, List<KvPair<byte[], byte[]>> kvPairs);

  /**
   * key,value保存到指定列族
   *
   * @param columnFamilyNameWithKvPairs 列族与kvpair列表
   */
  void putAllWith(List<Tuple2<String, KvPair<byte[], byte[]>>> columnFamilyNameWithKvPairs);

  /**
   * 删除default列族中的起始key和结束key之间的所有值
   *
   * @param startKey 起始key，includes
   * @param endKey 结束Key，excludes
   */
  void deleteRange(byte[] startKey, byte[] endKey);

  /**
   * 删除指定列族中的起始key和结束key之间的所有值
   *
   * @param columnFamilyName 列族名
   * @param startKey 起始key，includes
   * @param endKey 结束Key，excludes
   */
  void deleteRange(String columnFamilyName, byte[] startKey, byte[] endKey);

  /**
   * 获取当前rocksdb中所有存在的列族名称列表
   *
   * @return 列族名称列表
   */
  Collection<String> getAllColumnFamilyNames();

  /**
   * 遍历default列族内保存的所有kv pair
   *
   * @param consumer 消费者
   */
  void iterate(BiConsumer<byte[], byte[]> consumer);

  /**
   * 遍历列族内保存的所有kv pair
   *
   * @param columnFamilyName 列族名
   * @param consumer 消费者
   */
  void iterate(String columnFamilyName, BiConsumer<byte[], byte[]> consumer);
}
