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

package com.silong.foundation.rocksdbffm.options;

import static com.silong.foundation.rocksdbffm.Utils.*;
import static com.silong.foundation.rocksdbffm.enu.IOActivity.K_UNKNOWN;
import static com.silong.foundation.rocksdbffm.enu.IOPriority.IO_TOTAL;
import static com.silong.foundation.rocksdbffm.enu.ReadTier.K_READ_ALL_TIER;
import static com.silong.foundation.rocksdbffm.generated.RocksDB.*;
import static java.lang.foreign.MemoryLayout.paddingLayout;
import static java.lang.foreign.MemoryLayout.structLayout;
import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.*;

import com.silong.foundation.rocksdbffm.enu.IOActivity;
import com.silong.foundation.rocksdbffm.enu.IOPriority;
import com.silong.foundation.rocksdbffm.enu.ReadTier;
import java.io.Serial;
import java.io.Serializable;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.VarHandle;
import lombok.Data;
import lombok.NonNull;

/**
 * 读取配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-12-13 10:43
 */
@Data
public final class ReadOptions implements Options, Serializable {

  @Serial private static final long serialVersionUID = -3_531_065_411_779_519_195L;

  /** std::function 占位符实例 */
  private static final StdFunctionPlaceHolder TABLE_FILTER_PLACE_HOLDER =
      new StdFunctionPlaceHolder();

  private static final MemoryLayout LAYOUT =
      structLayout(
          C_POINTER.withName("snapshot"), // 8
          //          paddingLayout(1),
          C_POINTER.withName("timestamp"), // 16
          //          paddingLayout(1),
          C_POINTER.withName("iter_start_ts"), // 24
          //          paddingLayout(1),
          JAVA_LONG.withName("deadline"), // 32
          //          paddingLayout(1),
          JAVA_LONG.withName("io_timeout"), // 40
          //          paddingLayout(1),
          JAVA_INT.withName("read_tier"), // 44
          //          paddingLayout(2),
          JAVA_INT.withName("rate_limiter_priority"), // 48
          //          paddingLayout(4),
          uint64_t.withName("value_size_soft_limit"), // 56
          JAVA_BOOLEAN.withName("verify_checksums"),
          JAVA_BOOLEAN.withName("fill_cache"),
          JAVA_BOOLEAN.withName("ignore_range_deletions"),
          JAVA_BOOLEAN.withName("async_io"),
          JAVA_BOOLEAN.withName("optimize_multiget_for_io"),
          paddingLayout(3), // 64
          JAVA_LONG.withName("readahead_size"), // 72
          uint64_t.withName("max_skippable_internal_keys"), // 80
          C_POINTER.withName("iterate_lower_bound"), // 88
          C_POINTER.withName("iterate_upper_bound"), // 96
          JAVA_BOOLEAN.withName("tailing"),
          JAVA_BOOLEAN.withName("managed"),
          JAVA_BOOLEAN.withName("total_order_seek"),
          JAVA_BOOLEAN.withName("auto_prefix_mode"),
          JAVA_BOOLEAN.withName("prefix_same_as_start"),
          JAVA_BOOLEAN.withName("pin_data"),
          JAVA_BOOLEAN.withName("adaptive_readahead"),
          JAVA_BOOLEAN.withName("background_purge_on_iterator_cleanup"), // 104
          TABLE_FILTER_PLACE_HOLDER.layout().withName("table_filter"), // 112
          uint8_t.withName("io_activity"),
          paddingLayout(7));

  private static final VarHandle SNAPSHOT = LAYOUT.varHandle(PathElement.groupElement("snapshot"));

  private static final VarHandle TIMESTAMP =
      LAYOUT.varHandle(PathElement.groupElement("timestamp"));

  private static final VarHandle ITER_START_TS =
      LAYOUT.varHandle(PathElement.groupElement("iter_start_ts"));

  private static final VarHandle DEAD_LINE = LAYOUT.varHandle(PathElement.groupElement("deadline"));

  private static final VarHandle IO_TIMEOUT =
      LAYOUT.varHandle(PathElement.groupElement("io_timeout"));

  private static final VarHandle READ_TIER =
      LAYOUT.varHandle(PathElement.groupElement("read_tier"));

  private static final VarHandle RATE_LIMITER_PRIORITY =
      LAYOUT.varHandle(PathElement.groupElement("rate_limiter_priority"));

