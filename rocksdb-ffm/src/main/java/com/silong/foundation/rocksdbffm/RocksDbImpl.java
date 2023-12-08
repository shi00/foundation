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
import static java.time.temporal.ChronoUnit.SECONDS;

import com.silong.foundation.common.lambda.Tuple2;
import java.io.Serial;
import java.lang.foreign.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
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

  @Serial private static final long serialVersionUID = -2_521_667_833_385_354_826L;

  /** 共享库名称 */
  private static final String LIB_ROCKSDB =
      System.getProperty("rocksdb.library.name", "librocksdb");

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
      MemorySegment dbOptionsPtr = createRocksdbOption(config); // global scope，close释放
      MemorySegment path = arena.allocateUtf8String(config.getPersistDataPath());
      MemorySegment errPtr = newErrPtr(arena); // 出参，获取错误信息

      // 构建列族列表对应的ttl列表
      Map<String, Integer> columnFamilyNameTTLs =
          getColumnFamilyNames(
              arena,
              config.getDefaultColumnFamilyTTL(),
              dbOptionsPtr,
              path,
              config.getColumnFamilyNameWithTTL());
      List<String> columnFamilyNames = new LinkedList<>();
      MemorySegment ttlsPtr = arena.allocateArray(C_INT, columnFamilyNameTTLs.size());
      int index = 0;
      for (Map.Entry<String, Integer> entry : columnFamilyNameTTLs.entrySet()) {
        columnFamilyNames.add(entry.getKey());
        ttlsPtr.setAtIndex(C_INT, index++, entry.getValue());
      }

      // 列族名称列表构建指针
      MemorySegment cfNamesPtr = createColumnFamilyNames(arena, columnFamilyNames);

      // 列族选项列表指针，cfOptions close时释放
      MemorySegment cfOptionsPtr = createColumnFamilyOptions(arena, columnFamilyNames);

      // 出参，获取打开的列族handles，close时释放
      MemorySegment cfHandlesPtr = arena.allocateArray(C_POINTER, columnFamilyNames.size());

      // 打开指定的列族
      MemorySegment dbPtr = // global scope，close释放
          rocksdb_open_column_families_with_ttl(
              dbOptionsPtr, // rocksdb options
              path, // persist path
              columnFamilyNames.size(), // column family size
              cfNamesPtr, // column family names
              cfOptionsPtr, // column family options
              cfHandlesPtr, // 出参，打开的列族handle
              ttlsPtr, // 每个列族对应的ttl时间
              errPtr); // 错误信息

      String errMsg = readErrMsgAndFree(errPtr);
      if (OK.equals(errMsg)) {
        log.info(
            "The rocksdb(path:{}, cfs:{}) is opened successfully.",
            config.getPersistDataPath(),
            columnFamilyNameTTLs);
        closed.set(false);
        this.dbPtr = dbPtr;
        this.dbOptionsPtr = dbOptionsPtr;

        // 创建默认读取options，global scope close时释放
        this.readOptionsPtr = rocksdb_readoptions_create();

        // 创建默认写入options，global scope close时释放
        this.writeOptionsPtr = rocksdb_writeoptions_create();

        // 保存打开的列族
        cacheColumnFamilyDescriptor(arena, columnFamilyNames, cfOptionsPtr, cfHandlesPtr);
      } else {
        log.error(
            "Failed to open rocksdb(path:{}, cfs:{}), reason:{}.",
            config.getPersistDataPath(),
            columnFamilyNameTTLs,
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

  private List<String> listExistColumnFamilies(
      Arena arena, MemorySegment dbOptions, MemorySegment dbPath) {
    MemorySegment lenPtr = arena.allocate(C_POINTER);
    MemorySegment errPtr = newErrPtr(arena);
    MemorySegment cfsPtr =
        rocksdb_list_column_families(dbOptions, dbPath, lenPtr, errPtr)
            .reinterpret(arena, Utils::free);
    String errMsg = readErrMsgAndFree(errPtr);
    List<String> columnFamilies = new LinkedList<>();
    if (OK.equals(errMsg)) {
      long length = lenPtr.get(JAVA_LONG, 0);
      for (int i = 0; i < length; i++) {
        columnFamilies.add(cfsPtr.getAtIndex(C_POINTER, i).getUtf8String(0));
      }
      log.info(
          "ColumnFamilies:{} that currently exist in rocksdb:[{}]",
          columnFamilies,
          dbPath.getUtf8String(0));
    } else {
      log.info(
          "Unable to list columnFamilies of rocksdb:[{}]，prepare to create db. reason:{}",
          dbPath.getUtf8String(0),
          errMsg);
    }
    return columnFamilies;
  }

  private void cacheColumnFamilyDescriptor(
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

  private Map<String, Integer> getColumnFamilyNames(
      Arena arena,
      int defaultTTL,
      MemorySegment dbOptionsPtr,
      MemorySegment path,
      Map<String, Duration> columnFamilyNames) {
    // 获取当前数据库存在的列族列表
    List<String> cfs = listExistColumnFamilies(arena, dbOptionsPtr, path);
    if (columnFamilyNames == null) {
      if (cfs.isEmpty()) {
        return Map.of(DEFAULT_COLUMN_FAMILY_NAME, defaultTTL);
      }
      log.warn(
          "The column families that need to be opened are not specified, so all column families that exist in rocksdb are opened with ttl 0. cfs: {}",
          cfs);
      return cfs.stream().collect(Collectors.toMap(cf -> cf, cf -> defaultTTL));
    } else {
      // 添加默认列族
      if (!columnFamilyNames.containsKey(DEFAULT_COLUMN_FAMILY_NAME)) {
        columnFamilyNames.put(DEFAULT_COLUMN_FAMILY_NAME, Duration.ZERO);
      }

      // 添加已存在的列族到打开列表中，超时为0
      cfs.forEach(
          cfn -> {
            if (!columnFamilyNames.containsKey(cfn)) {
              columnFamilyNames.put(cfn, Duration.of(defaultTTL, SECONDS));
              log.info(
                  "Add column family {} to the list of column families to be opened with ttl 0.",
                  cfn);
            }
          });
      return columnFamilyNames.entrySet().stream()
          .collect(Collectors.toMap(Map.Entry::getKey, e -> (int) e.getValue().toSeconds()));
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

  @Override
  public Collection<String> openedColumnFamilies() {
    return columnFamilies.keySet();
  }

  @Override
  public boolean createColumnFamily(String columnFamilyName) {
    if (isEmpty(columnFamilyName)) {
      throw new IllegalArgumentException("columnFamilyName must not be null or empty.");
    }
    ColumnFamilyDescriptor descriptor =
        columnFamilies.computeIfAbsent(
            columnFamilyName,
            key -> {
              try (Arena arena = Arena.ofConfined()) {
                if (log.isDebugEnabled()) {
                  log.debug("Prepare to create column family: {}.", key);
                }
                MemorySegment cfOptions = rocksdb_options_create(); // global scope，cached 待close时释放
                MemorySegment cfNamesPtr = arena.allocateArray(C_POINTER, 1);
                cfNamesPtr.set(C_POINTER, 0, arena.allocateUtf8String(key));
                MemorySegment errPtr = newErrPtr(arena); // 出参，错误消息
                MemorySegment createdCfsSize = arena.allocate(C_POINTER); // 出参，成功创建列族长度
                MemorySegment cfHandlesPtr =
                    rocksdb_create_column_families(
                        dbPtr, cfOptions, 1, cfNamesPtr, createdCfsSize, errPtr);

                // 创建一个列族
                assert createdCfsSize.get(JAVA_LONG, 0) == 1L;

                String errMsg = readErrMsgAndFree(errPtr);
                if (OK.equals(errMsg)) {
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
        MemorySegment errPtr = newErrPtr(arena);
        rocksdb_drop_column_family(dbPtr, columnFamilies.get(cf).columnFamilyHandle(), errPtr);
        String errMsg = readErrMsgAndFree(errPtr);
        if (OK.equals(errMsg)) {
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
  public void delete(byte[] key) {
    delete(DEFAULT_COLUMN_FAMILY_NAME, key);
  }

  @Override
  public void delete(String columnFamilyName, byte[] key) {
    validateKey(key);
    delete(columnFamilyName, key, 0, key.length);
  }

  @Override
  public void delete(String columnFamilyName, byte[] key, int offset, int length) {
    validateColumnFamily(columnFamilyName);
    validateByteArrays(key, offset, length, "Invalid key.");
    validateOpenStatus();

    try (Arena arena = Arena.ofConfined()) {
      MemorySegment errPtr = newErrPtr(arena);
      MemorySegment keyPtr = arena.allocateArray(C_CHAR, key).asSlice(offset, length);
      rocksdb_delete_cf(
          dbPtr,
          writeOptionsPtr,
          columnFamilies.get(columnFamilyName).columnFamilyHandle(),
          keyPtr,
          keyPtr.byteSize(),
          errPtr);
      String errMsg = readErrMsgAndFree(errPtr);
      if (OK.equals(errMsg)) {
        if (log.isDebugEnabled()) {
          log.debug(
              "Successfully delete key:{} from columnFamily:{}",
              HexFormat.of().formatHex(key),
              columnFamilyName);
        }
      } else {
        log.error(
            "Failed to delete key:{} from columnFamily:{}, reason:{}",
            HexFormat.of().formatHex(key),
            columnFamilyName,
            errMsg);
      }
    }
  }

  @Override
  public void deleteRange(byte[] startKey, byte[] endKey) {
    deleteRange(DEFAULT_COLUMN_FAMILY_NAME, startKey, endKey);
  }

  @Override
  public void deleteRange(String columnFamilyName, byte[] startKey, byte[] endKey) {
    validateColumnFamily(columnFamilyName);
    validateKey(startKey);
    validateKey(endKey);
    validateOpenStatus();
    try (Arena arena = Arena.ofConfined()) {
      MemorySegment errPtr = newErrPtr(arena);
      MemorySegment startKeyPtr = arena.allocateArray(C_CHAR, startKey);
      MemorySegment endKeyPtr = arena.allocateArray(C_CHAR, endKey);
      rocksdb_delete_range_cf(
          dbPtr,
          writeOptionsPtr,
          columnFamilies.get(columnFamilyName).columnFamilyHandle(),
          startKeyPtr,
          startKeyPtr.byteSize(),
          endKeyPtr,
          endKeyPtr.byteSize(),
          errPtr);
      String errMsg = readErrMsgAndFree(errPtr);
      if (OK.equals(errMsg)) {
        if (log.isDebugEnabled()) {
          log.debug(
              "Successfully delete keys from startKey:{} to endKey:{} in cf:{}",
              HexFormat.of().formatHex(startKey),
              HexFormat.of().formatHex(endKey),
              columnFamilyName);
        }
      } else {
        log.error(
            "Failed to delete keys from startKey:{} to endKey:{} in cf:{}. reason:{}",
            HexFormat.of().formatHex(startKey),
            HexFormat.of().formatHex(endKey),
            columnFamilyName,
            errMsg);
      }
    }
  }

  @Override
  public void put(String columnFamilyName, byte[] key, byte[] value) {
    validateColumnFamily(columnFamilyName);
    validateKey(key);
    validateValue(value);
    validateOpenStatus();
    try (Arena arena = Arena.ofConfined()) {
      MemorySegment keyPtr = arena.allocateArray(C_CHAR, key);
      MemorySegment valPtr = arena.allocateArray(C_CHAR, value);
      MemorySegment errPtr = newErrPtr(arena);
      rocksdb_put_cf(
          dbPtr,
          writeOptionsPtr,
          columnFamilies.get(columnFamilyName).columnFamilyHandle(),
          keyPtr,
          keyPtr.byteSize(),
          valPtr,
          valPtr.byteSize(),
          errPtr);

      String errMsg = readErrMsgAndFree(errPtr);
      if (OK.equals(errMsg)) {
        if (log.isDebugEnabled()) {
          log.debug(
              "Successfully put kv:[{}---{}] to cf:{}",
              HexFormat.of().formatHex(key),
              HexFormat.of().formatHex(value),
              columnFamilyName);
        }
      } else {
        log.error(
            "Failed to put kv:[{}---{}] to cf:{}. reason:{}",
            HexFormat.of().formatHex(key),
            HexFormat.of().formatHex(value),
            columnFamilyName,
            errMsg);
      }
    }
  }

  @Override
  @SafeVarargs
  public final void putAll(String columnFamilyName, Tuple2<byte[], byte[]>... kvPairs)
      throws RocksDbException {
    validateKvPairs(kvPairs);
    validateOpenStatus();
    this.<Void>atomicBatchUpdate(
        writeBatch -> {
          MemorySegment columnFamilyHandle =
              columnFamilies.get(columnFamilyName).columnFamilyHandle();
          for (Tuple2<byte[], byte[]> pair : kvPairs) {
            writeBatch.put(
                columnFamilyHandle, pair.t1(), 0, pair.t1().length, pair.t2(), 0, pair.t2().length);
          }
          return null;
        });
  }

  @Override
  public void put(byte[] key, byte[] value) {
    put(DEFAULT_COLUMN_FAMILY_NAME, key, value);
  }

  @Override
  @Nullable
  public List<Tuple2<byte[], byte[]>> multiGet(byte[]... keys) {
    return multiGet(DEFAULT_COLUMN_FAMILY_NAME, keys);
  }

  @Override
  @Nullable
  public List<Tuple2<byte[], byte[]>> multiGet(String columnFamilyName, byte[]... keys) {
    validateColumnFamily(columnFamilyName);
    validateKeys(keys);
    validateOpenStatus();
    try (Arena arena = Arena.ofConfined()) {
      int size = keys.length;
      MemorySegment columnFamilyHandles = arena.allocateArray(C_POINTER, size);
      MemorySegment keysPtr = arena.allocateArray(C_POINTER, size);
      MemorySegment valuesPtr = arena.allocateArray(C_POINTER, size);
      MemorySegment keySizesPtr = arena.allocateArray(JAVA_LONG, size);
      MemorySegment valueSizesPtr = arena.allocateArray(JAVA_LONG, size);
      MemorySegment errPtr = newErrPtr(arena);
      for (int i = 0; i < size; i++) {
        columnFamilyHandles.setAtIndex(
            C_POINTER, i, columnFamilies.get(columnFamilyName).columnFamilyHandle());
        keysPtr.setAtIndex(C_POINTER, i, arena.allocateArray(C_CHAR, keys[i]));
        keySizesPtr.setAtIndex(JAVA_LONG, i, keys[i].length);
      }

      rocksdb_multi_get_cf(
          dbPtr,
          readOptionsPtr,
          columnFamilyHandles,
          size,
          keysPtr,
          keySizesPtr,
          valuesPtr,
          valueSizesPtr,
          errPtr);

      String errMsg = readErrMsgAndFree(errPtr);
      if (OK.equals(errMsg)) {
        List<Tuple2<byte[], byte[]>> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
          MemorySegment valuePtr =
              valuesPtr.getAtIndex(C_POINTER, i).reinterpret(arena, Utils::free);
          long length = valueSizesPtr.getAtIndex(JAVA_LONG, i);
          byte[] value = valuePtr.asSlice(0, length).toArray(C_CHAR);
          result.add(Tuple2.<byte[], byte[]>Tuple2Builder().t2(value).t1(keys[i]).build());
        }
        return result;
      } else {
        log.error(
            "Failed to get values by keys:{} from cf:{}. reason:{}",
            Arrays.stream(keys)
                .map(key -> HexFormat.of().formatHex(key))
                .collect(Collectors.joining(", ", "[", "]")),
            columnFamilyName,
            errMsg);
        return null;
      }
    }
  }

  @Override
  @Nullable
  public byte[] get(String columnFamilyName, byte[] key) {
    validateColumnFamily(columnFamilyName);
    validateKey(key);
    validateOpenStatus();
    try (Arena arena = Arena.ofConfined()) {
      MemorySegment keyPtr = arena.allocateArray(C_CHAR, key);
      MemorySegment errPtr = newErrPtr(arena);
      MemorySegment valLenPtr = arena.allocate(C_POINTER); // 出参，value长度
      MemorySegment valPtr =
          rocksdb_get(dbPtr, readOptionsPtr, keyPtr, keyPtr.byteSize(), valLenPtr, errPtr)
              .reinterpret(
                  arena, Utils::free); // 外部方法返回的指针都是global的，需要通过此方法关联大arena的scope，进行资源释放，避免出现内存泄露;
      String errMsg = readErrMsgAndFree(errPtr);
      if (OK.equals(errMsg)) {
        long valLen = valLenPtr.get(JAVA_LONG, 0);
        byte[] val = valLen != 0 ? valPtr.asSlice(0, valLen).toArray(JAVA_BYTE) : null;
        if (log.isDebugEnabled()) {
          log.debug(
              "Successfully get value:[{}---{}] from cf:{} by key:{}",
              valLen,
              val != null ? HexFormat.of().formatHex(val) : "",
              columnFamilyName,
              HexFormat.of().formatHex(key));
        }
        return val;
      } else {
        log.error(
            "Failed to get value from cf:{} by key:{}. reason:{}",
            columnFamilyName,
            HexFormat.of().formatHex(key),
            errMsg);
        return null;
      }
    }
  }

  @Override
  @Nullable
  public byte[] get(byte[] key) {
    return get(DEFAULT_COLUMN_FAMILY_NAME, key);
  }

  @Override
  public RocksDbIterator iterator() {
    return iterator(DEFAULT_COLUMN_FAMILY_NAME);
  }

  @Override
  public RocksDbIterator iterator(String columnFamilyName) {
    validateColumnFamily(columnFamilyName);
    validateOpenStatus();
    return new RocksDbIteratorImpl(
        rocksdb_create_iterator_cf(
            dbPtr, readOptionsPtr, columnFamilies.get(columnFamilyName).columnFamilyHandle()));
  }

  private void validateOpenStatus() {
    if (!isOpen()) {
      throw new IllegalStateException("rocksdb status abnormality.");
    }
  }

  @Nullable
  MemorySegment getColumnFamilyHandle(String columnFamilyName) {
    return columnFamilies.get(columnFamilyName).columnFamilyHandle();
  }

  /**
   * 列族是否存在
   *
   * @param columnFamilyName 列族名
   * @return true or false
   */
  boolean isColumnFamilyExist(String columnFamilyName) {
    if (isEmpty(columnFamilyName)) {
      throw new IllegalArgumentException("columnFamilyName must not be null or empty.");
    }
    return columnFamilies.containsKey(columnFamilyName);
  }

  void validateColumnFamily(String columnFamilyName) {
    if (!isColumnFamilyExist(columnFamilyName)) {
      throw new IllegalArgumentException(
          String.format("Column family %s does not exist.", columnFamilyName));
    }
  }

  /**
   * 原子更新操作
   *
   * @param action 操作
   * @return 结果
   * @param <R> 结果类型
   */
  public <R> R atomicBatchUpdate(@NonNull Function<WriteBatch, R> action) throws RocksDbException {
    try (Arena arena = Arena.ofConfined();
        WriteBatchImpl writeBatch = new WriteBatchImpl(arena)) {
      try {
        R result = action.apply(writeBatch); // 批量操作执行完毕后执行批量提交
        MemorySegment errPtr = newErrPtr(arena);
        rocksdb_write(dbPtr, dbOptionsPtr, writeBatch.writeBatch, errPtr);
        String errMsg = readErrMsgAndFree(errPtr);
        if (OK.equals(errMsg)) {
          if (log.isDebugEnabled()) {
            log.debug("Successfully completed batch update operation. action:{}", action);
          }
        } else {
          throw new RocksDbException(errMsg);
        }
        return result;
      } catch (Throwable t) {
        if (t instanceof RocksDbException e) {
          throw e;
        } else {
          throw new RocksDbException(t);
        }
      }
    }
  }
}
