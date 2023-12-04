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
import static com.silong.foundation.utilities.nlloader.NativeLibLoader.loadLibrary;
import static java.lang.foreign.ValueLayout.*;

import java.lang.foreign.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * RocksDB 实现
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-09-10 15:27
 */
@Slf4j
@ToString
class RocksDbImpl implements BasicRocksDbOperation {
  /** 共享库名称 */
  private static final String LIB_ROCKSDB = "librocksdb";

  private static final String OK = "";

  static {
    loadLibrary(LIB_ROCKSDB);
  }

  /** rocksdb配置 */
  @Getter private final RocksDbConfig config;

  /** 数据库指针 */
  @ToString.Exclude private final MemorySegment dbPtr;

  /** 数据库配置选项 */
  @ToString.Exclude private final MemorySegment optionsPtr;

  /** 默认数据读取option配置 */
  @ToString.Exclude private final MemorySegment readOptionsPtr;

  /** 是否需要关闭 */
  private final AtomicBoolean closed = new AtomicBoolean();

  /** 列族名称与其对应的native值 */
  private final ConcurrentHashMap<String, MemorySegment> columnFamilies = new ConcurrentHashMap<>();

  /**
   * 构造方法
   *
   * @param config 配置
   */
  public RocksDbImpl(@NonNull RocksDbConfig config) {
    this.config = config;
    try (Arena arena = Arena.ofConfined()) {
      MemorySegment optionsPtr = rocksdb_options_create();
      optionsPtr.set(JAVA_BOOLEAN, 0, config.isCreateIfMissing()); // create_if_missing
      optionsPtr.set(
          JAVA_BOOLEAN,
          1,
          config.isCreateMissingColumnFamilies()); // create_missing_column_families
      MemorySegment path = arena.allocateUtf8String(config.getPersistDataPath());
      MemorySegment errPtr = arena.allocate(C_POINTER);
      MemorySegment dbPtr = rocksdb_open(optionsPtr, path, errPtr);
      String errMsg = getErrMsg(errPtr);
      if (isEmpty(errMsg)) {
        log.info("The rocksdb[{}] is opened successfully.", config.getPersistDataPath());
        closed.set(false);
        this.dbPtr = dbPtr;
        this.optionsPtr = optionsPtr;

        // 创建默认读取option
        this.readOptionsPtr = rocksdb_readoptions_create();
      } else {
        log.info("Failed to open rocksdb[{}], reason:{}.", config.getPersistDataPath(), errMsg);
        closed.set(true);
        this.dbPtr = this.optionsPtr = this.readOptionsPtr = null;
      }
    }
  }

