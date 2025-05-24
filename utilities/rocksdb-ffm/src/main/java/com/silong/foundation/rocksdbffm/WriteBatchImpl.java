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

import static com.silong.foundation.rocksdbffm.Utils.validateByteArrays;
import static com.silong.foundation.rocksdbffm.generated.RocksDB.*;

import java.io.Serial;
import java.io.Serializable;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import lombok.NonNull;

/**
 * 批量写入操作实现
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-12-08 14:40
 */
class WriteBatchImpl implements WriteBatch, Serializable {

  @Serial private static final long serialVersionUID = -4_438_705_476_035_386_847L;

  MemorySegment writeBatch;

  Arena arena;

  /**
   * 构造方法
   *
   * @param arena 内存分配器
   */
  public WriteBatchImpl(@NonNull Arena arena) {
    this.arena = arena;
    this.writeBatch = rocksdb_writebatch_create();
  }

  @Override
  public void put(
      @NonNull MemorySegment columnFamilyHandle,
      byte[] key,
      int keyOffset,
      int keyLength,
      byte[] value,
      int valueOffset,
      int valueLength) {
    validateByteArrays(key, keyOffset, keyLength, "Invalid key.");
    validateByteArrays(value, valueOffset, valueLength, "Invalid value.");
    MemorySegment keyPtr = arena.allocateFrom(C_CHAR, key).asSlice(keyOffset, keyLength);
    MemorySegment valuePtr = arena.allocateFrom(C_CHAR, value).asSlice(valueOffset, valueLength);
    rocksdb_writebatch_put_cf(
        writeBatch, columnFamilyHandle, keyPtr, keyPtr.byteSize(), valuePtr, valuePtr.byteSize());
  }

  @Override
  public void delete(
      @NonNull MemorySegment columnFamilyHandle, byte[] key, int keyOffset, int keyLength) {
    validateByteArrays(key, keyOffset, keyLength, "Invalid key.");
    MemorySegment keyPtr = arena.allocateFrom(C_CHAR, key).asSlice(keyOffset, keyLength);
    rocksdb_writebatch_delete_cf(writeBatch, columnFamilyHandle, keyPtr, keyPtr.byteSize());
  }

  @Override
  public void deleteRange(
      @NonNull MemorySegment columnFamilyHandle,
      byte[] startKey,
      int startKeyOffset,
      int startKeyLength,
      byte[] endKey,
      int endKeyOffset,
      int endKeyLength) {
    validateByteArrays(startKey, startKeyOffset, startKeyLength, "Invalid startKey.");
    validateByteArrays(endKey, endKeyOffset, endKeyLength, "Invalid endKey.");
    MemorySegment startKeyPtr =
        arena.allocateFrom(C_CHAR, startKey).asSlice(startKeyOffset, startKeyLength);
    MemorySegment endKeyPtr =
        arena.allocateFrom(C_CHAR, endKey).asSlice(endKeyOffset, endKeyLength);
    rocksdb_writebatch_delete_range_cf(
        writeBatch,
        columnFamilyHandle,
        startKeyPtr,
        startKeyPtr.byteSize(),
        endKeyPtr,
        endKeyPtr.byteSize());
  }

  @Override
  public void close() {
    if (writeBatch != null) {
      rocksdb_writebatch_clear(writeBatch);
      rocksdb_writebatch_destroy(writeBatch);
      writeBatch = null;
      arena = null;
    }
  }
}
