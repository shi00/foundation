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
package com.silong.foundation.dj.scrapper;

import static org.rocksdb.CompressionType.LZ4_COMPRESSION;
import static org.rocksdb.CompressionType.ZSTD_COMPRESSION;
import static org.rocksdb.RocksDB.DEFAULT_COLUMN_FAMILY;

import com.silong.foundation.common.lambda.Tuple2;
import com.silong.foundation.dj.scrapper.configure.config.PersistStorageProperties;
import com.silong.foundation.dj.scrapper.exception.DataAccessException;
import com.silong.foundation.dj.scrapper.exception.InitializationException;
import com.silong.foundation.dj.scrapper.vo.KvPair;
import java.io.File;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.*;
import org.springframework.stereotype.Component;

/**
 * 基于RocksDB的持久化存储
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-10 17:52
 */
@Slf4j
@Component
class RocksDbPersistStorage implements PersistStorage, AutoCloseable, Serializable {

  @Serial private static final long serialVersionUID = -4_283_190_428_612_014_890L;

  /** 默认列族 */
  public static final String DEFAULT_COLUMN_FAMILY_NAME = "default";

  static {
    RocksDB.loadLibrary();
  }

  private final RocksDB rocksDB;

  private final ColumnFamilyOptions cfOpts;

  private final DBOptions options;

  private final Cache blockCache;

  private final Filter bloomFilter;

  private final RateLimiter rateLimiter;

  private final Statistics statistics;

  private final Map<String, ColumnFamilyHandle> columnFamilyHandlesMap = new ConcurrentHashMap<>();

  /**
   * 构造方法
   *
   * @param config 持久化存储配置
   */
  public RocksDbPersistStorage(PersistStorageProperties config) {
    validate(config == null, "config must not be null.");
    blockCache = new LRUCache(config.getBlockBaseTable().getCache().getBlockCacheCapacity());
    bloomFilter =
        new BloomFilter(
            config.getBlockBaseTable().getBloomFilter().getBloomFilterBitsPerKey(), false);
    rateLimiter =
        new RateLimiter(
            config.getRateLimiter().getRateBytesPerSecond(),
            config.getRateLimiter().getRefillPeriodMicros(),
            config.getRateLimiter().getFairness());

    cfOpts =
        new ColumnFamilyOptions()
            .setLevelCompactionDynamicLevelBytes(true)
            .setCompressionType(LZ4_COMPRESSION)
            .setBottommostCompressionType(ZSTD_COMPRESSION)
            .optimizeLevelStyleCompaction(config.getMemtableMemoryBudget())
            .setWriteBufferSize(config.getColumnFamilyWriteBufferSize())
            .setTableFormatConfig(
                new BlockBasedTableConfig()
                    .setBlockSize(config.getBlockBaseTable().getBlockSize())
                    .setCacheIndexAndFilterBlocks(true)
                    .setPinL0FilterAndIndexBlocksInCache(true)
                    .setFormatVersion(config.getBlockBaseTable().getFormatVersion())
                    .setBlockCache(blockCache)
                    .setOptimizeFiltersForMemory(true)
                    .setFilterPolicy(bloomFilter))
            .setMaxWriteBufferNumber(config.getMaxWriteBufferNumber());

    // list of column family descriptors, first entry must always be default column family
    ArrayList<ColumnFamilyDescriptor> columnFamilyDescriptors =
        new ArrayList<>(1 + config.getColumnFamilyNames().size());
    columnFamilyDescriptors.add(new ColumnFamilyDescriptor(DEFAULT_COLUMN_FAMILY, cfOpts));
    columnFamilyDescriptors.addAll(
        config.getColumnFamilyNames().stream()
            .map(name -> new ColumnFamilyDescriptor(name.getBytes(), cfOpts))
            .toList());

    options =
        new DBOptions()
            .setRateLimiter(rateLimiter)
            .setMaxBackgroundJobs(config.getMaxBackgroundJobs())
            .setBytesPerSync(config.getBytesPerSync())
            .setDbWriteBufferSize(config.getDbWriteBufferSize())
            .setCreateIfMissing(true)
            .setCreateMissingColumnFamilies(true);

    if (config.getStatistics().isEnable()) {
      statistics = new Statistics();
      statistics.setStatsLevel(config.getStatistics().getStatsLevel());
      options.setStatistics(statistics);
    } else {
      statistics = null;
    }

    List<ColumnFamilyHandle> columnFamilyHandles = new ArrayList<>();
    try {
      rocksDB =
          RocksDB.open(
              options,
              new File(config.getPersistDataPath()).getCanonicalPath(),
              columnFamilyDescriptors,
              columnFamilyHandles);
      for (ColumnFamilyHandle handle : columnFamilyHandles) {
        this.columnFamilyHandlesMap.put(new String(handle.getName()), handle);
      }
    } catch (Exception e) {
      throw new InitializationException(e);
    }
  }

