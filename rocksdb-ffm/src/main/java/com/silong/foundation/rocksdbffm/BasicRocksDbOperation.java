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

package com.silong.foundation.rocksdbffm;

import com.silong.foundation.common.lambda.Tuple2;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * Rocksdb 基础操作
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-12-02 11:14
 */
public interface BasicRocksDbOperation {

  /**
   * 返回打开的列族列表
   *
   * @return 列族列表
   */
  Collection<String> openedColumnFamilies();

  /**
   * 创建列族
   *
   * @param cf 列族名称列表
   * @return 创建成功返回true，否则false
   */
  boolean createColumnFamily(String cf);

  /**
   * 删除列族
   *
   * @param cf 列族名
   */
  void dropColumnFamily(String cf);

  /**
   * 在default ColumnFamily删除key
   *
   * @param key key
   */
  void delete(byte[] key);

  /**
   * 在指定列族中删除key
   *
   * @param columnFamilyName 列族名
   * @param key key
   */
  void delete(String columnFamilyName, byte[] key);

  /**
   * 在指定列族中删除key
   *
   * @param columnFamilyName 列族名
   * @param key key
   * @param offset offset
   * @param length length
   */
  void delete(String columnFamilyName, byte[] key, int offset, int length);

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
   * 向指定列族保存数据
   *
   * @param columnFamilyName 列族名
   * @param key key
   * @param value value
   */
  void put(String columnFamilyName, byte[] key, byte[] value);

  /**
   * 批量写入
   *
   * @param columnFamilyName 列族
   * @param kvPairs kv键值对列表
   */
  void putAll(String columnFamilyName, Tuple2<byte[], byte[]>... kvPairs) throws RocksDbException;

  /**
   * 向默认列族保存数据
   *
   * @param key key
   * @param value value
   */
  void put(byte[] key, byte[] value);

  /**
   * 查询指定列族的键值，如果出现错误或者key不存在，返回null
   *
   * @param columnFamilyName 列族名
   * @param key key
   * @return value
   */
  byte[] get(String columnFamilyName, byte[] key);

  /**
   * 查询默认列族保存数据
   *
   * @param key key
   * @return value
   */
  byte[] get(byte[] key);

  /**
   * 在default ColumnFamily查询多Key
   *
   * @param keys key列表
   * @return kvPairs
   */
  List<Tuple2<byte[], byte[]>> multiGet(byte[]... keys);

  /**
   * 在指定列族查询多Key
   *
   * @param columnFamilyName 列族名
   * @param keys key列表
   * @return kvPairs
   */
  List<Tuple2<byte[], byte[]>> multiGet(String columnFamilyName, byte[]... keys);

  /**
   * 获取default列族迭代器
   *
   * @return 迭代器
   */
  RocksDbIterator iterator();

  /**
   * 获取指定列族迭代器
   *
   * @param columnFamilyName 列族名称
   * @return 迭代器
   */
  RocksDbIterator iterator(String columnFamilyName);

  /**
   * 原子批量更新操作
   *
   * @param action 操作
   * @return 结果
   * @param <R> 结果类型
   * @throws RocksDbException 更新异常
   */
  <R> R atomicBatchUpdate(Function<WriteBatch, R> action) throws RocksDbException;
}