  private static final VarHandle VALUE_SIZE_SOFT_LIMIT =
      LAYOUT.varHandle(PathElement.groupElement("value_size_soft_limit"));

  private static final VarHandle VERIFY_CHECKSUMS =
      LAYOUT.varHandle(PathElement.groupElement("verify_checksums"));

  private static final VarHandle FILL_CACHE =
      LAYOUT.varHandle(PathElement.groupElement("fill_cache"));

  private static final VarHandle IGNORE_RANGE_DELETIONS =
      LAYOUT.varHandle(PathElement.groupElement("ignore_range_deletions"));

  private static final VarHandle ASYNC_IO = LAYOUT.varHandle(PathElement.groupElement("async_io"));

  private static final VarHandle OPTIMIZE_MULTIGET_FOR_IO =
      LAYOUT.varHandle(PathElement.groupElement("optimize_multiget_for_io"));

  private static final VarHandle READAHEAD_SIZE =
      LAYOUT.varHandle(PathElement.groupElement("readahead_size"));

  private static final VarHandle MAX_SKIPPABLE_INTERNAL_KEYS =
      LAYOUT.varHandle(PathElement.groupElement("max_skippable_internal_keys"));

  private static final VarHandle ITERATE_LOWER_BOUND =
      LAYOUT.varHandle(PathElement.groupElement("iterate_lower_bound"));

  private static final VarHandle ITERATE_UPPER_BOUND =
      LAYOUT.varHandle(PathElement.groupElement("iterate_upper_bound"));

  private static final VarHandle TAILING = LAYOUT.varHandle(PathElement.groupElement("tailing"));

  private static final VarHandle MANAGED = LAYOUT.varHandle(PathElement.groupElement("managed"));

  private static final VarHandle TOTAL_ORDER_SEEK =
      LAYOUT.varHandle(PathElement.groupElement("total_order_seek"));

  private static final VarHandle AUTO_PREFIX_MODE =
      LAYOUT.varHandle(PathElement.groupElement("auto_prefix_mode"));

  private static final VarHandle PREFIX_SAME_AS_START =
      LAYOUT.varHandle(PathElement.groupElement("prefix_same_as_start"));

  private static final VarHandle PIN_DATA = LAYOUT.varHandle(PathElement.groupElement("pin_data"));

  private static final VarHandle ADAPTIVE_READAHEAD =
      LAYOUT.varHandle(PathElement.groupElement("adaptive_readahead"));

  private static final VarHandle BACKGROUND_PURGE_ON_ITERATOR_CLEANUP =
      LAYOUT.varHandle(PathElement.groupElement("background_purge_on_iterator_cleanup"));

  //  private static final VarHandle TABLE_FILTER =
  //      LAYOUT.varHandle(PathElement.groupElement("table_filter"));

  private static final VarHandle IO_ACTIVITY =
      LAYOUT.varHandle(PathElement.groupElement("io_activity"));

  /**
   * If "snapshot" is non-nullptr, read as of the supplied snapshot (which must belong to the DB
   * that is being read and which must not have been released). If "snapshot" is nullptr, use an
   * implicit snapshot of the state at the beginning of this read operation.
   */
  private MemorySegment snapshot = NULL;

  /**
   * Timestamp of operation. Read should return the latest data visible to the specified timestamp.
   * All timestamps of the same database must be of the same length and format. The user is
   * responsible for providing a customized compare function via Comparator to order <key,
   * timestamp> tuples. For iterator, iter_start_ts is the lower bound (older) and timestamp serves
   * as the upper bound. Versions of the same record that fall in the timestamp range will be
   * returned. If iter_start_ts is nullptr, only the most recent version visible to timestamp is
   * returned. The user-specified timestamp feature is still under active development, and the API
   * is subject to change.
   */
  private MemorySegment timestamp = NULL;

  /**
   * If true and if user is trying to write to column families that don't exist (they were dropped),
   * ignore the write (don't return an error). If there are multiple writes in a WriteBatch, other
   * writes will succeed. Default: false
   */
  private MemorySegment iterStartTs = NULL;

  /**
   * Deadline for completing an API call (Get/MultiGet/Seek/Next for now) in microseconds. It should
   * be set to microseconds since epoch, i.e, gettimeofday or equivalent plus allowed duration in
   * microseconds. The best way is to use env->NowMicros() + some timeout. This is best efforts. The
   * call may exceed the deadline if there is IO involved and the file system doesn't support
   * deadlines, or due to checking for deadline periodically rather than for every key if processing
   * a batch. default:std::chrono::microseconds::zero()
   */
  private long deadline;