  @Override
  public void close() {
    closeNativeResources(rocksDB);
    closeNativeResources(cfOpts);
    closeNativeResources(options);
    closeNativeResources(blockCache);
    closeNativeResources(bloomFilter);
    closeNativeResources(rateLimiter);
    closeNativeResources(statistics);
    columnFamilyHandlesMap.values().forEach(this::closeNativeResources);
    columnFamilyHandlesMap.clear();
  }

  private void closeNativeResources(AbstractImmutableNativeReference reference) {
    if (reference != null) {
      reference.close();
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
    try {
      ColumnFamilyHandle columnFamilyHandle = findColumnFamilyHandle(columnFamilyName);
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
    try (RocksIterator iterator = rocksDB.newIterator(findColumnFamilyHandle(columnFamilyName))) {
      for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
        consumer.accept(iterator.key(), iterator.value());
      }
    }
  }

  private ColumnFamilyHandle findColumnFamilyHandle(String columnFamilyName) {
    return columnFamilyHandlesMap.get(columnFamilyName);
  }

  private void validate(boolean invalid, String s) {
    if (invalid) {
      throw new IllegalArgumentException(s);
    }
  }

  private void validateColumnFamily(String columnFamilyName) {
    validate(isEmpty(columnFamilyName), "columnFamilyName must not be null or empty.");
    validate(
        !columnFamilyHandlesMap.containsKey(columnFamilyName),
        columnFamilyName + " does not exist in rocksdb.");
  }

  @Override
  public void deleteColumnFamily(String columnFamilyName) {
    validate(isEmpty(columnFamilyName), "columnFamilyName must not be null or empty.");
    try {
      ColumnFamilyHandle columnFamilyHandle = findColumnFamilyHandle(columnFamilyName);
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
  public void createColumnFamily(String columnFamilyName) {
    validate(isEmpty(columnFamilyName), "columnFamilyName must not be null or empty.");
    try {
      columnFamilyHandlesMap.putIfAbsent(
          columnFamilyName,
          rocksDB.createColumnFamily(
              new ColumnFamilyDescriptor(columnFamilyName.getBytes(), cfOpts)));
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
    try {
      ColumnFamilyHandle columnFamilyHandle = findColumnFamilyHandle(columnFamilyName);
      if (rocksDB.keyMayExist(columnFamilyHandle, key, null)) {
        rocksDB.delete(columnFamilyHandle, key);
      }
    } catch (RocksDBException e) {
      throw new DataAccessException(e);
    }
  }

  @Override
  public void multiRemove(List<byte[]> keys) {
    multiRemove(DEFAULT_COLUMN_FAMILY_NAME, keys);
  }

  @Override
  public void multiRemove(String columnFamilyName, List<byte[]> keys) {
    validate(keys == null, "keys must not be null.");
    multiRemoveAll(keys.stream().map(key -> new Tuple2<>(columnFamilyName, key)).toList());
  }

  @Override
  public void multiRemoveAll(List<Tuple2<String, byte[]>> keys) {
    validate(keys == null || keys.isEmpty(), "keys must not be null or empty.");
    try (WriteOptions writeOptions = new WriteOptions()) {
      try (WriteBatch batch = new WriteBatch()) {
        for (Tuple2<String, byte[]> tuple : keys) {
          byte[] key = tuple.t2();
          String columnFamilyName = tuple.t1();
          if (key == null || isEmpty(columnFamilyName)) {
            continue;
          }
          batch.delete(findColumnFamilyHandle(columnFamilyName), key);
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
    try {
      ColumnFamilyHandle columnFamilyHandle = findColumnFamilyHandle(columnFamilyName);
      // 使用布隆过滤器快速判断key是否存在
      return rocksDB.keyMayExist(columnFamilyHandle, key, null)
          ? rocksDB.get(columnFamilyHandle, key)
          : null;
    } catch (RocksDBException e) {
      throw new DataAccessException(e);
    }
  }

  @Override
  public List<KvPair<byte[], byte[]>> multiGet(List<byte[]> keys) {
    return multiGet(DEFAULT_COLUMN_FAMILY_NAME, keys);
  }

  @Override
  public List<KvPair<byte[], byte[]>> multiGet(String columnFamilyName, List<byte[]> keys) {
    validate(keys == null, "keys must not be null.");
    return multiGetAll(keys.stream().map(key -> new Tuple2<>(columnFamilyName, key)).toList())
        .stream()
        .map(kvPair -> new KvPair<>(kvPair.key().t2(), kvPair.value()))
        .toList();
  }

  @Override
  public List<KvPair<Tuple2<String, byte[]>, byte[]>> multiGetAll(
      List<Tuple2<String, byte[]>> keys) {
    validate(keys == null || keys.isEmpty(), "keys must not be null or empty.");
    List<ColumnFamilyHandle> columnFamilyHandles =
        keys.stream()
            .filter(t -> isNotEmpty(t.t1()))
            .map(t -> findColumnFamilyHandle(t.t1()))
            .toList();
    List<byte[]> keys1 = keys.stream().filter(t -> t.t2() != null).map(Tuple2::t2).toList();
    assert keys1.size() == columnFamilyHandles.size();
    try {
      List<byte[]> values = rocksDB.multiGetAsList(columnFamilyHandles, keys1);
      return IntStream.range(0, keys.size())
          .mapToObj(i -> new KvPair<>(keys.get(i), values.get(i)))
          .toList();
    } catch (RocksDBException e) {
      throw new DataAccessException(e);
    }
  }

  private ColumnFamilyHandle createColumnFamilyIfAbsent(String columnFamilyName) {
    ColumnFamilyHandle columnFamilyHandle = findColumnFamilyHandle(columnFamilyName);
    if (columnFamilyHandle == null) {
      createColumnFamily(columnFamilyName);
      columnFamilyHandle = findColumnFamilyHandle(columnFamilyName);
    }
    return columnFamilyHandle;
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
    try {
      rocksDB.put(createColumnFamilyIfAbsent(columnFamilyName), key, value);
    } catch (RocksDBException e) {
      throw new DataAccessException(e);
    }
  }

  @Override
  public void putAll(List<KvPair<byte[], byte[]>> kvPairs) {
    putAll(DEFAULT_COLUMN_FAMILY_NAME, kvPairs);
  }

  @Override
  public void putAll(String columnFamilyName, List<KvPair<byte[], byte[]>> kvPairs) {
    validate(kvPairs == null, "kvPairs must not be null.");
    putAllWith(kvPairs.stream().map(kvPair -> new Tuple2<>(columnFamilyName, kvPair)).toList());
  }

  @Override
  public void putAllWith(List<Tuple2<String, KvPair<byte[], byte[]>>> columnFamilyNameWithKvPairs) {
    validate(
        columnFamilyNameWithKvPairs == null || columnFamilyNameWithKvPairs.isEmpty(),
        "columnFamilyNameWithKvPairs must not be null or empty.");
    try (WriteOptions writeOptions = new WriteOptions()) {
      try (WriteBatch batch = new WriteBatch()) {
        for (Tuple2<String, KvPair<byte[], byte[]>> tuple : columnFamilyNameWithKvPairs) {
          String columnFamilyName = tuple.t1();
          KvPair<byte[], byte[]> kvPair = tuple.t2();
          byte[] key;
          byte[] value;
          if (columnFamilyName == null
              || kvPair == null
              || (key = kvPair.key()) == null
              || (value = kvPair.value()) == null) {
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

  private boolean isEmpty(String s) {
    return s == null || s.isEmpty();
  }

  private boolean isNotEmpty(String s) {
    return !isEmpty(s);
  }
}
