/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.silong.foundation.devastator.utils;

import net.jpountz.xxhash.XXHash64;
import net.jpountz.xxhash.XXHashFactory;

import static org.apache.commons.lang3.ArrayUtils.isEmpty;

/**
 * 通用工具
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-07-09 23:42
 */
public interface Utilities {

  /** xxhash64 seed */
  long XX_HASH_64_SEED = 0xcafebabe;

  /**
   * The maximum capacity, used if a higher value is implicitly specified by either of the
   * constructors with arguments. MUST be a power of two <= 1<<30.
   */
  int MAXIMUM_CAPACITY = 1 << 30;

  /** xxhash64 */
  XXHash64 XX_HASH_64 = XXHashFactory.fastestInstance().hash64();

  /**
   * 计算xxhash64
   *
   * @param val data
   * @return hash值
   */
  static long xxhash64(byte[] val) {
    if (isEmpty(val)) {
      throw new IllegalArgumentException("val must not be null or empty.");
    }
    return XX_HASH_64.hash(val, 0, val.length, XX_HASH_64_SEED);
  }

  /**
   * 取离size最近的2的指数值
   *
   * @param size 数值
   * @return 值
   */
  static int powerOf2(int size) {
    int n = -1 >>> Integer.numberOfLeadingZeros(size - 1);
    return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
  }

  /**
   * 把两个int类型hash值组合成一个long型hash。<br>
   * 基于Wang/Jenkins hash
   *
   * @param val1 Hash1
   * @param val2 Hash2
   * @see <a href="https://gist.github.com/badboy/6267743#64-bit-mix-functions">64 bit mix
   *     functions</a>
   * @return long hash
   */
  static long mixHash(int val1, int val2) {
    long key = (val1 & 0xFFFFFFFFL) | ((val2 & 0xFFFFFFFFL) << 32);
    key = (~key) + (key << 21);
    key ^= (key >>> 24);
    key += (key << 3) + (key << 8);
    key ^= (key >>> 14);
    key += (key << 2) + (key << 4);
    key ^= (key >>> 28);
    key += (key << 31);
    return key;
  }
}
