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
import static com.silong.foundation.rocksdbffm.generated.RocksDB_1.rocksdb_readoptions_get_async_io;
import static com.silong.foundation.rocksdbffm.generated.RocksDB_1.rocksdb_readoptions_get_deadline;
import static java.lang.foreign.MemorySegment.NULL;

import com.silong.foundation.rocksdbffm.enu.IOActivity;
import com.silong.foundation.rocksdbffm.enu.IOPriority;
import com.silong.foundation.rocksdbffm.enu.ReadTier;
import java.io.Serial;
import java.io.Serializable;
import java.lang.foreign.MemorySegment;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * 读取配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-12-13 10:43
 */
@Data
@Slf4j
public final class ReadOptions implements Options, Serializable {

  @Serial private static final long serialVersionUID = -3_531_065_411_779_519_195L;

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
  private Boolean verifyChecksums = Boolean.TRUE;

  /**
   * Should the "data block"/"index block" read for this iteration be placed in block cache? Callers
   * may wish to set this field to false for bulk scans. This would help not to the change eviction
   * order of existing items in the block cache.
   */
  private Boolean fillCache = Boolean.TRUE;

  /**
   * If true, range tombstones handling will be skipped in key lookup paths. For DB instances that
   * don't use DeleteRange() calls, this setting can be used to optimize the read performance. Note
   * that, if this assumption (of no previous DeleteRange() calls) is broken, stale keys could be
   * served in read paths.
   */
  private Boolean ignoreRangeDeletions = Boolean.FALSE;

  /**
   * Experimental: If async_io is enabled, RocksDB will prefetch some of data asynchronously.
   * RocksDB apply it if reads are sequential and its internal automatic prefetching.
   */
  private Boolean asyncIO = Boolean.FALSE;

  /**
   * Experimental: If async_io is set, then this flag controls whether we read SST files in multiple
   * levels asynchronously. Enabling this flag can help reduce MultiGet latency by maximizing the
   * number of SST files read in parallel if the keys in the MultiGet batch are in different levels.
   * It comes at the expense of slightly higher CPU overhead.
   */
  private Boolean optimizeMultiGetForIO = Boolean.TRUE;

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
  private Boolean tailing = Boolean.FALSE;

  /**
   * This options is not used anymore. It was to turn on a functionality that has been removed.
   * DEPRECATED
   */
  @Deprecated private Boolean managed = Boolean.FALSE;

  /**
   * Enable a total order seek regardless of index format (e.g. hash index) used in the table. Some
   * table format (e.g. plain table) may not support this option. If true when calling Get(), we
   * also skip prefix bloom when reading from block based table, which only affects Get()
   * performance.
   */
  private Boolean totalOrderSeek = Boolean.FALSE;

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
  private Boolean autoPrefixMode = Boolean.FALSE;

  /**
   * Enforce that the iterator only iterates over the same prefix as the seek. This option is
   * effective only for prefix seeks, i.e. prefix_extractor is non-null for the column family and
   * total_order_seek is false. Unlike iterate_upper_bound, prefix_same_as_start only works within a
   * prefix but in both directions.
   */
  private Boolean prefixSameAsStart = Boolean.FALSE;

  /**
   * Keep the blocks loaded by the iterator pinned in memory as long as the iterator is not deleted,
   * If used when reading from tables created with BlockBasedTableOptions::use_delta_encoding =
   * false, Iterator's property "rocksdb.iterator.is-key-pinned" is guaranteed to return 1.
   */
  private Boolean pinData = Boolean.FALSE;

  /**
   * For iterators, RocksDB does auto-readahead on noticing more than two sequential reads for a
   * table file if user doesn't provide readahead_size. The readahead starts at 8KB and doubles on
   * every additional read upto max_auto_readahead_size only when reads are sequential. However at
   * each level, if iterator moves over next file, readahead_size starts again from 8KB. By enabling
   * this option, RocksDB will do some enhancements for prefetching the data.
   */
  private Boolean adaptiveReadAhead = Boolean.FALSE;

  /**
   * If true, when PurgeObsoleteFile is called in CleanupIteratorState, we schedule a background job
   * in the flush job queue and delete obsolete files in background.
   */
  private Boolean backgroundPurgeOnIteratorCleanup = Boolean.FALSE;

