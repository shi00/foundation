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

package com.silong.foundation.dj.longhaul;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.rocksdb.RocksDB.*;

import com.silong.foundation.common.lambda.Tuple2;
import com.silong.foundation.common.lambda.Tuple3;
import com.silong.foundation.dj.longhaul.config.PersistStorageProperties;
import com.silong.foundation.dj.longhaul.config.PersistStorageProperties.DataScale;
import com.silong.foundation.dj.longhaul.exception.DataAccessException;
import com.silong.foundation.dj.longhaul.exception.InitializationDBException;
import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.*;

/**
 * rocksdb实现的持久化存储
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-25 17:59
 */
@Slf4j
class RocksDbPersistStorage implements BasicPersistStorage, ObjectAccessor, Serializable {

  @Serial private static final long serialVersionUID = 7_801_457_952_536_771_210L;

  /** 线程名 */
  private static final String ROCKS_DB_GUARD = "RocksDB-Guard-Shutdown";

  /** 默认列族名称 */
  static final String DEFAULT_COLUMN_FAMILY_NAME = "default";

  static {
    loadLibrary();
  }

  /** 数据库实例 */
  private volatile RocksDB rocksDB;

  private volatile Exception initException;

  /** 列族配置 */
  private ColumnFamilyOptions cfOpts;

  private final CountDownLatch shutdownSignal = new CountDownLatch(1);

  private final CountDownLatch startedSignal = new CountDownLatch(1);

  private final Map<String, ColumnFamilyHandle> columnFamilyHandlesMap = new ConcurrentHashMap<>();

  /**
   * 构造方法
   *
   * @param properties 存储配置
   */
  public RocksDbPersistStorage(PersistStorageProperties properties) {
    this(properties, false);
  }

  /**
   * 构造方法
   *
   * @param properties 存储配置
   * @param isBlocking 是否阻塞当前线程
   */
  public RocksDbPersistStorage(@NonNull PersistStorageProperties properties, boolean isBlocking) {
    if (isBlocking) {
      openDB(properties);
    } else {
      Thread.ofVirtual().name(ROCKS_DB_GUARD).start(() -> openDB(properties));
      waitUtilStarted();
    }
  }

  /** 等待启动完毕 */
  private void waitUtilStarted() {
    try {
      startedSignal.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new InitializationDBException(e);
    }
    if (initException != null) {
      throw new InitializationDBException(initException);
    }
    assert rocksDB != null : "rocksDB must not be null.";
  }

  private void openDB(PersistStorageProperties properties) {
    // 此处按照数据量优化
    try (ColumnFamilyOptions cfOpts =
        this.cfOpts = buildColumnFamilyOptions(properties.getDataScale())) {
      // list of column family descriptors, first entry must always be default column family
      final List<ColumnFamilyDescriptor> cfDescriptors =
          buildColumnFamilyDescriptor(cfOpts, properties.getColumnFamilyNames());
      // a list which will hold the handles for the column families once the db is opened
      final LinkedList<ColumnFamilyHandle> columnFamilyHandleList = new LinkedList<>();
      try (DBOptions options =
              new DBOptions()
                  .setInfoLogLevel(properties.getInfoLogLevel())
                  .setCreateIfMissing(true)
                  .setCreateMissingColumnFamilies(true);
          RocksDB rocksDB =
              this.rocksDB =
                  RocksDB.open(
                      options,
                      properties.getPersistDataPath(),
                      cfDescriptors,
                      columnFamilyHandleList)) {
        try {
          // 缓存打开的列族
          while (!columnFamilyHandleList.isEmpty()) {
            if (!cacheColumnFamilyHandle(columnFamilyHandleList.poll())) {
              return;
            }
          }

          // 如果是非阻塞打开则通知等待线程
          if (isRocksDBGuardThread()) {
            startedSignal.countDown();
          }

          log.info("RocksDB has been opened successfully.");
          waitUtilShutdown();
        } finally {
          // NOTE frees the column family handles before freeing the db
          columnFamilyHandlesMap.forEach(
              (columnFamilyName, columnFamilyHandle) -> columnFamilyHandle.close());
          columnFamilyHandlesMap.clear();
          this.rocksDB = null;
          this.cfOpts = null;
        }
      } catch (RocksDBException e) {
        if (isRocksDBGuardThread()) {
          initException = e;
          startedSignal.countDown();
        } else {
          throw new InitializationDBException(e);
        }
      } finally {
        if (!columnFamilyHandleList.isEmpty()) {
          columnFamilyHandleList.forEach(AbstractNativeReference::close);
          columnFamilyHandleList.clear();
        }
        cfDescriptors.clear();
      }
    } finally {
      log.info("RocksDB has been shutdown gracefully.");
    }
  }

