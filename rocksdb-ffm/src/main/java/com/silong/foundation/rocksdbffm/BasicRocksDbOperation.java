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
   * @param columnFamilyName 列族名称列表
   * @throws RocksDbException 异常
   */
  void createColumnFamily(String columnFamilyName) throws RocksDbException;

  /**
   * 删除列族
   *
   * @param cf 列族名
   * @throws RocksDbException 异常
   */
  void dropColumnFamily(String cf) throws RocksDbException;

  /**
   * 在default ColumnFamily删除key
   *
   * @param key key
   * @throws RocksDbException 异常
   */
  void delete(byte[] key) throws RocksDbException;

  /**
   * 在指定列族中删除key
   *
   * @param columnFamilyName 列族名
   * @param key key
   * @throws RocksDbException 异常
   */
  void delete(String columnFamilyName, byte[] key) throws RocksDbException;

  /**
   * 在指定列族中删除key
   *
   * @param columnFamilyName 列族名
   * @param key key
   * @param offset offset
   * @param length length
   * @throws RocksDbException 异常
   */
  void delete(String columnFamilyName, byte[] key, int offset, int length) throws RocksDbException;

  /**
   * 删除default列族中的起始key和结束key之间的所有值
   *
   * @param startKey 起始key，includes
   * @param endKey 结束Key，excludes
   * @throws RocksDbException 异常
   */
  void deleteRange(byte[] startKey, byte[] endKey) throws RocksDbException;

  /**
   * 删除指定列族中的起始key和结束key之间的所有值
   *
   * @param columnFamilyName 列族名
   * @param startKey 起始key，includes
   * @param endKey 结束Key，excludes
   * @throws RocksDbException 异常
   */
  void deleteRange(String columnFamilyName, byte[] startKey, byte[] endKey) throws RocksDbException;

  /**
   * 删除指定列族中的起始key和结束key之间的所有值
   *
   * @param columnFamilyName 列族名
   * @param startKey 起始key，includes
   * @param startKeyOffset offset
   * @param startKeyLength length
   * @param endKey 结束Key，excludes
   * @param endKeyOffset offset
   * @param endKeyLength length
   * @throws RocksDbException 异常
   */
  void deleteRange(
      String columnFamilyName,
      byte[] startKey,
      int startKeyOffset,
      int startKeyLength,
      byte[] endKey,
      int endKeyOffset,
      int endKeyLength)
      throws RocksDbException;

  /**
   * 批量写入
   *
   * @param columnFamilyName 列族
   * @param kvPairs kv键值对列表
   * @throws RocksDbException 异常
   */
  void putAll(String columnFamilyName, Tuple2<byte[], byte[]>... kvPairs) throws RocksDbException;

  /**
   * 向指定列族保存数据
   *
   * @param columnFamilyName 列族名
   * @param key key
   * @param keyOffset offset
   * @param keyLength length
   * @param value value
   * @param valueOffset offset
   * @param valueLength length
   * @throws RocksDbException 异常
   */
  void put(
      String columnFamilyName,
      byte[] key,
      int keyOffset,
      int keyLength,
      byte[] value,
      int valueOffset,
      int valueLength)
      throws RocksDbException;

  /**
   * 向指定列族保存数据
   *
   * @param columnFamilyName 列族名
   * @param key key
   * @param value value
   * @throws RocksDbException 异常
   */
  void put(String columnFamilyName, byte[] key, byte[] value) throws RocksDbException;

  /**
   * 向默认列族保存数据
   *
   * @param key key
   * @param value value
   * @throws RocksDbException 异常
   */
  void put(byte[] key, byte[] value) throws RocksDbException;

  /**
   * 查询指定列族的键值，如果出现错误或者key不存在，返回null
   *
   * @param columnFamilyName 列族名
   * @param key key
   * @return value
   */
  byte[] get(String columnFamilyName, byte[] key) throws RocksDbException;

  /**
   * 查询默认列族保存数据
   *
   * @param key key
   * @return value
   */
  byte[] get(byte[] key) throws RocksDbException;

  /**
   * 在default ColumnFamily查询多Key
   *
   * @param keys key列表
   * @return kvPairs
   */
  List<Tuple2<byte[], byte[]>> multiGet(byte[]... keys) throws RocksDbException;

  /**
   * 在指定列族查询多Key
   *
   * @param columnFamilyName 列族名
   * @param keys key列表
   * @return kvPairs
   */
  List<Tuple2<byte[], byte[]>> multiGet(String columnFamilyName, byte[]... keys)
      throws RocksDbException;

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
