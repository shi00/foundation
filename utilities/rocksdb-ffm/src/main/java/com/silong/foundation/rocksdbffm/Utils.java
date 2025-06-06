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

import static com.silong.foundation.rocksdbffm.generated.RocksDB.C_POINTER;
import static com.silong.foundation.utilities.nlloader.PlatformDetector.*;
import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.*;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.silong.foundation.rocksdbffm.fi.Tuple2;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import lombok.NonNull;
import lombok.SneakyThrows;

/**
 * 工具类
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-12-06 9:10
 */
public class Utils {

  private static final Linker LINKER = Linker.nativeLinker();

  /** 操作系统名 */
  public static final String OS_NAME;

  /** 操作系统名 */
  public static final String OS_ARCH;

  private static final MethodHandle STRLEN =
      LINKER.downcallHandle(
          LINKER.defaultLookup().find("strlen").orElseThrow(),
          FunctionDescriptor.of(JAVA_LONG, ADDRESS));

  private static final MethodHandle FREE =
      LINKER.downcallHandle(
          LINKER.defaultLookup().find("free").orElseThrow(), FunctionDescriptor.ofVoid(ADDRESS));

  public static final String OK = "";

  static {
    Properties properties = new Properties();
    PLATFORM_DETECTOR.detect(properties, List.of());
    OS_NAME = properties.getProperty(DETECTED_NAME);
    OS_ARCH = properties.getProperty(DETECTED_ARCH);
  }

  /**
   * 获取char * 长度
   *
   * @param charPtr 字符串指针
   * @return 长度
   */
  @SneakyThrows
  public static long strlen(@NonNull MemorySegment charPtr) {
    return (long) STRLEN.invokeExact(charPtr);
  }

  /**
   * 释放内存空间
   *
   * @param ptr 指针
   */
  @SneakyThrows
  public static void free(@NonNull MemorySegment ptr) {
    FREE.invokeExact(ptr);
  }

  public static boolean isEmpty(byte[] array) {
    return array == null || array.length == 0;
  }

  public static boolean isEmpty(String str) {
    return str == null || str.isEmpty();
  }

  public static void validateKey(byte[] key) {
    validateByteArrays(key, "key must not be null or empty.");
  }

  @SafeVarargs
  public static void validateKvPairs(Tuple2<byte[], byte[]>... kvPairs) {
    if (kvPairs == null
        || kvPairs.length == 0
        || Arrays.stream(kvPairs).anyMatch(pair -> isEmpty(pair.t1()) || isEmpty(pair.t2()))) {
      throw new IllegalArgumentException("kvPairs must not be null or empty.");
    }
  }

  public static void validateKeys(byte[]... keys) {
    if (keys == null || keys.length == 0 || Arrays.stream(keys).anyMatch(Utils::isEmpty)) {
      throw new IllegalArgumentException("keys must not be null or empty.");
    }
  }

  public static void validateValue(byte[] value) {
    validateByteArrays(value, "value must not be null or empty.");
  }

  public static void validateByteArrays(byte[] array, @NonNull String message) {
    if (isEmpty(array)) {
      throw new IllegalArgumentException(message);
    }
  }

  public static void validateByteArrays(
      byte[] array, int offset, int length, @NonNull String message) {
    if (isEmpty(array)) {
      throw new IllegalArgumentException(message);
    }
    if (offset < 0 || offset >= array.length) {
      throw new IllegalArgumentException(
          String.format(
              "offset must be greater than or equals to 0 and less than %d.", array.length));
    }
    if (length < 0 || length > array.length) {
      throw new IllegalArgumentException(
          String.format(
              "length must be greater than or equals to 0 and less than or equals to %d.",
              array.length));
    }
    if ((length + offset) > array.length) {
      throw new IllegalArgumentException(
          String.format("(length + offset) must be less than or equals to %d.", array.length));
    }
  }

  public static String getUtf8String(@NonNull MemorySegment charPtr, long length) {
    if (length <= 0) {
      throw new IllegalArgumentException("length must be greater than 0.");
    }
    return new String(charPtr.asSlice(0, length).toArray(JAVA_BYTE), UTF_8);
  }

  /**
   * 分配char** 错误信息出参
   *
   * @param arena 内存分配区
   * @return 错误信息指针
   */
  public static MemorySegment newErrPtr(@NonNull Arena arena) {
    return arena.allocate(C_POINTER, 1);
  }

  /**
   * 读取索引为0的错误信息(char** )，如果返回空字符串则表示操作成功，如果返回有错误信息则在读取结果后释放native资源
   *
   * @param errPtr C_POINTER指针
   * @return 错误信息或空字符串
   */
  public static String readErrMsgAndFree(MemorySegment errPtr) {
    return readErrMsgAndFree(errPtr, 0);
  }

  /**
   * 读取指定索引位置的错误信息(char** )，如果返回空字符串则表示操作成功，如果返回有错误信息则在读取结果后释放native资源
   *
   * @param errPtr C_POINTER指针
   * @param index 错误索引
   * @return 错误信息或空字符串
   */
  public static String readErrMsgAndFree(@NonNull MemorySegment errPtr, int index) {
    if (index < 0) {
      throw new IllegalArgumentException("index must be greater than or equals to 0.");
    }
    MemorySegment ptr = errPtr.getAtIndex(C_POINTER, index);
    if (NULL.equals(ptr)) {
      return OK;
    }
    try (Arena arena = Arena.ofConfined()) {
      return getUtf8String(ptr.reinterpret(arena, Utils::free), strlen(ptr));
    }
  }

  /**
   * 根据枚举值查找枚举类型
   *
   * @param value 值
   * @param enumClass 类型
   * @return 枚举
   * @param <T> 枚举类型
   */
  public static <T extends Enum<T>> T enumType(int value, @NonNull Class<T> enumClass) {
    for (T t : enumClass.getEnumConstants()) {
      if (t.ordinal() == value) {
        return t;
      }
    }
    throw new IllegalStateException(
        String.format("%s: Unknown type: %d", enumClass.getName(), value));
  }

  /**
   * boolean转byte，true->1，false->0.
   *
   * @param b true or false
   * @return 1 or 0
   */
  public static byte boolean2Byte(boolean b) {
    return (byte) (b ? 1 : 0);
  }

  public static Boolean byte2Boolean(byte v) {
    return v == 0 ? Boolean.FALSE : Boolean.TRUE;
  }

  /** Forbidden */
  private Utils() {}
}