  private boolean cacheColumnFamilyHandle(ColumnFamilyHandle columnFamilyHandle) {
    try {
      columnFamilyHandlesMap.put(
          new String(columnFamilyHandle.getName(), UTF_8), columnFamilyHandle);
      return true;
    } catch (RocksDBException e) {
      if (isRocksDBGuardThread()) {
        initException = e;
        startedSignal.countDown();
        return false;
      } else {
        throw new InitializationDBException(e);
      }
    }
  }

  private boolean isRocksDBGuardThread() {
    return ROCKS_DB_GUARD.equals(Thread.currentThread().getName())
        && Thread.currentThread().isVirtual();
  }

  /** 等待shutdown信号 */
  private void waitUtilShutdown() {
    try {
      shutdownSignal.await();
    } catch (InterruptedException e) {
      Thread thread = Thread.currentThread();
      thread.interrupt(); // 设置线程中断状态
      if (isRocksDBGuardThread()) {
        initException = e;
        startedSignal.countDown();
      } else {
        throw new InitializationDBException(
            String.format("Thread(%s) is interrupted and RocksDB is closed.", thread.getName()));
      }
    }
  }

  private List<ColumnFamilyDescriptor> buildColumnFamilyDescriptor(
      ColumnFamilyOptions cfOpts, @Nullable Collection<String> columnFamilyNames) {
    ColumnFamilyDescriptor defaultCfd = new ColumnFamilyDescriptor(DEFAULT_COLUMN_FAMILY, cfOpts);
    if (columnFamilyNames == null || columnFamilyNames.isEmpty()) {
      ArrayList<ColumnFamilyDescriptor> list = new ArrayList<>(1);
      list.add(defaultCfd);
      return list;
    } else {
      boolean contains = columnFamilyNames.contains(DEFAULT_COLUMN_FAMILY_NAME);
      LinkedList<ColumnFamilyDescriptor> list =
          columnFamilyNames.stream()
              .distinct()
              .map(
                  columnFamilyName ->
                      new ColumnFamilyDescriptor(columnFamilyName.getBytes(UTF_8), cfOpts))
              .collect(Collectors.toCollection(LinkedList::new));
      if (!contains) {
        list.addFirst(defaultCfd);
      }
      return list;
    }
  }

  private ColumnFamilyOptions buildColumnFamilyOptions(DataScale scale) {
    final ColumnFamilyOptions columnFamilyOptions = new ColumnFamilyOptions();
    switch (scale) {
      case SMALL -> {
        return columnFamilyOptions.optimizeForSmallDb();
      }
      case MEDIUM, BIG, HUGE -> {
        return columnFamilyOptions.optimizeUniversalStyleCompaction();
      }
    }
    log.error("Unknown dataScale: " + scale);
    return columnFamilyOptions;
  }

  private boolean isEmpty(String s) {
    return s == null || s.isEmpty();
  }

  private boolean isNotEmpty(String s) {
    return !isEmpty(s);
  }

  private void checkOpenStatus() {
    if (!isOpen()) {
      throw new IllegalStateException("RocksDB is not in open state.");
    }
  }