  /**
   * A timeout in microseconds to be passed to the underlying FileSystem for reads. As opposed to
   * deadline, this determines the timeout for each individual file read request. If a
   * MultiGet/Get/Seek/Next etc call results in multiple reads, each read can last up to io_timeout
   * us.default:std::chrono::microseconds::zero()
   */
  private long ioTimeout;

  /**
   * Specify if this read request should process data that ALREADY resides on a particular cache. If
   * the required data is not found at the specified cache, then Status::Incomplete is returned.
   */
  private ReadTier readTier = K_READ_ALL_TIER;

  /**
   * For writes associated with this option, charge the internal rate limiter (see
   * `DBOptions::rate_limiter`) at the specified priority. The special value `Env::IO_TOTAL`
   * disables charging the rate limiter.
   *
   * <p>Currently the support covers automatic WAL flushes, which happen during live updates
   * (`Put()`, `Write()`, `Delete()`, etc.) when `WriteOptions::disableWAL == false` and
   * `DBOptions::manual_wal_flush == false`.
   *
   * <p>Only `Env::IO_USER` and `Env::IO_TOTAL` are allowed due to implementation constraints.
   *
   * <p>Default: `Env::IO_TOTAL`
   */
  private IOPriority rateLimiterPriority = IO_TOTAL;

  /**
   * It limits the maximum cumulative value size of the keys in batch while reading through
   * MultiGet. Once the cumulative value size exceeds this soft limit then all the remaining keys
   * are returned with status Aborted. Default:std::numeric_limits<uint64_t>::max().
   *
   * <p>rocksdb源码中使用uint64_t，java中使用long表示，因为初始值为max，所以java侧初始值为-1
   */
  private long valueSizeSoftLimit = -1L;

  /**
   * If true, all data read from underlying storage will be verified against corresponding
   * checksums.
   */
  private boolean verifyChecksums = true;

  /**
   * Should the "data block"/"index block" read for this iteration be placed in block cache? Callers
   * may wish to set this field to false for bulk scans. This would help not to the change eviction
   * order of existing items in the block cache.
   */
  private boolean fillCache = true;

  /**
   * If true, range tombstones handling will be skipped in key lookup paths. For DB instances that
   * don't use DeleteRange() calls, this setting can be used to optimize the read performance. Note
   * that, if this assumption (of no previous DeleteRange() calls) is broken, stale keys could be
   * served in read paths.
   */
  private boolean ignoreRangeDeletions = false;

  /**
   * Experimental: If async_io is enabled, RocksDB will prefetch some of data asynchronously.
   * RocksDB apply it if reads are sequential and its internal automatic prefetching.
   */
  private boolean asyncIO = false;

  /**
   * Experimental: If async_io is set, then this flag controls whether we read SST files in multiple
   * levels asynchronously. Enabling this flag can help reduce MultiGet latency by maximizing the
   * number of SST files read in parallel if the keys in the MultiGet batch are in different levels.
   * It comes at the expense of slightly higher CPU overhead.
   */
  private boolean optimizeMultiGetForIO = true;

  /**
   * RocksDB does auto-readahead for iterators on noticing more than two reads for a table file. The
   * readahead starts at 8KB and doubles on every additional read up to 256KB. This option can help
   * if most of the range scans are large, and if it is determined that a larger readahead than that
   * enabled by auto-readahead is needed. Using a large readahead size (> 2MB) can typically improve
   * the performance of forward iteration on spinning disks.
   */
  private long readAheadSize = 0;

  /**
   * A threshold for the number of keys that can be skipped before failing an iterator seek as
   * incomplete. The default value of 0 should be used to never fail a request as incomplete, even
   * on skipping too many keys.
   *
   * <p>rocksdb源码中使用uint64_t，java中使用long表示
   */
  private long maxSkippableInternalKeys = 0;

  /**
   * `iterate_lower_bound` defines the smallest key at which the backward iterator can return an
   * entry. Once the bound is passed, Valid() will be false. `iterate_lower_bound` is inclusive ie
   * the bound value is a valid entry. If prefix_extractor is not null, the Seek target and
   * `iterate_lower_bound` need to have the same prefix. This is because ordering is not guaranteed
   * outside of prefix domain. In case of user_defined timestamp, if enabled, iterate_lower_bound
   * should point to key without timestamp part.
   */
  private MemorySegment iterateLowerBound = NULL;

