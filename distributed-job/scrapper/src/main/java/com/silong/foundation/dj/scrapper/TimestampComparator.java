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

package com.silong.foundation.dj.scrapper;

import com.silong.foundation.rocksdbffm.RocksDbComparator;

/**
 * 时间戳比较器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-01-09 19:35
 */
public class TimestampComparator implements RocksDbComparator {
  @Override
  public void release() {}

  private static long fromBytes(
      byte b1, byte b2, byte b3, byte b4, byte b5, byte b6, byte b7, byte b8) {
    return (b1 & 0xFFL) << 56
        | (b2 & 0xFFL) << 48
        | (b3 & 0xFFL) << 40
        | (b4 & 0xFFL) << 32
        | (b5 & 0xFFL) << 24
        | (b6 & 0xFFL) << 16
        | (b7 & 0xFFL) << 8
        | (b8 & 0xFFL);
  }

  @Override
  public int compare(byte[] key1, byte[] key2) {
    if (key1 == null || key1.length != 8) {
      throw new IllegalArgumentException("key1 must not be null or empty and length must be 8.");
    }
    if (key2 == null || key2.length != 8) {
      throw new IllegalArgumentException("key2 must not be null or empty and length must be 8.");
    }
    return Long.compare(
        fromBytes(key1[0], key1[1], key1[2], key1[3], key1[4], key1[5], key1[6], key1[7]),
        fromBytes(key2[0], key2[1], key2[2], key2[3], key2[4], key2[5], key2[6], key2[7]));
  }

  @Override
  public String name() {
    return "Timestamp-Comparator";
  }
}
