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

import static com.silong.foundation.rocksdbffm.Utils.*;
import static com.silong.foundation.rocksdbffm.generated.RocksDB.*;
import static com.silong.foundation.utilities.nlloader.NativeLibLoader.loadLibrary;
import static java.lang.foreign.ValueLayout.*;

import java.lang.foreign.*;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
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
class RocksDbImpl implements RocksDb {
  /** 共享库名称 */
  private static final String LIB_ROCKSDB = "librocksdb";

  static {
    loadLibrary(LIB_ROCKSDB);
  }

  /** rocksdb配置 */
  @Getter private final RocksDbConfig config;

  /** 数据库指针 */
  @ToString.Exclude private final MemorySegment dbPtr;

  /** 数据库配置选项 */
  @ToString.Exclude private final MemorySegment dbOptionsPtr;

  /** 默认数据读取option配置 */
  @ToString.Exclude private final MemorySegment readOptionsPtr;

  @ToString.Exclude private final MemorySegment writeOptionsPtr;

  /** 是否需要关闭 */
  private final AtomicBoolean closed = new AtomicBoolean();

  /** 列族名称与其对应的native值 */
  private final ConcurrentHashMap<String, ColumnFamilyDescriptor> columnFamilies =
      new ConcurrentHashMap<>();

  /**
   * 构造方法
   *
   * @param config 配置
   */
  public RocksDbImpl(@NonNull RocksDbConfig config) {
    try (Arena arena = Arena.ofConfined()) {
      this.config = config;
      MemorySegment dbOptionsPtr = createRocksdbOption(config); // global scope
      MemorySegment path = arena.allocateUtf8String(config.getPersistDataPath());
      MemorySegment errPtr = arena.allocateArray(C_POINTER, 1);
      List<String> columnFamilyNames = getColumnFamilyNames(config.getColumnFamilyNames());

      // 列族名称列表构建指针
      MemorySegment cfNamesPtr = createColumnFamilyNames(arena, columnFamilyNames);

      // 列族选项列表指针
      MemorySegment cfOptionsPtr = createColumnFamilyOptions(arena, columnFamilyNames);

      // 出参，获取打开的列族handles
      MemorySegment cfHandlesPtr = arena.allocateArray(C_POINTER, columnFamilyNames.size());

      // 打开指定的列族
      MemorySegment dbPtr = // global scope
          rocksdb_open_column_families(
              dbOptionsPtr, // rocksdb options
              path, // persist path
              columnFamilyNames.size(), // column family size
              cfNamesPtr, // column family names
              cfOptionsPtr, // column family options
              cfHandlesPtr, // 出参，打开的列族handle
              errPtr); // 错误信息

      String errMsg = getErrMsg(errPtr);
      if (isEmpty(errMsg)) {
        log.info(
            "The rocksdb(path:{}, cfs:{}) is opened successfully.",
            config.getPersistDataPath(),
            columnFamilyNames);
        closed.set(false);
        this.dbPtr = dbPtr;
        this.dbOptionsPtr = dbOptionsPtr;

        // 创建默认读取options
        this.readOptionsPtr = rocksdb_readoptions_create();

        // 创建默认写入options
        this.writeOptionsPtr = rocksdb_writeoptions_create();

        // 保存打开的列族
        saveColumnFamilyDescriptor(arena, columnFamilyNames, cfOptionsPtr, cfHandlesPtr);
      } else {
        log.error(
            "Failed to open rocksdb(path:{}, cfs:{}), reason:{}.",
            config.getPersistDataPath(),
            columnFamilyNames,
            errMsg);
        closed.set(true);
        this.dbPtr = this.dbOptionsPtr = this.readOptionsPtr = this.writeOptionsPtr = null;

        // 释放已经创建出来的global scope资源
        freeRocksDb(dbPtr);
        freeDbOptions(dbOptionsPtr);
        IntStream.range(0, columnFamilyNames.size())
            .forEach(i -> freeColumnFamilyOptions(cfOptionsPtr.getAtIndex(C_POINTER, i)));
      }
    }
  }