  /**
   * "iterate_upper_bound" defines the extent up to which the forward iterator can return entries.
   * Once the bound is reached, Valid() will be false. "iterate_upper_bound" is exclusive ie the
   * bound value is not a valid entry. If prefix_extractor is not null: 1. If
   * options.auto_prefix_mode = true, iterate_upper_bound will be used to infer whether prefix
   * iterating (e.g. applying prefix bloom filter) can be used within RocksDB. This is done by
   * comparing iterate_upper_bound with the seek key. 2. If options.auto_prefix_mode = false,
   * iterate_upper_bound only takes effect if it shares the same prefix as the seek key. If
   * iterate_upper_bound is outside the prefix of the seek key, then keys returned outside the
   * prefix range will be undefined, just as if iterate_upper_bound = null. If iterate_upper_bound
   * is not null, SeekToLast() will position the iterator at the first key smaller than
   * iterate_upper_bound. // In case of user_defined timestamp, if enabled, iterate_upper_bound
   * should point to key without timestamp part.
   */
  private MemorySegment iterateUpperBound = NULL;

  /**
   * Specify to create a tailing iterator -- a special iterator that has a view of the complete
   * database (i.e. it can also be used to read newly added data) and is optimized for sequential
   * reads. It will return records that were inserted into the database after the creation of the
   * iterator.
   */
  private boolean tailing = false;

  /**
   * This options is not used anymore. It was to turn on a functionality that has been removed.
   * DEPRECATED
   */
  @Deprecated private boolean managed = false;

  /**
   * Enable a total order seek regardless of index format (e.g. hash index) used in the table. Some
   * table format (e.g. plain table) may not support this option. If true when calling Get(), we
   * also skip prefix bloom when reading from block based table, which only affects Get()
   * performance.
   */
  private boolean totalOrderSeek = false;

  /**
   * When true, by default use total_order_seek = true, and RocksDB can selectively enable prefix
   * seek mode if won't generate a different result from total_order_seek, based on seek key, and
   * iterator upper bound. BUG: Using Comparator::IsSameLengthImmediateSuccessor and
   * SliceTransform::FullLengthEnabled to enable prefix mode in cases where prefix of upper bound
   * differs from prefix of seek key has a flaw. If present in the DB, "short keys" (shorter than
   * "full length" prefix) can be omitted from auto_prefix_mode iteration when they would be present
   * in total_order_seek iteration, regardless of whether the short keys are "in domain" of the
   * prefix extractor. This is not an issue if no short keys are added to DB or are not expected to
   * be returned by such iterators. (We are also assuming the new condition on
   * IsSameLengthImmediateSuccessor is satisfied; see its BUG section). A bug example is in
   * DBTest2::AutoPrefixMode1, search for "BUG".
   */
  private boolean autoPrefixMode = false;

  /**
   * Enforce that the iterator only iterates over the same prefix as the seek. This option is
   * effective only for prefix seeks, i.e. prefix_extractor is non-null for the column family and
   * total_order_seek is false. Unlike iterate_upper_bound, prefix_same_as_start only works within a
   * prefix but in both directions.
   */
  private boolean prefixSameAsStart = false;

  /**
   * Keep the blocks loaded by the iterator pinned in memory as long as the iterator is not deleted,
   * If used when reading from tables created with BlockBasedTableOptions::use_delta_encoding =
   * false, Iterator's property "rocksdb.iterator.is-key-pinned" is guaranteed to return 1.
   */
  private boolean pinData = false;

  /**
   * For iterators, RocksDB does auto-readahead on noticing more than two sequential reads for a
   * table file if user doesn't provide readahead_size. The readahead starts at 8KB and doubles on
   * every additional read upto max_auto_readahead_size only when reads are sequential. However at
   * each level, if iterator moves over next file, readahead_size starts again from 8KB. By enabling
   * this option, RocksDB will do some enhancements for prefetching the data.
   */
  private boolean adaptiveReadAhead = false;

  /**
   * If true, when PurgeObsoleteFile is called in CleanupIteratorState, we schedule a background job
   * in the flush job queue and delete obsolete files in background.
   */
  private boolean backgroundPurgeOnIteratorCleanup = false;

