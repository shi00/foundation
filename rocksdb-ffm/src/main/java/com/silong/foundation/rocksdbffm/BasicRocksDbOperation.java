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

/**
 * Rocksdb 基础操作
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-12-02 11:14
 */
public interface BasicRocksDbOperation extends RocksDb {

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
   * key是否存在，采用布隆过滤器，返回false则表示真的不存在，返回true表示一定概率不存在
   *
   * @param columnFamilyName 列族名称
   * @param key key
   * @return true or false
   */
  boolean keyMayExist(String columnFamilyName, byte[] key);

  /**
   * 向指定列族保存数据
   *
   * @param columnFamilyName 列族名
   * @param key key
   * @param value value
   */
  void put(String columnFamilyName, byte[] key, byte[] value);

  /**
   * 向默认列族保存数据
   *
   * @param key key
   * @param value value
   */
  void put(byte[] key, byte[] value);

  /**
   * 查询指定列族的键值
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
}
