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

/**
 * hash工具方法
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-30 10:07
 */
public interface HashUtils {

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
