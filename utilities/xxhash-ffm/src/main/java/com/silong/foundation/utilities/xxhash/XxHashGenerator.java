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

import static com.silong.foundation.utilities.xxhash.generated.XxHash.*;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;

import com.silong.foundation.utilities.nlloader.NativeLibLoader;
import java.lang.foreign.Arena;
import java.util.HexFormat;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 提供xxhash生成工具
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-09-16 14:52
 */
public final class XxHashGenerator {

  /** 共享库名称 */
  private static final String LIB_XXHASH = "libxxhash";

  static {
    NativeLibLoader.loadLibrary(LIB_XXHASH);
  }

  /** 工具类禁止实例化 */
  private XxHashGenerator() {}

  /**
   * 生成xxhash
   *
   * @param data 数据
   * @return hash码
   */
  public static long hash64(byte[] data) {
    return hash64(Objects.requireNonNull(data), 0, data.length);
  }

  /**
   * 生成xxhash
   *
   * @param data 数据
   * @param offset offset
   * @param length length
   * @return hash码
   */
  public static long hash64(byte[] data, int offset, int length) {
    check(
        data,
        offset,
        length,
        () ->
            String.format(
                "Invalid data:%s or offset:%d or length:%d.",
                data == null ? null : HexFormat.of().formatHex(data), offset, length));
    try (Arena arena = Arena.ofConfined()) {
      return XXH3_64bits_withSeed(arena.allocateFrom(JAVA_BYTE, data), data.length, 0xcafebabeL);
    }
  }

  /**
   * 生成xxhash
   *
   * @param data 数据
   * @return hash码
   */
  public static int hash32(byte[] data) {
    return hash32(data, 0, data.length);
  }

  /**
   * 生成xxhash
   *
   * @param data 数据
   * @param offset offset
   * @param length length
   * @return hash码
   */
  public static int hash32(byte[] data, int offset, int length) {
    check(
        data,
        offset,
        length,
        () ->
            String.format(
                "Invalid data:%s or offset:%d or length:%d.",
                data == null ? null : HexFormat.of().formatHex(data), offset, length));
    try (Arena arena = Arena.ofConfined()) {
      return XXH32(arena.allocateFrom(JAVA_BYTE, data), data.length, 0xcafebabe);
    }
  }

  private static void check(byte[] bytes, int offset, int length, Supplier<String> msgSupplier) {
    if (bytes == null
        || offset < 0
        || offset >= bytes.length
        || length < 0
        || length > bytes.length
        || (offset + length) > bytes.length) {
      throw new IllegalArgumentException(msgSupplier.get());
    }
  }
}