  private void validate(boolean invalid, String s) {
    if (invalid) {
      throw new IllegalArgumentException(s);
    }
  }

  private ColumnFamilyHandle createColumnFamilyIfAbsent(String columnFamilyName) {
    ColumnFamilyHandle columnFamilyHandle = columnFamilyHandlesMap.get(columnFamilyName);
    if (columnFamilyHandle == null) {
      createColumnFamily(columnFamilyName);
      columnFamilyHandle = columnFamilyHandlesMap.get(columnFamilyName);
    }
    return columnFamilyHandle;
  }

  private void validateColumnFamily(String columnFamilyName) {
    validate(isEmpty(columnFamilyName), "columnFamilyName must not be null or empty.");
    validate(
        !columnFamilyHandlesMap.containsKey(columnFamilyName),
        columnFamilyName + " does not exist in RocksDB.");
  }

  /** 关闭数据库 */
  @Override
  public void close() {
    if (isOpen()) {
      shutdownSignal.countDown();
    }
  }

  @Override
  public boolean isOpen() {
    return rocksDB != null;
  }

  @Override
  public void createColumnFamily(String columnFamilyName) {
    validate(isEmpty(columnFamilyName), "columnFamilyName must not be null or empty.");
    checkOpenStatus();
    try {
      columnFamilyHandlesMap.putIfAbsent(
          columnFamilyName,
          rocksDB.createColumnFamily(
              new ColumnFamilyDescriptor(columnFamilyName.getBytes(UTF_8), cfOpts)));
    } catch (RocksDBException e) {
      throw new DataAccessException(e);
    }
  }

  @Override
  public void deleteColumnFamily(String columnFamilyName) {
    validate(isEmpty(columnFamilyName), "columnFamilyName must not be null or empty.");
    checkOpenStatus();
    try {
      ColumnFamilyHandle columnFamilyHandle = columnFamilyHandlesMap.get(columnFamilyName);
      if (columnFamilyHandle == null) {
        log.warn("{} does not exist and cannot be deleted.", columnFamilyName);
        return;
      }
      rocksDB.dropColumnFamily(columnFamilyHandle);
      columnFamilyHandlesMap.remove(columnFamilyName).close();
    } catch (RocksDBException e) {
      throw new DataAccessException(e);
    }
  }

  @Override
  public void remove(byte[] key) {
    remove(DEFAULT_COLUMN_FAMILY_NAME, key);
  }

  @Override
  public void remove(String columnFamilyName, byte[] key) {
    validateColumnFamily(columnFamilyName);
    validate(key == null, "key must not be null.");
    checkOpenStatus();
    try {
      ColumnFamilyHandle columnFamilyHandle = columnFamilyHandlesMap.get(columnFamilyName);
      // 先通过布隆过滤器确认Key是否存在，如果不存在则无需删除
      if (rocksDB.keyMayExist(columnFamilyHandle, key, null)) {
        rocksDB.delete(columnFamilyHandle, key);
      }
    } catch (RocksDBException e) {
      throw new DataAccessException(e);
    }
  }