  private void saveColumnFamilyDescriptor(
      Arena arena,
      List<String> columnFamilyNames,
      MemorySegment cfOptionsPtr,
      MemorySegment cfHandlesPtr) {
    for (int i = 0; i < columnFamilyNames.size(); i++) {
      String columnFamilyName = columnFamilyNames.get(i);
      MemorySegment cfOptions = cfOptionsPtr.getAtIndex(C_POINTER, i);
      MemorySegment cfHandle = cfHandlesPtr.getAtIndex(C_POINTER, i);

      if (log.isDebugEnabled()) {
        MemorySegment cfnLengthPtr = arena.allocate(C_POINTER);
        MemorySegment cfNamePtr = rocksdb_column_family_handle_get_name(cfHandle, cfnLengthPtr);
        log.debug(
            "cache column family: {}", getUtf8String(cfNamePtr, cfnLengthPtr.get(JAVA_LONG, 0)));
      }

      columnFamilies.put(
          columnFamilyName,
          ColumnFamilyDescriptor.builder()
              .columnFamilyName(columnFamilyName)
              .columnFamilyOptions(cfOptions)
              .columnFamilyHandle(cfHandle)
              .build());
    }
  }

  /**
   * 为每个列族创建一个独立的option
   *
   * @param arena 内存分配器
   * @param columnFamilyNames 列族名称列表
   * @return 列族option列表指针
   */
  private MemorySegment createColumnFamilyOptions(Arena arena, List<String> columnFamilyNames) {
    MemorySegment cfOptionsPtr = arena.allocateArray(C_POINTER, columnFamilyNames.size());
    for (int i = 0; i < columnFamilyNames.size(); i++) {
      cfOptionsPtr.setAtIndex(C_POINTER, i, rocksdb_options_create()); // global scope
    }
    return cfOptionsPtr;
  }

  /**
   * 创建rocksdb option，并根据配置值对option进行赋值
   *
   * @param config 配置
   * @return option指针
   */
  private MemorySegment createRocksdbOption(RocksDbConfig config) {
    MemorySegment optionsPtr = rocksdb_options_create();
    optionsPtr.set(JAVA_BOOLEAN, 0, config.isCreateIfMissing()); // create_if_missing
    optionsPtr.set(
        JAVA_BOOLEAN, 1, config.isCreateMissingColumnFamilies()); // create_missing_column_families
    return optionsPtr;
  }

  /**
   * 根据配置的列族名称列表构建指针，如果未配置default列族则会添加此默认列族
   *
   * @param arena 内存分配器
   * @param columnFamilyNames 列族名称列表
   * @return 列族名称列表指针
   */
  private MemorySegment createColumnFamilyNames(Arena arena, List<String> columnFamilyNames) {
    MemorySegment cfNamesPtr = arena.allocateArray(C_POINTER, columnFamilyNames.size());
    for (int i = 0; i < columnFamilyNames.size(); i++) {
      cfNamesPtr.setAtIndex(C_POINTER, i, arena.allocateUtf8String(columnFamilyNames.get(i)));
    }
    return cfNamesPtr;
  }

