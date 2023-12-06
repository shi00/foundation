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
 * RocksDB提供的API接口封装
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-09-10 14:44
 */
public interface RocksDb extends BasicRocksDbOperation, AutoCloseable {

  /** 默认列族名称 */
  String DEFAULT_COLUMN_FAMILY_NAME = "default";

  /**
   * 获取实例
   *
   * @param config 配置
   * @return 实例
   */
  static RocksDb getInstance(RocksDbConfig config) {
    return new RocksDbImpl(config);
  }
}