  /**
   * A callback to determine whether relevant keys for this scan exist in a given table based on the
   * table's properties. The callback is passed the properties of each table during iteration. If
   * the callback returns false, the table will not be scanned. This option only affects Iterators
   * and has no impact on point lookups. Default: empty (every table will be scanned).
   */
  private PlaceHolder tableFilter = TABLE_FILTER_PLACE_HOLDER;

  /** For RocksDB internal use only */
  private IOActivity ioActivity = K_UNKNOWN;

  @Override
  public ReadOptions from(@NonNull MemorySegment readOptions) {
    this.adaptiveReadAhead = adaptiveReadAhead(readOptions);
    this.autoPrefixMode = autoPrefixMode(readOptions);
    //    this.tableFilter = tableFilter(readOptions);
    this.managed = managed(readOptions);
    this.fillCache = fillCache(readOptions);
    this.asyncIO = asyncIO(readOptions);
    this.backgroundPurgeOnIteratorCleanup = backgroundPurgeOnIteratorCleanup(readOptions);
    this.deadline = deadline(readOptions);
    this.ioTimeout = ioTimeout(readOptions);
    this.ignoreRangeDeletions = ignoreRangeDeletions(readOptions);
    this.ioActivity = ioActivity(readOptions);
    this.iterateLowerBound = iterateLowerBound(readOptions);
    this.iterateUpperBound = iterateUpperBound(readOptions);
    this.rateLimiterPriority = rateLimiterPriority(readOptions);
    this.optimizeMultiGetForIO = optimizeMultiGetForIO(readOptions);
    this.maxSkippableInternalKeys = maxSkippableInternalKeys(readOptions);
    this.pinData = pinData(readOptions);
    this.tailing = tailing(readOptions);
    this.totalOrderSeek = totalOrderSeek(readOptions);
    this.readTier = readTier(readOptions);
    this.prefixSameAsStart = prefixSameAsStart(readOptions);
    this.timestamp = timestamp(readOptions);
    this.iterStartTs = iterStartTs(readOptions);
    this.readAheadSize = readAheadSize(readOptions);
    this.snapshot = snapshot(readOptions);
    this.valueSizeSoftLimit = valueSizeSoftLimit(readOptions);
    this.verifyChecksums = verifyChecksums(readOptions);
    return this;
  }

  /**
   * 分配的native options必须由调用方自行释放内存
   *
   * @return native options
   */
  @Override
  public MemorySegment to() {
    MemorySegment readOptions = rocksdb_readoptions_create();
    verifyChecksums(this.verifyChecksums, readOptions);
    valueSizeSoftLimit(this.valueSizeSoftLimit, readOptions);
    snapshot(this.snapshot, readOptions);
    readAheadSize(this.readAheadSize, readOptions);
    iterStartTs(this.iterStartTs, readOptions);
    timestamp(this.timestamp, readOptions);
    prefixSameAsStart(this.prefixSameAsStart, readOptions);
    readTier(this.readTier, readOptions);
    totalOrderSeek(this.totalOrderSeek, readOptions);
    tailing(this.tailing, readOptions);
    pinData(this.pinData, readOptions);
    maxSkippableInternalKeys(this.maxSkippableInternalKeys, readOptions);
    optimizeMultiGetForIO(this.optimizeMultiGetForIO, readOptions);
    rateLimiterPriority(this.rateLimiterPriority, readOptions);
    iterateUpperBound(this.iterateUpperBound, readOptions);
    iterateLowerBound(this.iterateLowerBound, readOptions);
    ioActivity(this.ioActivity, readOptions);
    ignoreRangeDeletions(this.ignoreRangeDeletions, readOptions);
    ioTimeout(this.ioTimeout, readOptions);
    deadline(this.deadline, readOptions);
    backgroundPurgeOnIteratorCleanup(this.backgroundPurgeOnIteratorCleanup, readOptions);
    asyncIO(this.asyncIO, readOptions);
    fillCache(this.fillCache, readOptions);
    managed(this.managed, readOptions);
    //    tableFilter(this.tableFilter, readOptions);
    autoPrefixMode(this.autoPrefixMode, readOptions);
    adaptiveReadAhead(this.adaptiveReadAhead, readOptions);
    return readOptions;
  }

  @Override
  public MemoryLayout layout() {
    return LAYOUT;
  }

