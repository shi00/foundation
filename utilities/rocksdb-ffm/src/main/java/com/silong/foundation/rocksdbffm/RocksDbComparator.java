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
import static java.nio.charset.StandardCharsets.UTF_8;

import com.silong.foundation.rocksdbffm.generated.rocksdb_comparator_create$compare;
import com.silong.foundation.rocksdbffm.generated.rocksdb_comparator_create$destructor;
import com.silong.foundation.rocksdbffm.generated.rocksdb_comparator_create$name;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import lombok.NonNull;

/**
 * Rocksdb Key Comparator，必须提供无参构造方法
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-12-19 21:42
 */
public interface RocksDbComparator {

  /**
   * 释放比较器资源
   *
   * @param comparator 比较器
   */
  static void destroy(@NonNull MemorySegment comparator) {
    rocksdb_comparator_destroy(comparator); // c++析构函数会检测null指针，此处无需处理
  }

  /**
   * 生成rocksdb使用的比较器
   *
   * @return 比较器
   */
  default MemorySegment comparator() {
    Arena global = Arena.global();
    return rocksdb_comparator_create(
        NULL,
        rocksdb_comparator_create$destructor.allocate(_ -> release(), global),
        rocksdb_comparator_create$compare.allocate(
            (_, key1, len1, key2, len2) -> {
              byte[] k1 = key1.asSlice(0, len1).toArray(JAVA_BYTE);
              byte[] k2 = key2.asSlice(0, len2).toArray(JAVA_BYTE);
              return compare(k1, k2);
            },
            global),
        rocksdb_comparator_create$name.allocate(_ -> global.allocateFrom(name(), UTF_8), global));
  }

  /** 释放比较器资源 */
  void release();

  /**
   * 比较key
   *
   * @param a key bytes
   * @param b key bytes
   * @return a==b返回0，a<b返回-1，否则返回1
   */
  int compare(byte[] a, byte[] b);

  /**
   * 比较器名称
   *
   * @return 比较器名称
   */
  String name();
}
