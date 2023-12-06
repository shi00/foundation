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

import static com.silong.foundation.rocksdbffm.Utils.*;
import static com.silong.foundation.rocksdbffm.generated.RocksDB.*;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

import com.silong.foundation.common.lambda.Tuple2;
import com.silong.foundation.rocksdbffm.generated.RocksDB;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.function.BiFunction;
import lombok.AllArgsConstructor;
import lombok.NonNull;

/**
 * 迭代器实现
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-12-06 22:40
 */
@AllArgsConstructor
class RocksDbIteratorImpl implements RocksDbIterator {

  /** 迭代器 */
  @NonNull private MemorySegment iterator;

  private byte[] get(BiFunction<MemorySegment, MemorySegment, MemorySegment> function) {
    try (Arena arena = Arena.ofConfined()) {
      MemorySegment lengthPtr = arena.allocate(C_POINTER);
      MemorySegment keyPtr = function.apply(iterator, lengthPtr);
      long length = lengthPtr.get(JAVA_LONG, 0);
      keyPtr = keyPtr.reinterpret(length, arena, Utils::free);
      return keyPtr.toArray(C_CHAR);
    }
  }

  @Override
  public Tuple2<byte[], byte[]> get() {
    return new Tuple2<>(get(RocksDB::rocksdb_iter_key), get(RocksDB::rocksdb_iter_value));
  }

  @Override
  public boolean isValid() {
    return rocksdb_iter_valid(iterator) != 0;
  }

  @Override
  public void checkStatus() throws RocksDbException {
    try (Arena arena = Arena.ofConfined()) {
      MemorySegment errPtr = arena.allocateArray(C_POINTER, 1);
      rocksdb_iter_get_error(iterator, errPtr);
      String errMsg = getErrMsg(errPtr);
      if (!OK.equals(errMsg)) {
        throw new RocksDbException(errMsg);
      }
    }
  }

  @Override
  public void seekToFirst() {
    rocksdb_iter_seek_to_first(iterator);
  }

  @Override
  public void seekToLast() {
    rocksdb_iter_seek_to_last(iterator);
  }

  @Override
  public void seek(byte[] target) {
    validateKey(target);
    try (Arena arena = Arena.ofConfined()) {
      MemorySegment targetPtr = arena.allocateArray(C_CHAR, target);
      rocksdb_iter_seek(iterator, targetPtr, targetPtr.byteSize());
    }
  }

  @Override
  public void seekForPrev(byte[] target) {
    validateKey(target);
    try (Arena arena = Arena.ofConfined()) {
      MemorySegment targetPtr = arena.allocateArray(C_CHAR, target);
      rocksdb_iter_seek_for_prev(iterator, targetPtr, targetPtr.byteSize());
    }
  }

  @Override
  public void next() {
    rocksdb_iter_next(iterator);
  }

  @Override
  public void prev() {
    rocksdb_iter_prev(iterator);
  }

  @Override
  public void close() {
    rocksdb_iter_destroy(iterator);
  }
}
