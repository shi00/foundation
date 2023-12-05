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

import static com.silong.foundation.rocksdbffm.generated.RocksDB.rocksdb_column_family_handle_destroy;
import static com.silong.foundation.rocksdbffm.generated.RocksDB.rocksdb_options_destroy;
import static java.lang.foreign.MemorySegment.NULL;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/**
 * 列族描述对象
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-12-05 9:48
 */
@Data
@Builder
@Slf4j
@Accessors(fluent = true)
class ColumnFamilyDescriptor implements AutoCloseable {
  /** 列族名称 */
  private String columnFamilyName;

  /** 列族options */
  @ToString.Exclude private MemorySegment columnFamilyOptions;

  /** 列族handle */
  @ToString.Exclude private MemorySegment columnFamilyHandle;

  /** 关闭标识 */
  private final AtomicBoolean closed = new AtomicBoolean(false);

  @Override
  public void close() {
    if (log.isDebugEnabled()) {
      log.debug("Free Resources: {}", this);
    }
    if (closed.compareAndSet(false, true)) {
      if (columnFamilyHandle != null && !columnFamilyHandle.equals(NULL)) {
        rocksdb_column_family_handle_destroy(columnFamilyHandle);
        columnFamilyHandle = null;
      }
      if (columnFamilyOptions != null && !columnFamilyOptions.equals(NULL)) {
        rocksdb_options_destroy(columnFamilyOptions);
        columnFamilyOptions = null;
      }
      columnFamilyName = null;
    }
  }
}
