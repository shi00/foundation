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

import static com.silong.foundation.rocksdbffm.generated.RocksDB.*;
import static com.silong.foundation.rocksdbffm.generated.RocksDB.C_POINTER;
import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.*;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.silong.foundation.common.lambda.Consumer3;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import lombok.NonNull;
import lombok.SneakyThrows;

/**
 * 工具类
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-12-06 9:10
 */
class Utils {

  private static final Linker LINKER = Linker.nativeLinker();

  private static final MethodHandle STRLEN =
      LINKER.downcallHandle(
          LINKER.defaultLookup().find("strlen").orElseThrow(),
          FunctionDescriptor.of(JAVA_LONG, ADDRESS));

  static final String OK = "";

  /**
   * 获取char * 长度
   *
   * @param charPtr 字符串指针
   * @return 长度
   * @throws Throwable 异常
   */
  @SneakyThrows(Throwable.class)
  public static long strlen(@NonNull MemorySegment charPtr) {
    return (long) STRLEN.invokeExact(charPtr);
  }

  public static boolean isEmpty(byte[] array) {
    return array == null || array.length == 0;
  }

  public static boolean isEmpty(String str) {
    return str == null || str.isEmpty();
  }

  public static void validateColumnFamilyName(String columnFamilyName) {
    if (isEmpty(columnFamilyName)) {
      throw new IllegalArgumentException("columnFamilyName must not be null or empty.");
    }
  }

  public static void validateKey(byte[] key) {
    validateByteArrays(key, "key must not be null or empty.");
  }

  public static void validateValue(byte[] value) {
    validateByteArrays(value, "value must not be null or empty.");
  }

  public static void validateByteArrays(byte[] array, String message) {
    if (isEmpty(array)) {
      throw new IllegalArgumentException(message);
    }
  }

  public static String getUtf8String(MemorySegment charPtr, long length) {
    return new String(charPtr.asSlice(0, length).toArray(JAVA_BYTE), UTF_8);
  }

  public static boolean checkColumnFamilyHandleName(
      Arena arena,
      String expectedName,
      MemorySegment columnFamilyHandle,
      Consumer3<String, String, Boolean> consumer3) {
    MemorySegment cfnLengthPtr = arena.allocate(C_POINTER);
    MemorySegment cfNamePtr =
        rocksdb_column_family_handle_get_name(columnFamilyHandle, cfnLengthPtr);
    String cfn = getUtf8String(cfNamePtr, cfnLengthPtr.get(JAVA_LONG, 0));
    boolean equals = expectedName.equals(cfn);
    consumer3.accept(expectedName, cfn, equals);
    return equals;
  }

  public static void freeDbOptions(MemorySegment optionsPtr) {
    if (!NULL.equals(optionsPtr)) {
      rocksdb_options_destroy(optionsPtr);
    }
  }

  public static void freeColumnFamilyOptions(MemorySegment optionsPtr) {
    if (!NULL.equals(optionsPtr)) {
      rocksdb_options_destroy(optionsPtr);
    }
  }

  public static void freeReadOptions(MemorySegment optionsPtr) {
    if (!NULL.equals(optionsPtr)) {
      rocksdb_readoptions_destroy(optionsPtr);
    }
  }

  public static void freeWriteOptions(MemorySegment optionsPtr) {
    if (!NULL.equals(optionsPtr)) {
      rocksdb_writeoptions_destroy(optionsPtr);
    }
  }

  public static void freeRocksDb(MemorySegment dbPtr) {
    if (!NULL.equals(dbPtr)) {
      rocksdb_close(dbPtr);
    }
  }

  /**
   * 读取错误信息(char** )，如果返回空字符串则表示操作成功
   *
   * @param msgPtr C_POINTER指针
   * @return 错误信息或空字符串
   */
  public static String getErrMsg(MemorySegment msgPtr) {
    MemorySegment ptr = msgPtr.get(C_POINTER, 0);
    return NULL.equals(ptr) ? OK : ptr.getUtf8String(0);
  }

  /** Forbidden */
  private Utils() {}
}