  private List<String> getColumnFamilyNames(List<String> columnFamilyNames) {
    if (columnFamilyNames == null) {
      columnFamilyNames = List.of(DEFAULT_COLUMN_FAMILY_NAME);
    } else {
      columnFamilyNames.add(DEFAULT_COLUMN_FAMILY_NAME);
      columnFamilyNames = columnFamilyNames.parallelStream().distinct().toList(); // 列族名称去重
    }
    return columnFamilyNames;
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
    // clean only once
    if (closed.compareAndSet(false, true)) {
      columnFamilies.forEach((k, v) -> v.close());
      freeRocksDb(dbPtr);
      freeDbOptions(dbOptionsPtr);
      freeReadOptions(readOptionsPtr);
      freeWriteOptions(writeOptionsPtr);
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
    ColumnFamilyDescriptor descriptor =
        columnFamilies.computeIfAbsent(
            cf,
            key -> {
              try (Arena arena = Arena.ofConfined()) {
                if (log.isDebugEnabled()) {
                  log.debug("Prepare to create column family: {}.", key);
                }
                MemorySegment cfOptions = rocksdb_options_create(); // global scope
                MemorySegment cfNamesPtr = arena.allocateArray(C_POINTER, 1);
                cfNamesPtr.set(C_POINTER, 0, arena.allocateUtf8String(key));
                MemorySegment errPtr = arena.allocateArray(C_POINTER, 1); // 出参，错误消息
                MemorySegment createdCfsSize = arena.allocate(C_POINTER); // 出参，成功创建列族长度
                MemorySegment cfHandlesPtr =
                    rocksdb_create_column_families(
                        dbPtr, cfOptions, 1, cfNamesPtr, createdCfsSize, errPtr);

                // 创建一个列族
                assert createdCfsSize.get(JAVA_LONG, 0) == 1L;

                String errMsg = getErrMsg(errPtr);
                if (errMsg.isEmpty()) {
                  MemorySegment columnFamilyHandle = cfHandlesPtr.get(C_POINTER, 0);
                  assert checkColumnFamilyHandleName(
                      arena,
                      key,
                      columnFamilyHandle,
                      (e, c, b) -> {
                        if (log.isDebugEnabled()) {
                          log.debug("(expectedName: {} == columnFamilyName:{}) = {}", e, c, b);
                        }
                      });
                  if (log.isDebugEnabled()) {
                    log.debug("Successfully created column family: {}", key);
                  }

                  return ColumnFamilyDescriptor.builder()
                      .columnFamilyName(key)
                      .columnFamilyOptions(cfOptions)
                      .columnFamilyHandle(columnFamilyHandle)
                      .build();
                } else {
                  log.error("Failed to create column family: {}, reason:{}.", key, errMsg);
                  return null;
                }
              }
            });
    return descriptor != null;
  }

  @Override
  public void dropColumnFamily(String cf) {
    if (isEmpty(cf)) {
      throw new IllegalArgumentException("cf must not be null or empty.");
    }
    if (columnFamilies.containsKey(cf)) {
      try (Arena arena = Arena.ofConfined()) {
        MemorySegment errPtr = arena.allocateArray(C_POINTER, 1);
        rocksdb_drop_column_family(dbPtr, columnFamilies.get(cf).columnFamilyHandle(), errPtr);
        String errMsg = getErrMsg(errPtr);
        if (errMsg.isEmpty()) {
          columnFamilies.remove(cf).close();
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
  public void put(String columnFamilyName, byte[] key, byte[] value) {
    validateColumnFamilyName(columnFamilyName);
    validateKey(key);
    validateValue(value);
    validateOpenStatus();
    try (Arena arena = Arena.ofConfined()) {
      MemorySegment keyPtr = arena.allocateArray(JAVA_BYTE, key);
      MemorySegment valPtr = arena.allocateArray(JAVA_BYTE, value);
      MemorySegment errPtr = arena.allocateArray(C_POINTER, 1);
      rocksdb_put_cf(
          dbPtr,
          writeOptionsPtr,
          columnFamilies.get(columnFamilyName).columnFamilyHandle(),
          keyPtr,
          keyPtr.byteSize(),
          valPtr,
          valPtr.byteSize(),
          errPtr);

      String errMsg = getErrMsg(errPtr);
      if (isEmpty(errMsg)) {
        if (log.isDebugEnabled()) {
          log.debug(
              "Successfully put kv:[{}---{}] to cf:{}",
              HexFormat.of().formatHex(key),
              HexFormat.of().formatHex(value),
              columnFamilyName);
        }
      } else {
        log.error(
            "Failed to put kv:[{}---{}] to cf:{}",
            HexFormat.of().formatHex(key),
            HexFormat.of().formatHex(value),
            columnFamilyName);
      }
    }
  }

  @Override
  public void put(byte[] key, byte[] value) {
    put(DEFAULT_COLUMN_FAMILY_NAME, key, value);
  }

  @Override
  @Nullable
  public byte[] get(String columnFamilyName, byte[] key) {
    validateColumnFamilyName(columnFamilyName);
    validateKey(key);
    validateOpenStatus();
    try (Arena arena = Arena.ofConfined()) {
      MemorySegment keyPtr = arena.allocateArray(JAVA_BYTE, key);
      MemorySegment errPtr = arena.allocateArray(C_POINTER, 1);
      MemorySegment valLenPtr = arena.allocate(C_POINTER); // 出参，value长度
      MemorySegment valPtr =
          rocksdb_get(dbPtr, readOptionsPtr, keyPtr, keyPtr.byteSize(), valLenPtr, errPtr);
      String errMsg = getErrMsg(errPtr);
      if (isEmpty(errMsg)) {
        long valLen = valLenPtr.get(JAVA_LONG, 0);
        byte[] val = valPtr.asSlice(0, valLen).toArray(JAVA_BYTE);
        if (log.isDebugEnabled()) {
          log.debug(
              "Successfully get value:[{}---{}] from cf:{} by key:{}",
              valLen,
              val,
              columnFamilyName,
              HexFormat.of().formatHex(key));
        }
        return val;
      } else {
        log.error(
            "Failed to get value from cf:{} by key:{}",
            columnFamilyName,
            HexFormat.of().formatHex(key));
        return null;
      }
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
}