  /**
   * rocksdb是否已经成功打开
   *
   * @return true or false
   */
  public boolean isOpen() {
    return !closed.get();
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      rocksdb_close(dbPtr);
      rocksdb_options_destroy(optionsPtr);
      rocksdb_readoptions_destroy(readOptionsPtr);
      columnFamilies.forEach((k, v) -> rocksdb_column_family_handle_destroy(v));
      columnFamilies.clear();
    }
  }

  /**
   * 列族是否存在
   *
   * @param cf 列族名
   * @return true or false
   */
  public boolean isColumnFamilyExist(String cf) {
    if (isEmpty(cf)) {
      throw new IllegalArgumentException("cf must not be null or empty.");
    }
    return columnFamilies.containsKey(cf);
  }

  @Override
  public boolean createColumnFamily(String cf) {
    if (isEmpty(cf)) {
      throw new IllegalArgumentException("cf must not be null or empty.");
    }
    MemorySegment cfHandle =
        columnFamilies.computeIfAbsent(
            cf,
            key -> {
              try (Arena arena = Arena.ofConfined()) {
                if (log.isDebugEnabled()) {
                  log.debug("Prepare to create column family: {}.", cf);
                }
                MemorySegment columnFamilyNames = arena.allocateUtf8String(key);
                MemorySegment cfsNamePtr = arena.allocate(C_POINTER, columnFamilyNames);
                MemorySegment errPtr = arena.allocate(C_POINTER); // 出参，错误消息
                MemorySegment createdCfsSize = arena.allocate(C_POINTER); // 出参，成功创建列族长度
                MemorySegment cfHandlesPtr =
                    rocksdb_create_column_families(
                        dbPtr, optionsPtr, 1, cfsNamePtr, createdCfsSize, errPtr);
                String errMsg = getErrMsg(errPtr);
                if (errMsg.isEmpty()) {
                  if (log.isDebugEnabled()) {
                    log.debug("Successfully created column family: {}", cf);
                  }
                  return cfHandlesPtr.get(C_POINTER, 0);
                } else {
                  log.error("Failed to create column family: {}, reason:{}.", cf, errMsg);
                  return MemorySegment.NULL;
                }
              }
            });
    return !cfHandle.equals(MemorySegment.NULL);
  }

  @Override
  public void dropColumnFamily(String cf) {
    if (isEmpty(cf)) {
      throw new IllegalArgumentException("cf must not be null or empty.");
    }
    if (columnFamilies.containsKey(cf)) {
      try (Arena arena = Arena.ofConfined()) {
        MemorySegment errPtr = arena.allocate(C_POINTER);
        MemorySegment cfHandle = columnFamilies.get(cf);
        rocksdb_drop_column_family(dbPtr, cfHandle, errPtr);
        String errMsg = getErrMsg(errPtr);
        if (errMsg.isEmpty()) {
          rocksdb_column_family_handle_destroy(columnFamilies.remove(cf));
          if (log.isDebugEnabled()) {
            log.debug("Successfully dropped column family: {}", cf);
          }
        } else {
          log.error("Failed to drop column family: {}, reason:{}.", cf, errMsg);
        }
      }
    }
  }

  @Override
  public boolean keyMayExist(String columnFamilyName, byte[] key) {
    //    RocksDB.rocksdb_key_may_exist_cf()

    return false;
  }

  @Override
  public void put(String columnFamilyName, byte[] key, byte[] value) {}

  @Override
  public void put(byte[] key, byte[] value) {
    put(DEFAULT_COLUMN_FAMILY_NAME, key, value);
  }

  @Override
  public byte[] get(String columnFamilyName, byte[] key) {
    validateColumnFamilyName(columnFamilyName);
    validateKey(key);
    validateOpenStatus();
    try (Arena arena = Arena.ofConfined()) {
      MemorySegment keyPtr = arena.allocateArray(JAVA_BYTE, key);
      MemorySegment errPtr = arena.allocate(C_POINTER);
      AddressLayout valLengthLayout = ADDRESS.withTargetLayout(JAVA_LONG);
      MemorySegment valLength = arena.allocate(valLengthLayout);
      MemorySegment value =
          rocksdb_get(dbPtr, optionsPtr, keyPtr, keyPtr.byteSize(), valLength, errPtr);

      return null;
    }
  }

  @Override
  public byte[] get(byte[] key) {
    return get(DEFAULT_COLUMN_FAMILY_NAME, key);
  }

  private void validateOpenStatus() {
    if (!isOpen()) {
      throw new IllegalStateException("rocksdb status abnormality.");
    }
  }

  private static boolean isEmpty(byte[] array) {
    return array == null || array.length == 0;
  }

  private static boolean isEmpty(String str) {
    return str == null || str.isEmpty();
  }

  private static void validateColumnFamilyName(String columnFamilyName) {
    if (isEmpty(columnFamilyName)) {
      throw new IllegalArgumentException("columnFamilyName must not be null or empty.");
    }
  }

  private static void validateKey(byte[] key) {
    validateByteArrays(key, "key must not be null or empty.");
  }

  private static void validateValue(byte[] value) {
    validateByteArrays(value, "value must not be null or empty.");
  }

  private static void validateByteArrays(byte[] array, String message) {
    if (isEmpty(array)) {
      throw new IllegalArgumentException(message);
    }
  }

  /**
   * 读取错误信息(char** )，如果返回空字符串则表示操作成功
   *
   * @param msgPtr C_POINTER指针
   * @return 错误信息或空字符串
   */
  private static String getErrMsg(MemorySegment msgPtr) {
    MemorySegment ptr = msgPtr.get(C_POINTER, 0);
    return ptr.equals(MemorySegment.NULL) ? OK : ptr.getUtf8String(0);
  }
}