  @Override
  public void multiRemove(byte[]... keys) {
    multiRemove(DEFAULT_COLUMN_FAMILY_NAME, keys);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void multiRemove(String columnFamilyName, byte[]... keys) {
    validate(keys == null, "keys must not be null.");
    multiRemoveAll(
        Stream.of(keys).map(key -> new Tuple2<>(columnFamilyName, key)).toArray(Tuple2[]::new));
  }

  @Override
  @SafeVarargs
  public final void multiRemoveAll(Tuple2<String, byte[]>... columnFamilyNameAndKeys) {
    validate(
        columnFamilyNameAndKeys == null || columnFamilyNameAndKeys.length == 0,
        "columnFamilyNameAndKeys must not be null or empty.");
    checkOpenStatus();
    try (WriteOptions writeOptions = new WriteOptions()) {
      try (WriteBatch batch = new WriteBatch()) {
        for (Tuple2<String, byte[]> pair : columnFamilyNameAndKeys) {
          String columnFamilyName = pair.t1();
          byte[] key = pair.t2();
          if (key == null
              || isEmpty(columnFamilyName)
              || !columnFamilyHandlesMap.containsKey(columnFamilyName)) {
            log.info(
                "Ignore ---> {}:{}",
                columnFamilyName,
                key != null ? HexFormat.of().formatHex(key) : "null");
            continue;
          }
          batch.delete(columnFamilyHandlesMap.get(columnFamilyName), key);
        }
        rocksDB.write(writeOptions, batch);
      }
    } catch (RocksDBException e) {
      throw new DataAccessException(e);
    }
  }

  @Override
  public byte[] get(byte[] key) {
    return get(DEFAULT_COLUMN_FAMILY_NAME, key);
  }

  @Override
  public byte[] get(String columnFamilyName, byte[] key) {
    validateColumnFamily(columnFamilyName);
    validate(key == null, "key must not be null.");
    checkOpenStatus();
    try {
      ColumnFamilyHandle columnFamilyHandle = columnFamilyHandlesMap.get(columnFamilyName);
      // 使用布隆过滤器快速判断key是否存在
      return rocksDB.keyMayExist(columnFamilyHandle, key, null)
          ? rocksDB.get(columnFamilyHandle, key)
          : null;
    } catch (RocksDBException e) {
      throw new DataAccessException(e);
    }
  }

  @Override
  public List<Tuple2<byte[], byte[]>> multiGet(byte[]... keys) {
    return multiGet(DEFAULT_COLUMN_FAMILY_NAME, keys);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Tuple2<byte[], byte[]>> multiGet(String columnFamilyName, byte[]... keys) {
    validate(keys == null, "keys must not be null.");
    Tuple2<String, byte[]>[] keyBytes =
        Stream.of(keys).map(key -> new Tuple2<>(columnFamilyName, key)).toArray(Tuple2[]::new);
    return multiGetAll(keyBytes).stream()
        .map(tuple2 -> new Tuple2<>(tuple2.t1().t2(), tuple2.t2()))
        .toList();
  }

  @Override
  @SafeVarargs
  public final List<Tuple2<Tuple2<String, byte[]>, byte[]>> multiGetAll(
      Tuple2<String, byte[]>... keys) {
    validate(keys == null || keys.length == 0, "keys must not be null or empty.");
    checkOpenStatus();
    List<Tuple3<String, byte[], ColumnFamilyHandle>> tuple3s =
        Stream.of(keys)
            .filter(
                t ->
                    isNotEmpty(t.t1())
                        && t.t2() != null
                        && columnFamilyHandlesMap.containsKey(t.t1()))
            .map(t -> new Tuple3<>(t.t1(), t.t2(), columnFamilyHandlesMap.get(t.t1())))
            .toList();
    try {
      List<byte[]> values =
          rocksDB.multiGetAsList(
              tuple3s.stream().map(Tuple3::t3).toList(), tuple3s.stream().map(Tuple2::t2).toList());
      return IntStream.range(0, tuple3s.size())
          .mapToObj(
              i -> {
                Tuple3<String, byte[], ColumnFamilyHandle> tuple3 = tuple3s.get(i);
                return new Tuple2<>(new Tuple2<>(tuple3.t1(), tuple3.t2()), values.get(i));
              })
          .toList();
    } catch (RocksDBException e) {
      throw new DataAccessException(e);
    }
  }

  @Override
  public void put(byte[] key, byte[] value) {
    put(DEFAULT_COLUMN_FAMILY_NAME, key, value);
  }

  @Override
  public void put(String columnFamilyName, byte[] key, byte[] value) {
    validate(isEmpty(columnFamilyName), "columnFamilyName must not be null or empty.");
    validate(key == null, "key must not be null.");
    validate(value == null, "value must not be null.");
    checkOpenStatus();
    try {
      rocksDB.put(createColumnFamilyIfAbsent(columnFamilyName), key, value);
    } catch (RocksDBException e) {
      throw new DataAccessException(e);
    }
  }

  @Override
  @SafeVarargs
  public final void putAll(Tuple2<byte[], byte[]>... kvPairs) {
    putAll(DEFAULT_COLUMN_FAMILY_NAME, kvPairs);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void putAll(String columnFamilyName, Tuple2<byte[], byte[]>... kvPairs) {
    validate(kvPairs == null, "kvPairs must not be null.");
    putAllWith(
        Stream.of(kvPairs)
            .map(kvPair -> new Tuple2<>(columnFamilyName, kvPair))
            .toArray(Tuple2[]::new));
  }

  @Override
  @SafeVarargs
  public final void putAllWith(
      Tuple2<String, Tuple2<byte[], byte[]>>... columnFamilyNameWithKvPairs) {
    validate(
        columnFamilyNameWithKvPairs == null || columnFamilyNameWithKvPairs.length == 0,
        "columnFamilyNameWithKvPairs must not be null or empty.");
    checkOpenStatus();
    try (WriteOptions writeOptions = new WriteOptions()) {
      try (WriteBatch batch = new WriteBatch()) {
        for (Tuple2<String, Tuple2<byte[], byte[]>> tuple : columnFamilyNameWithKvPairs) {
          String columnFamilyName = tuple.t1();
          Tuple2<byte[], byte[]> kvPair = tuple.t2();
          byte[] key;
          byte[] value;
          if (columnFamilyName == null
              || kvPair == null
              || (key = kvPair.t1()) == null
              || (value = kvPair.t2()) == null) {
            log.info(
                "Ignore --->{}|{}:{}",
                columnFamilyName,
                kvPair == null
                    ? "null"
                    : kvPair.t1() != null ? HexFormat.of().formatHex(kvPair.t1()) : "null",
                kvPair == null
                    ? "null"
                    : kvPair.t2() != null ? HexFormat.of().formatHex(kvPair.t2()) : "null");
            continue;
          }
          batch.put(createColumnFamilyIfAbsent(columnFamilyName), key, value);
        }
        rocksDB.write(writeOptions, batch);
      }
    } catch (RocksDBException e) {
      throw new DataAccessException(e);
    }
  }

  @Override
  public void deleteRange(byte[] startKey, byte[] endKey) {
    deleteRange(DEFAULT_COLUMN_FAMILY_NAME, startKey, endKey);
  }

  @Override
  public void deleteRange(String columnFamilyName, byte[] startKey, byte[] endKey) {
    validateColumnFamily(columnFamilyName);
    validate(startKey == null, "startKey must not be null.");
    validate(endKey == null, "endKey must not be null.");
    checkOpenStatus();
    try {
      ColumnFamilyHandle columnFamilyHandle = columnFamilyHandlesMap.get(columnFamilyName);
      rocksDB.deleteRange(columnFamilyHandle, startKey, endKey);
    } catch (RocksDBException e) {
      throw new DataAccessException(e);
    }
  }

  /**
   * 获取当前rocksdb中所有存在的列族名称列表
   *
   * @return 列族名称列表
   */
  @Override
  public Collection<String> getAllColumnFamilyNames() {
    return columnFamilyHandlesMap.keySet();
  }

  @Override
  public void iterate(BiConsumer<byte[], byte[]> consumer) {
    iterate(DEFAULT_COLUMN_FAMILY_NAME, consumer);
  }

  @Override
  public void iterate(String columnFamilyName, BiConsumer<byte[], byte[]> consumer) {
    validateColumnFamily(columnFamilyName);
    validate(consumer == null, "consumer must not be null.");
    checkOpenStatus();
    try (RocksIterator iterator =
        rocksDB.newIterator(columnFamilyHandlesMap.get(columnFamilyName))) {
      for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
        consumer.accept(iterator.key(), iterator.value());
      }
    }
  }
}