  public static String toString(MemorySegment readOptions) {
    return String.format(
        "readOptions:[snapshot:%s, timestamp:%s, iter_start_ts:%b, deadline:%d, io_timeout:%d, read_tier:%s, rate_limiter_priority:%s, value_size_soft_limit:%s, verify_checksums=%b, fill_cache=%b, ignore_range_deletions=%b, async_io=%b, optimize_multiget_for_io=%b, readahead_size:%d, max_skippable_internal_keys:%d, iterate_lower_bound=%s, iterate_upper_bound=%s, tailing=%b, managed=%b, total_order_seek=%b, auto_prefix_mode=%b, prefix_same_as_start=%b, pin_data=%b, adaptive_readahead=%b, background_purge_on_iterator_cleanup=%b, table_filter=%s, io_activity=%s]",
        snapshot(readOptions).equals(NULL) ? "nullptr" : snapshot(readOptions),
        timestamp(readOptions).equals(NULL) ? "nullptr" : timestamp(readOptions),
        iterStartTs(readOptions).equals(NULL) ? "nullptr" : iterStartTs(readOptions),
        deadline(readOptions),
        ioTimeout(readOptions),
        readTier(readOptions),
        rateLimiterPriority(readOptions),
        Long.toUnsignedString(valueSizeSoftLimit(readOptions)),
        verifyChecksums(readOptions),
        fillCache(readOptions),
        ignoreRangeDeletions(readOptions),
        asyncIO(readOptions),
        optimizeMultiGetForIO(readOptions),
        readAheadSize(readOptions),
        maxSkippableInternalKeys(readOptions),
        iterateLowerBound(readOptions).equals(NULL) ? "nullptr" : iterateLowerBound(readOptions),
        iterateUpperBound(readOptions).equals(NULL) ? "nullptr" : iterateUpperBound(readOptions),
        tailing(readOptions),
        managed(readOptions),
        totalOrderSeek(readOptions),
        autoPrefixMode(readOptions),
        prefixSameAsStart(readOptions),
        pinData(readOptions),
        adaptiveReadAhead(readOptions),
        backgroundPurgeOnIteratorCleanup(readOptions),
        TABLE_FILTER_PLACE_HOLDER,
        ioActivity(readOptions));
  }

  public static MemorySegment rateLimiterPriority(
      @NonNull IOPriority ioPriority, @NonNull MemorySegment readOptions) {
    RATE_LIMITER_PRIORITY.set(readOptions, ioPriority.ordinal());
    return readOptions;
  }

  public static IOPriority rateLimiterPriority(@NonNull MemorySegment writeOptions) {
    return enumType((int) RATE_LIMITER_PRIORITY.get(writeOptions), IOPriority.class);
  }

  public static MemorySegment prefixSameAsStart(
      boolean prefixSameAsStart, @NonNull MemorySegment readOptions) {
    PREFIX_SAME_AS_START.set(readOptions, prefixSameAsStart);
    return readOptions;
  }

  public static boolean prefixSameAsStart(@NonNull MemorySegment readOptions) {
    return (boolean) PREFIX_SAME_AS_START.get(readOptions);
  }

  public static MemorySegment pinData(boolean pinData, @NonNull MemorySegment readOptions) {
    PIN_DATA.set(readOptions, pinData);
    return readOptions;
  }

  public static boolean pinData(@NonNull MemorySegment readOptions) {
    return (boolean) PIN_DATA.get(readOptions);
  }

  public static MemorySegment backgroundPurgeOnIteratorCleanup(
      boolean backgroundPurgeOnIteratorCleanup, @NonNull MemorySegment readOptions) {
    BACKGROUND_PURGE_ON_ITERATOR_CLEANUP.set(readOptions, backgroundPurgeOnIteratorCleanup);
    return readOptions;
  }

  public static boolean backgroundPurgeOnIteratorCleanup(@NonNull MemorySegment readOptions) {
    return (boolean) BACKGROUND_PURGE_ON_ITERATOR_CLEANUP.get(readOptions);
  }

  public static MemorySegment adaptiveReadAhead(
      boolean adaptiveReadAhead, @NonNull MemorySegment readOptions) {
    ADAPTIVE_READAHEAD.set(readOptions, adaptiveReadAhead);
    return readOptions;
  }

  public static boolean adaptiveReadAhead(@NonNull MemorySegment readOptions) {
    return (boolean) ADAPTIVE_READAHEAD.get(readOptions);
  }

  public static MemorySegment autoPrefixMode(
      boolean autoPrefixMode, @NonNull MemorySegment readOptions) {
    AUTO_PREFIX_MODE.set(readOptions, autoPrefixMode);
    return readOptions;
  }

