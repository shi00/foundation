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
package com.silong.foundation.devastator.core;

import com.silong.foundation.devastator.PersistStorage;
import com.silong.foundation.devastator.config.PersistStorageConfig;
import com.silong.foundation.devastator.exception.GeneralException;
import com.silong.foundation.devastator.utils.KvPair;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.rocksdb.*;

import java.io.File;
import java.io.Serial;
import java.util.*;
import java.util.stream.IntStream;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.rocksdb.CompressionType.LZ4_COMPRESSION;
import static org.rocksdb.CompressionType.ZSTD_COMPRESSION;
import static org.rocksdb.RocksDB.DEFAULT_COLUMN_FAMILY;

/**
 * 基于RocksDB的持久化存储
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-10 17:52
 */
@SuppressFBWarnings({"PATH_TRAVERSAL_IN"})
public class RocksDbPersistStorage implements PersistStorage {

  @Serial private static final long serialVersionUID = 0L;

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

  private final Map<String, ColumnFamilyHandle> columnFamilyHandlesMap = new HashMap<>();

  /**
   * 构造方法
   *
   * @param config 持久化存储配置
   */
  public RocksDbPersistStorage(PersistStorageConfig config) {
    validate(config == null, "config must not be null.");
    blockCache = new LRUCache(config.blockBaseTable().cache().blockCacheCapacity());
    bloomFilter =
        new BloomFilter(config.blockBaseTable().bloomFilter().bloomFilterBitsPerKey(), false);
    rateLimiter =
        new RateLimiter(
            config.rateLimiter().rateBytesPerSecond(),
            config.rateLimiter().refillPeriodMicros(),
            config.rateLimiter().fairness());

    cfOpts =
        new ColumnFamilyOptions()
            .setLevelCompactionDynamicLevelBytes(true)
            .setCompressionType(LZ4_COMPRESSION)
            .setBottommostCompressionType(ZSTD_COMPRESSION)
            .optimizeLevelStyleCompaction(config.memtableMemoryBudget())
            .setWriteBufferSize(config.columnFamilyWriteBufferSize())
            .setTableFormatConfig(
                new BlockBasedTableConfig()
                    .setBlockSize(config.blockBaseTable().blockSize())
                    .setCacheIndexAndFilterBlocks(true)
                    .setPinL0FilterAndIndexBlocksInCache(true)
                    .setFormatVersion(config.blockBaseTable().formatVersion())
                    .setBlockCache(blockCache)
                    .setOptimizeFiltersForMemory(true)
                    .setFilterPolicy(bloomFilter))
            .setMaxWriteBufferNumber(config.maxWriteBufferNumber());

    // list of column family descriptors, first entry must always be default column family
    ArrayList<ColumnFamilyDescriptor> columnFamilyDescriptors =
        new ArrayList<>(1 + config.columnFamilyNames().size());
    columnFamilyDescriptors.add(new ColumnFamilyDescriptor(DEFAULT_COLUMN_FAMILY, cfOpts));
    columnFamilyDescriptors.addAll(
        config.columnFamilyNames().stream()
            .map(name -> new ColumnFamilyDescriptor(name.getBytes(), cfOpts))
            .toList());
    options =
        new DBOptions()
            .setRateLimiter(rateLimiter)
            .setMaxBackgroundJobs(config.maxBackgroundJobs())
            .setBytesPerSync(config.bytesPerSync())
            .setDbWriteBufferSize(config.dbWriteBufferSize())
            .setCreateIfMissing(true)
            .setCreateMissingColumnFamilies(true);

    if (config.statistics().enable()) {
      statistics = new Statistics();
      statistics.setStatsLevel(config.statistics().statsLevel());
      options.setStatistics(statistics);
    } else {
      statistics = null;
    }

    List<ColumnFamilyHandle> columnFamilyHandles = new ArrayList<>();
    try {
      rocksDB =
          RocksDB.open(
              options,
              new File(config.persistDataPath()).getCanonicalPath(),
              columnFamilyDescriptors,
              columnFamilyHandles);
      for (ColumnFamilyHandle handle : columnFamilyHandles) {
        this.columnFamilyHandlesMap.put(new String(handle.getName()), handle);
      }
    } catch (Exception e) {
      throw new GeneralException(e);
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
  public void deleteRange(String columnFamilyName, byte[] startKey, byte[] endKey) {
    validateColumnFamily(columnFamilyName);
    validate(startKey == null, "startKey must not be null.");
    validate(endKey == null, "endKey must not be null.");
    try {
      ColumnFamilyHandle columnFamilyHandle = findColumnFamilyHandle(columnFamilyName);
      rocksDB.deleteRange(columnFamilyHandle, startKey, endKey);
    } catch (RocksDBException e) {
      throw new GeneralException(e);
    }
  }

  /**
   * 获取当前rocksdb中所有存在的列族名称列表
   *
   * @return 列族名称列表
   */
  public Collection<String> getAllColumnFamilyNames() {
    return columnFamilyHandlesMap.keySet();
  }

  private ColumnFamilyHandle findColumnFamilyHandle(String columnFamilyName) {
    return columnFamilyHandlesMap.get(columnFamilyName);
  }

  private void validate(boolean invalid, String s) {
    if (invalid) {
      throw new IllegalArgumentException(s);
    }
  }

  private void validateKeysContainsNullKey(Collection<byte[]> keys) {
    validate(keys.stream().anyMatch(Objects::isNull), "Keys cannot contain any null key.");
  }

  private void validateColumnFamily(String columnFamilyName) {
    validate(isEmpty(columnFamilyName), "columnFamilyName must not be null or empty.");
    validate(
        !columnFamilyHandlesMap.containsKey(columnFamilyName),
        columnFamilyName + " does not exist in rocksdb.");
  }

  @Override
  public void deleteColumnFamily(String columnFamilyName) {
    validateColumnFamily(columnFamilyName);
    try {
      ColumnFamilyHandle columnFamilyHandle = findColumnFamilyHandle(columnFamilyName);
      rocksDB.dropColumnFamily(columnFamilyHandle);
      columnFamilyHandlesMap.remove(columnFamilyName).close();
    } catch (RocksDBException e) {
      throw new GeneralException(e);
    }
  }

  @Override
  public void createColumnFamily(String columnFamilyName) {
    validate(isEmpty(columnFamilyName), "columnFamilyName must not be null or empty.");
    try {
      columnFamilyHandlesMap.put(
          columnFamilyName,
          rocksDB.createColumnFamily(
              new ColumnFamilyDescriptor(columnFamilyName.getBytes(), cfOpts)));
    } catch (RocksDBException e) {
      throw new GeneralException(e);
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
      throw new GeneralException(e);
    }
  }

  @Override
  public void multiRemove(List<byte[]> keys) {
    multiRemove(DEFAULT_COLUMN_FAMILY_NAME, keys);
  }

  @Override
  public void multiRemove(String columnFamilyName, List<byte[]> keys) {
    validateColumnFamily(columnFamilyName);
    validate(keys == null || keys.isEmpty(), "keys must not be null or empty.");
    validateKeysContainsNullKey(keys);
    try (WriteOptions writeOptions = new WriteOptions()) {
      try (WriteBatch batch = new WriteBatch()) {
        ColumnFamilyHandle columnFamilyHandle = findColumnFamilyHandle(columnFamilyName);
        for (byte[] key : keys) {
          batch.delete(columnFamilyHandle, key);
        }
        rocksDB.write(writeOptions, batch);
      }
    } catch (RocksDBException e) {
      throw new GeneralException(e);
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
      throw new GeneralException(e);
    }
  }

  @Override
  public List<KvPair<byte[], byte[]>> multiGet(List<byte[]> keys) {
    return multiGet(DEFAULT_COLUMN_FAMILY_NAME, keys);
  }

  @Override
  public List<KvPair<byte[], byte[]>> multiGet(String columnFamilyName, List<byte[]> keys) {
    validateColumnFamily(columnFamilyName);
    validate(keys == null || keys.isEmpty(), "keys must not be null or empty.");
    validateKeysContainsNullKey(keys);
    ColumnFamilyHandle columnFamilyHandle = findColumnFamilyHandle(columnFamilyName);
    List<ColumnFamilyHandle> handles =
        IntStream.range(0, keys.size()).mapToObj(i -> columnFamilyHandle).toList();
    try {
      List<byte[]> values = rocksDB.multiGetAsList(handles, keys);
      return IntStream.range(0, keys.size())
          .mapToObj(i -> new KvPair<>(keys.get(i), values.get(i)))
          .toList();
    } catch (RocksDBException e) {
      throw new GeneralException(e);
    }
  }

  @Override
  public void put(byte[] key, byte[] value) {
    put(DEFAULT_COLUMN_FAMILY_NAME, key, value);
  }

  @Override
  public void put(String columnFamilyName, byte[] key, byte[] value) {
    validate(key == null, "key must not be null.");
    validate(value == null, "value must not be null.");
    try {
      rocksDB.put(findColumnFamilyHandle(columnFamilyName), key, value);
    } catch (RocksDBException e) {
      throw new GeneralException(e);
    }
  }

  @Override
  public void putAll(List<KvPair<byte[], byte[]>> kvPairs) {
    putAll(DEFAULT_COLUMN_FAMILY_NAME, kvPairs);
  }

  @Override
  public void putAll(String columnFamilyName, List<KvPair<byte[], byte[]>> kvPairs) {
    validateColumnFamily(columnFamilyName);
    validate(kvPairs == null || kvPairs.isEmpty(), "kvPairs must not be null or empty.");
    validate(
        kvPairs.stream().anyMatch(KvPair::isKeyOrValueNull),
        "kvPairs cannot contain any null key or null value.");
    try (WriteOptions writeOptions = new WriteOptions()) {
      try (WriteBatch batch = new WriteBatch()) {
        ColumnFamilyHandle columnFamilyHandle = findColumnFamilyHandle(columnFamilyName);
        for (KvPair<byte[], byte[]> kvPair : kvPairs) {
          batch.put(columnFamilyHandle, kvPair.key(), kvPair.value());
        }
        rocksDB.write(writeOptions, batch);
      }
    } catch (RocksDBException e) {
      throw new GeneralException(e);
    }
  }

  @Override
  public void deleteRange(byte[] startKey, byte[] endKey) {
    deleteRange(DEFAULT_COLUMN_FAMILY_NAME, startKey, endKey);
  }
}
