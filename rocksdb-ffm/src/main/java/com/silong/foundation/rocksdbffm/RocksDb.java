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

import com.silong.foundation.rocksdbffm.config.RocksDbConfig;
import java.io.Serializable;

/**
 * RocksDB提供的API接口封装
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-09-10 14:44
 */
public interface RocksDb
    extends BasicRocksDbOperation, RocksDbSnapshot, AutoCloseable, Serializable {

  /** 默认列族名称 */
  String DEFAULT_COLUMN_FAMILY_NAME = "default";

  /** 默认共享库名，可以通过启动参数-Drocksdb.library.name自定义，但是需要确保名称与实际动态库名称一致 */
  String DEFAULT_LIB_NAME = "librocksdb";

  /** 共享库名称 */
  String LIB_ROCKSDB = System.getProperty("rocksdb.library.name", DEFAULT_LIB_NAME);

  /** rocksdb共享库目录环境变量 */
  String ROCKSDB_LIBS_DIR = "ROCKSDB_LIBS_DIR";

  int KB = 1024;

  int MB = 1024 * KB;

  int GB = 1024 * MB;

  /**
   * 获取实例
   *
   * @param config 配置
   * @return 实例
   */
  static RocksDb getInstance(RocksDbConfig config) {
    return new RocksDbImpl(config);
  }

  /** 关闭服务，释放native资源 */
  @Override
  void close();
}
