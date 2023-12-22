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

package com.silong.foundation.rocksdbffm.enu;

import lombok.Getter;

/**
 * rocksdb内部使用的压缩类型
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-12-22 11:19
 */
@Getter
public enum CompressionType {
  K_NO_COMPRESSION(0),

  K_SNAPPY_COMPRESSION(1),

  K_ZLIB_COMPRESSION(2),

  K_BZIP2_COMPRESSION(3),

  K_LZ4_COMPRESSION(4),

  K_LZ4HC_COMPRESSION(5),

  K_XPRESS_COMPRESSION(6),

  K_ZSTD(7),

  // Only use kZSTDNotFinalCompression if you have to use ZSTD lib older than
  // 0.8.0 or consider a possibility of downgrading the service or copying
  // the database files to another service running with an older version of
  // RocksDB that doesn't have kZSTD. Otherwise, you should use kZSTD. We will
  // eventually remove the option from the public API.
  K_ZSTD_NOT_FINAL_COMPRESSION(0x40),

  // kDisableCompressionOption is used to disable some compression options.
  K_DISABLE_COMPRESSION_OPTION(0xff);

  final int value;

  CompressionType(int value) {
    this.value = value;
  }
}