  public static boolean autoPrefixMode(@NonNull MemorySegment readOptions) {
    return (boolean) AUTO_PREFIX_MODE.get(readOptions);
  }

  public static MemorySegment totalOrderSeek(
      boolean totalOrderSeek, @NonNull MemorySegment readOptions) {
    TOTAL_ORDER_SEEK.set(readOptions, totalOrderSeek);
    return readOptions;
  }

  public static boolean totalOrderSeek(@NonNull MemorySegment readOptions) {
    return (boolean) TOTAL_ORDER_SEEK.get(readOptions);
  }

  public static MemorySegment tailing(boolean tailing, @NonNull MemorySegment readOptions) {
    TAILING.set(readOptions, tailing);
    return readOptions;
  }

  public static boolean tailing(@NonNull MemorySegment readOptions) {
    return (boolean) TAILING.get(readOptions);
  }

  public static MemorySegment maxSkippableInternalKeys(
      long maxSkippableInternalKeys, @NonNull MemorySegment readOptions) {
    MAX_SKIPPABLE_INTERNAL_KEYS.set(readOptions, maxSkippableInternalKeys);
    return readOptions;
  }

  public static long maxSkippableInternalKeys(@NonNull MemorySegment readOptions) {
    return (long) MAX_SKIPPABLE_INTERNAL_KEYS.get(readOptions);
  }

  public static MemorySegment readAheadSize(
      long readAheadSize, @NonNull MemorySegment readOptions) {
    READAHEAD_SIZE.set(readOptions, readAheadSize);
    return readOptions;
  }

  public static long readAheadSize(@NonNull MemorySegment readOptions) {
    return (long) READAHEAD_SIZE.get(readOptions);
  }

  public static MemorySegment optimizeMultiGetForIO(
      boolean optimizeMultiGetForIO, @NonNull MemorySegment readOptions) {
    OPTIMIZE_MULTIGET_FOR_IO.set(readOptions, optimizeMultiGetForIO);
    return readOptions;
  }

  public static boolean optimizeMultiGetForIO(@NonNull MemorySegment readOptions) {
    return (boolean) OPTIMIZE_MULTIGET_FOR_IO.get(readOptions);
  }

  public static MemorySegment asyncIO(boolean asyncIO, @NonNull MemorySegment readOptions) {
    ASYNC_IO.set(readOptions, asyncIO);
    return readOptions;
  }

  public static boolean asyncIO(@NonNull MemorySegment readOptions) {
    return (boolean) ASYNC_IO.get(readOptions);
  }

  public static MemorySegment ignoreRangeDeletions(
      boolean ignoreRangeDeletions, @NonNull MemorySegment readOptions) {
    IGNORE_RANGE_DELETIONS.set(readOptions, ignoreRangeDeletions);
    return readOptions;
  }

  public static boolean ignoreRangeDeletions(@NonNull MemorySegment readOptions) {
    return (boolean) IGNORE_RANGE_DELETIONS.get(readOptions);
  }

  public static MemorySegment verifyChecksums(
      boolean verifyChecksums, @NonNull MemorySegment readOptions) {
    VERIFY_CHECKSUMS.set(readOptions, verifyChecksums);
    return readOptions;
  }

  public static boolean verifyChecksums(@NonNull MemorySegment readOptions) {
    return (boolean) VERIFY_CHECKSUMS.get(readOptions);
  }

  @Deprecated
  public static MemorySegment managed(boolean managed, @NonNull MemorySegment readOptions) {
    MANAGED.set(readOptions, managed);
    return readOptions;
  }

  @Deprecated
  public static boolean managed(@NonNull MemorySegment readOptions) {
    return (boolean) MANAGED.get(readOptions);
  }

  public static MemorySegment fillCache(boolean fillCache, @NonNull MemorySegment readOptions) {
    FILL_CACHE.set(readOptions, fillCache);
    return readOptions;
  }

  public static boolean fillCache(@NonNull MemorySegment readOptions) {
    return (boolean) FILL_CACHE.get(readOptions);
  }

  public static MemorySegment valueSizeSoftLimit(
      long valueSizeSoftLimit, @NonNull MemorySegment readOptions) {
    VALUE_SIZE_SOFT_LIMIT.set(readOptions, valueSizeSoftLimit);
    return readOptions;
  }

  public static long valueSizeSoftLimit(@NonNull MemorySegment readOptions) {
    return (long) VALUE_SIZE_SOFT_LIMIT.get(readOptions);
  }