  /**
   * A callback to determine whether relevant keys for this scan exist in a given table based on the
   * table's properties. The callback is passed the properties of each table during iteration. If
   * the callback returns false, the table will not be scanned. This option only affects Iterators
   * and has no impact on point lookups. Default: empty (every table will be scanned).
   */
  private MemorySegment tableFilter;

  /**
   * If auto_readahead_size is set to true, it will auto tune the readahead_size during scans
   * internally. For this feature to enabled, iterate_upper_bound must also be specified.
   *
   * <p>NOTE: - Recommended for forward Scans only. - If there is a backward scans, this option will
   * be disabled internally and won't be enabled again if the forward scan is issued again.
   *
   * <p>Default: true
   */
  private Boolean autoReadAheadSize = Boolean.TRUE;

  // *** END options only relevant to iterators or scans ***

  // *** BEGIN options for RocksDB internal use only ***
  private IOActivity ioActivity = K_UNKNOWN;

  // *** END options for RocksDB internal use only ***

  @Override
  public ReadOptions from(@NonNull MemorySegment readOptionsPtr) {
    this.asyncIO = byte2Boolean(rocksdb_readoptions_get_async_io(readOptionsPtr));
    this.deadline = rocksdb_readoptions_get_deadline(readOptionsPtr);
    this.fillCache = byte2Boolean(rocksdb_readoptions_get_fill_cache(readOptionsPtr));
    this.backgroundPurgeOnIteratorCleanup =
        byte2Boolean(rocksdb_readoptions_get_background_purge_on_iterator_cleanup(readOptionsPtr));
    this.ignoreRangeDeletions =
        byte2Boolean(rocksdb_readoptions_get_ignore_range_deletions(readOptionsPtr));
    this.ioTimeout = rocksdb_readoptions_get_io_timeout(readOptionsPtr);
    this.maxSkippableInternalKeys =
        rocksdb_readoptions_get_max_skippable_internal_keys(readOptionsPtr);
    this.pinData = byte2Boolean(rocksdb_readoptions_get_pin_data(readOptionsPtr));
    this.prefixSameAsStart =
        byte2Boolean(rocksdb_readoptions_get_prefix_same_as_start(readOptionsPtr));
    this.readTier = enumType(rocksdb_readoptions_get_read_tier(readOptionsPtr), ReadTier.class);
    this.readAheadSize = rocksdb_readoptions_get_readahead_size(readOptionsPtr);
    this.tailing = byte2Boolean(rocksdb_readoptions_get_tailing(readOptionsPtr));
    this.totalOrderSeek = byte2Boolean(rocksdb_readoptions_get_total_order_seek(readOptionsPtr));
    this.verifyChecksums = byte2Boolean(rocksdb_readoptions_get_verify_checksums(readOptionsPtr));
    return this;
  }

  /**
   * 分配的native options必须由调用方自行释放内存
   *
   * @return native options
   */
  @Override
  public MemorySegment to() {
    MemorySegment readOptionsPtr = create();
    rocksdb_readoptions_set_verify_checksums(readOptionsPtr, boolean2Byte(verifyChecksums));
    rocksdb_readoptions_set_auto_readahead_size(readOptionsPtr, boolean2Byte(autoReadAheadSize));
    rocksdb_readoptions_set_async_io(readOptionsPtr, boolean2Byte(asyncIO));
    rocksdb_readoptions_set_background_purge_on_iterator_cleanup(
        readOptionsPtr, boolean2Byte(backgroundPurgeOnIteratorCleanup));
    rocksdb_readoptions_set_deadline(readOptionsPtr, deadline);
    rocksdb_readoptions_set_fill_cache(readOptionsPtr, boolean2Byte(fillCache));
    rocksdb_readoptions_set_io_timeout(readOptionsPtr, ioTimeout);
    rocksdb_readoptions_set_ignore_range_deletions(
        readOptionsPtr, boolean2Byte(ignoreRangeDeletions));
    rocksdb_readoptions_set_snapshot(readOptionsPtr, snapshot);

    return readOptionsPtr;
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
   * @param readOptions ReadOptions ptr
   */
  public static void destroy(@NonNull MemorySegment readOptions) {
    rocksdb_readoptions_destroy(readOptions);
  }
}
