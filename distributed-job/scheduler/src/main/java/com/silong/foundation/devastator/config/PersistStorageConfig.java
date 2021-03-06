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
package com.silong.foundation.devastator.config;

import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import org.rocksdb.StatsLevel;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import static org.apache.commons.lang3.SystemUtils.getUserDir;
import static org.rocksdb.StatsLevel.ALL;
import static org.rocksdb.util.SizeUnit.KB;
import static org.rocksdb.util.SizeUnit.MB;

/**
 * Devastator持久化存储配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-15 22:12
 */
@Data
@Accessors(fluent = true)
public class PersistStorageConfig implements Serializable {

  @Serial private static final long serialVersionUID = 2589946416585154817L;

  /** 默认持久化数据保存路径 */
  public static final String DEFAULT_PERSIST_DATA_PATH =
      getUserDir().toPath().resolve("devastator-data").toFile().getAbsolutePath();

  /** 持久化数据保存路径 */
  @NotEmpty private String persistDataPath = DEFAULT_PERSIST_DATA_PATH;

  /** 列族名列表，不指定则只有default列族 */
  @NonNull @Valid private List<@NotEmpty String> columnFamilyNames = new LinkedList<>();

  /** memtable memory budget. Default: 32MB */
  @Positive private long memtableMemoryBudget = 32 * MB;

  /** 列族写入缓存大小，默认：32MB */
  @Positive private long columnFamilyWriteBufferSize = 32 * MB;

  /** db写入缓存大小，默认：128MB */
  @Positive private long dbWriteBufferSize = 128 * MB;

  /** 最大写缓存数量，默认：4 */
  @Positive private int maxWriteBufferNumber = 4;

  /** 最大后台任务数，默认：6 */
  @Positive private int maxBackgroundJobs = 6;

  /**
   * Allows OS to incrementally sync files to disk while they are being written, asynchronously, in
   * the background.Default: 1MB
   */
  @Positive private long bytesPerSync = MB;

  /** 性能统计配置 */
  @NotNull @Valid private StatisticsConfig statistics = new StatisticsConfig();

  /** RateLimiter配置 */
  @NotNull @Valid private RateLimiterConfig rateLimiter = new RateLimiterConfig();

  /** 块表配置 */
  @NotNull @Valid private BlockBaseTableConfig blockBaseTable = new BlockBaseTableConfig();

  /** 统计配置 */
  @Data
  @Accessors(fluent = true)
  public static class StatisticsConfig {
    /** 是否可开启存储统计，默认：不开启 */
    private boolean enable;

    /** 统计级别，默认：ALL */
    @NotNull private StatsLevel statsLevel = ALL;
  }

  /** RateLimiter配置 */
  @Data
  @Accessors(fluent = true)
  public static class RateLimiterConfig {
    /**
     * It controls the total write rate of compaction * and flush in bytes per second. Currently,
     * RocksDB does not enforce * rate limit for anything other than flush and compaction, e.g.
     * write to * WAL. The default value is 10000000.
     */
    @Positive private long rateBytesPerSecond = 10_000_000;

    /**
     * this controls how often tokens are refilled. For * example, * when rate_bytes_per_sec is set
     * to 10MB/s and refill_period_us is set to * 100ms, then 1MB is refilled every 100ms
     * internally. Larger value can * lead to burstier writes while smaller value introduces more
     * CPU * overhead. The default of 10ms should work for most cases.
     */
    @Positive private long refillPeriodMicros = 10_000;

    /**
     * RateLimiter accepts high-pri requests and low-pri requests. * A low-pri request is usually
     * blocked in favor of hi-pri request. * Currently, RocksDB assigns low-pri to request from
     * compaction and * high-pri to request from flush. Low-pri requests can get blocked if * flush
     * requests come in continuously. This fairness parameter grants * low-pri requests permission
     * by fairness chance even though high-pri * requests exist to avoid starvation. * You should be
     * good by leaving it at default 10.
     */
    @Positive private int fairness = 10;
  }

  /** BlockBasedTable配置 */
  @Data
  @Accessors(fluent = true)
  public static class BlockBaseTableConfig {

    /**
     * Approximate size of user data packed per block. Note that the block size specified here
     * corresponds to uncompressed data. The actual size of the unit read from disk may be smaller
     * if compression is enabled. This parameter can be changed dynamically. Default: 16KB
     */
    @Positive private long blockSize = 16 * KB;

    /** integer representing the version to be used. Default: 5 */
    @Positive private int formatVersion = 5;

    /** 布隆过滤器配置 */
    @NotNull @Valid private BloomFilterConfig bloomFilter = new BloomFilterConfig();

    /** 缓存配置 */
    @NotNull @Valid private CacheConfig cache = new CacheConfig();

    /** 布隆过滤器配置 */
    @Data
    @Accessors(fluent = true)
    public static class BloomFilterConfig {

      /** 布隆过滤器bit per key，默认：10.0 */
      @Positive private double bloomFilterBitsPerKey = 10.0;
    }

    /** 缓存配置 */
    @Data
    @Accessors(fluent = true)
    public static class CacheConfig {

      /** 块缓存容量 */
      @Positive private long blockCacheCapacity = 64 * MB;
    }
  }
}