  public static MemorySegment readTier(
      @NonNull ReadTier readTier, @NonNull MemorySegment readOptions) {
    READ_TIER.set(readOptions, readTier.ordinal());
    return readOptions;
  }

  public static ReadTier readTier(@NonNull MemorySegment readOptions) {
    return enumType((int) READ_TIER.get(readOptions), ReadTier.class);
  }

  public static MemorySegment ioTimeout(long ioTimeout, @NonNull MemorySegment readOptions) {
    rocksdb_readoptions_set_io_timeout(readOptions, ioTimeout);
    return readOptions;
  }

  public static long ioTimeout(@NonNull MemorySegment readOptions) {
    return rocksdb_readoptions_get_io_timeout(readOptions);
  }

  public static MemorySegment deadline(long deadline, @NonNull MemorySegment readOptions) {
    rocksdb_readoptions_set_deadline(readOptions, deadline);
    return readOptions;
  }

  public static long deadline(@NonNull MemorySegment readOptions) {
    return rocksdb_readoptions_get_deadline(readOptions);
  }

  public static MemorySegment iterStartTs(
      @NonNull MemorySegment iterStartTs, @NonNull MemorySegment readOptions) {
    ITER_START_TS.set(readOptions, iterStartTs);
    return readOptions;
  }

  public static MemorySegment iterStartTs(@NonNull MemorySegment readOptions) {
    return (MemorySegment) ITER_START_TS.get(readOptions);
  }

  public static MemorySegment snapshot(
      @NonNull MemorySegment snapshot, @NonNull MemorySegment readOptions) {
    SNAPSHOT.set(readOptions, snapshot);
    return readOptions;
  }

  public static MemorySegment snapshot(@NonNull MemorySegment readOptions) {
    return (MemorySegment) SNAPSHOT.get(readOptions);
  }

  public static MemorySegment timestamp(@NonNull MemorySegment readOptions) {
    return (MemorySegment) TIMESTAMP.get(readOptions);
  }

  public static MemorySegment timestamp(
      @NonNull MemorySegment timestamp, @NonNull MemorySegment readOptions) {
    TIMESTAMP.set(readOptions, timestamp);
    return readOptions;
  }

  public static MemorySegment iterateUpperBound(
      @NonNull MemorySegment iterateUpperBound, @NonNull MemorySegment readOptions) {
    ITERATE_UPPER_BOUND.set(readOptions, iterateUpperBound);
    return readOptions;
  }

  public static MemorySegment iterateUpperBound(@NonNull MemorySegment readOptions) {
    return (MemorySegment) ITERATE_UPPER_BOUND.get(readOptions);
  }

  public static MemorySegment iterateLowerBound(
      @NonNull MemorySegment iterateLowerBound, @NonNull MemorySegment readOptions) {
    ITERATE_LOWER_BOUND.set(readOptions, iterateLowerBound);
    return readOptions;
  }

  public static MemorySegment iterateLowerBound(@NonNull MemorySegment readOptions) {
    return (MemorySegment) ITERATE_LOWER_BOUND.get(readOptions);
  }

  //  public static MemorySegment tableFilter(
  //      @NonNull MemorySegment tableFilter, @NonNull MemorySegment readOptions) {
  //    TABLE_FILTER.set(readOptions, tableFilter);
  //    return readOptions;
  //  }

  //  public static MemorySegment tableFilter(@NonNull MemorySegment readOptions) {
  //    return (MemorySegment) TABLE_FILTER.get(readOptions);
  //  }

  public static MemorySegment ioActivity(
      @NonNull IOActivity ioActivity, @NonNull MemorySegment readOptions) {
    IO_ACTIVITY.set(readOptions, (byte) ioActivity.ordinal());
    return readOptions;
  }

  public static IOActivity ioActivity(@NonNull MemorySegment readOptions) {
    return enumType((int) IO_ACTIVITY.get(readOptions), IOActivity.class);
  }

  /**
   * 创建native ReadOptions，需要自行释放资源
   *
   * @return ReadOptions
   */
  public static MemorySegment create() {
    return rocksdb_readoptions_create();
  }

  /**
   * 释放readOptions资源
   *
   * @param readOptions native ReadOptions
   */
  public static void destroy(@NonNull MemorySegment readOptions) {
    rocksdb_readoptions_destroy(readOptions);
  }
}
