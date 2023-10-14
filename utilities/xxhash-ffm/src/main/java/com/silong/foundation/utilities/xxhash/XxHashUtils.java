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

package com.silong.foundation.utilities.xxhash;

import static com.silong.foundation.utilities.xxhash.generated.Xxhash.*;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;

import com.silong.foundation.utilities.nlloader.NativeLibLoader;
import java.lang.foreign.Arena;

/**
 * 提供xxhash生成工具
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-09-16 14:52
 */
public final class XxHashUtils {

  /** 共享库名称 */
  private static final String LIB_XXHASH = "libxxhash";

  static {
    NativeLibLoader.loadLibrary(LIB_XXHASH);
  }

  /** 工具类禁止实例化 */
  private XxHashUtils() {}

  /**
   * 生成xxhash
   *
   * @param data 数据
   * @return hash码
   */
  public static long hash64(byte[] data) {
    if (data == null || data.length == 0) {
      return 0;
    }
    try (Arena arena = Arena.ofConfined()) {
      return XXH3_64bits_withSeed(arena.allocateArray(JAVA_BYTE, data), data.length, 0xcafebabeL);
    }
  }

  /**
   * 生成xxhash
   *
   * @param data 数据
   * @return hash码
   */
  public static long hash32(byte[] data) {
    if (data == null || data.length == 0) {
      return 0;
    }
    try (Arena arena = Arena.ofConfined()) {
      return XXH32(arena.allocateArray(JAVA_BYTE, data), data.length, 0xcafebabe);
    }
  }
}
