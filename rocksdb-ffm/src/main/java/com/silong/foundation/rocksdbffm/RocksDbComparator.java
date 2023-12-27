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

import static com.silong.foundation.rocksdbffm.generated.RocksDB.rocksdb_comparator_create;
import static com.silong.foundation.rocksdbffm.generated.RocksDB.rocksdb_comparator_destroy;
import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;

import com.silong.foundation.rocksdbffm.generated.rocksdb_comparator_create$compare;
import com.silong.foundation.rocksdbffm.generated.rocksdb_comparator_create$destructor;
import com.silong.foundation.rocksdbffm.generated.rocksdb_comparator_create$name;
import java.io.Closeable;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

/**
 * Rocksdb Key Comparator
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-12-19 21:42
 */
public interface RocksDbComparator extends Closeable {

  /**
   * 释放比较器资源
   *
   * @param comparator 比较器
   */
  static void destroy(MemorySegment comparator) {
    if (comparator != null && !NULL.equals(comparator)) {
      rocksdb_comparator_destroy(comparator);
    }
  }

  /**
   * 生成rocksdb使用的比较器
   *
   * @return 比较器
   */
  default MemorySegment comparator() {
    Arena global = Arena.global();
    rocksdb_comparator_create$compare compare =
        (state, key1, len1, key2, len2) -> {
          byte[] k1 = key1.asSlice(0, len1).toArray(JAVA_BYTE);
          byte[] k2 = key2.asSlice(0, len2).toArray(JAVA_BYTE);
          return compare(k1, 0, k1.length, k2, 0, k2.length);
        };
    rocksdb_comparator_create$destructor destructor = state -> close();
    rocksdb_comparator_create$name name = state -> global.allocateUtf8String(name());
    return rocksdb_comparator_create(
        NULL,
        rocksdb_comparator_create$destructor.allocate(destructor, global),
        rocksdb_comparator_create$compare.allocate(compare, global),
        rocksdb_comparator_create$name.allocate(name, global));
  }

  /** 释放比较器资源 */
  @Override
  void close();

  /**
   * 比较key
   *
   * @param a key bytes
   * @param aOffset offset
   * @param aLength length
   * @param b key bytes
   * @param bOffset offset
   * @param bLength length
   * @return a==b返回0，a<b返回-1，否则返回1
   */
  int compare(byte[] a, int aOffset, int aLength, byte[] b, int bOffset, int bLength);

  /**
   * 比较器名称
   *
   * @return 比较器名称
   */
  String name();
}
