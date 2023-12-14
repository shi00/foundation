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

import static com.silong.foundation.rocksdbffm.Utils.enumType;
import static com.silong.foundation.rocksdbffm.enu.IOPriority.IO_TOTAL;
import static com.silong.foundation.rocksdbffm.generated.RocksDB.rocksdb_writeoptions_create;
import static com.silong.foundation.rocksdbffm.generated.RocksDB.rocksdb_writeoptions_destroy;
import static java.lang.foreign.MemoryLayout.paddingLayout;
import static java.lang.foreign.MemoryLayout.structLayout;
import static java.lang.foreign.ValueLayout.*;

import com.silong.foundation.rocksdbffm.enu.IOPriority;
import java.io.Serial;
import java.io.Serializable;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.VarHandle;
import lombok.Data;
import lombok.NonNull;

/**
 * 写入配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-12-13 10:43
 */
@Data
public final class WriteOptions implements Options, Serializable {

  @Serial private static final long serialVersionUID = 8_670_699_038_504_974_883L;

  private static final MemoryLayout LAYOUT =
      structLayout(
          JAVA_BOOLEAN.withName("sync"),
          //          paddingLayout(1),
          JAVA_BOOLEAN.withName("disableWAL"),
          //          paddingLayout(1),
          JAVA_BOOLEAN.withName("ignore_missing_column_families"),
          //          paddingLayout(1),
          JAVA_BOOLEAN.withName("no_slowdown"),
          //          paddingLayout(1),
          JAVA_BOOLEAN.withName("low_pri"),
          //          paddingLayout(1),
          JAVA_BOOLEAN.withName("memtable_insert_hint_per_batch"),
          paddingLayout(2),
          JAVA_INT.withName("rate_limiter_priority"),
          paddingLayout(4),
          JAVA_LONG.withName("protection_bytes_per_key"));

  private static final VarHandle SYNC = LAYOUT.varHandle(PathElement.groupElement("sync"));

  private static final VarHandle DISABLE_WAL =
      LAYOUT.varHandle(PathElement.groupElement("disableWAL"));

  private static final VarHandle IGNORE_MISSING_COLUMN_FAMILIES =
      LAYOUT.varHandle(PathElement.groupElement("ignore_missing_column_families"));

  private static final VarHandle NO_SLOWDOWN =
      LAYOUT.varHandle(PathElement.groupElement("no_slowdown"));

  private static final VarHandle LOW_PRI = LAYOUT.varHandle(PathElement.groupElement("low_pri"));

  private static final VarHandle MEMTABLE_INSERT_HINT_PER_BATCH =
      LAYOUT.varHandle(PathElement.groupElement("memtable_insert_hint_per_batch"));

  private static final VarHandle RATE_LIMITER_PRIORITY =
      LAYOUT.varHandle(PathElement.groupElement("rate_limiter_priority"));

  private static final VarHandle PROTECTION_BYTES_PER_KEY =
      LAYOUT.varHandle(PathElement.groupElement("protection_bytes_per_key"));

  /**
   * If true, the write will be flushed from the operating system buffer cache (by calling
   * WritableFile::Sync()) before the write is considered complete. If this flag is true, writes
   * will be slower. If this flag is false, and the machine crashes, some recent writes may be lost.
   * Note that if it is just the process that crashes (i.e., the machine does not reboot), no writes
   * will be lost even if sync==false. In other words, a DB write with sync==false has similar crash
   * semantics as the "write()" system call. A DB write with sync==true has similar crash semantics
   * to a "write()" system call followed by "fdatasync()". Default: false
   */
  private boolean sync;

  /**
   * If true, writes will not first go to the write ahead log, and the write may get lost after a
   * crash. The backup engine relies on write-ahead logs to back up the memtable, so if you disable
   * write-ahead logs, you must create backups with flush_before_backup=true to avoid losing
   * unflushed memtable data. Default: false
   */
  private boolean disableWAL;

  /**
   * If true and if user is trying to write to column families that don't exist (they were dropped),
   * ignore the write (don't return an error). If there are multiple writes in a WriteBatch, other
   * writes will succeed. Default: false
   */
  private boolean ignoreMissingColumnFamilies;

  /**
   * If true and we need to wait or sleep for the write request, fails immediately with
   * Status::Incomplete(). Default: false
   */
  private boolean noSlowdown;

  /**
   * If true, this write request is of lower priority if compaction is behind. In this case,
   * no_slowdown = true, the request will be canceled immediately with Status::Incomplete()
   * returned. Otherwise, it will be slowed down. The slowdown value is determined by RocksDB to
   * guarantee it introduces minimum impacts to high priority writes.
   *
   * <p>Default: false
   */
  private boolean lowPri;

  /**
   * If true, this writebatch will maintain the last insert positions of each memtable as hints in
   * concurrent write. It can improve write performance in concurrent writes if keys in one
   * writebatch are sequential. In non-concurrent writes (when concurrent_memtable_writes is false)
   * this option will be ignored.
   *
   * <p>Default: false
   */
  private boolean memtableInsertHintPerBatch;

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
   * <p>Default: `Env::IO_TOTAL` Env::IOPriority rate_limiter_priority;
   *
   * <p>`protection_bytes_per_key` is the number of bytes used to store protection information for
   * each key entry. Currently supported values are zero (disabled) and eight.
   *
   * <p>Default: zero (disabled).
   */
  private long protectionBytesPerKey;

  @Override
  public WriteOptions from(@NonNull MemorySegment writeOptions) {
    this.sync = sync(writeOptions);
    this.disableWAL = disableWAL(writeOptions);
    this.noSlowdown = noSlowdown(writeOptions);
    this.memtableInsertHintPerBatch = memtableInsertHintPerBatch(writeOptions);
    this.lowPri = lowPri(writeOptions);
    this.ignoreMissingColumnFamilies = ignoreMissingColumnFamilies(writeOptions);
    this.protectionBytesPerKey = protectionBytesPerKey(writeOptions);
    this.rateLimiterPriority = rateLimiterPriority(writeOptions);
    return this;
  }

  /**
   * 分配的native options必须由调用方自行释放内存
   *
   * @return native options
   */
  @Override
  public MemorySegment to() {
    MemorySegment writeOptions = rocksdb_writeoptions_create();
    rateLimiterPriority(this.rateLimiterPriority, writeOptions);
    lowPri(this.lowPri, writeOptions);
    sync(this.sync, writeOptions);
    noSlowdown(this.noSlowdown, writeOptions);
    ignoreMissingColumnFamilies(this.ignoreMissingColumnFamilies, writeOptions);
    disableWAL(this.disableWAL, writeOptions);
    memtableInsertHintPerBatch(this.memtableInsertHintPerBatch, writeOptions);
    protectionBytesPerKey(this.protectionBytesPerKey, writeOptions);
    return writeOptions;
  }

  @Override
  public MemoryLayout layout() {
    return LAYOUT;
  }

  public static String toString(MemorySegment writeOptions) {
    return String.format(
        "writeOptions:[sync:%b, disableWAL:%b, ignore_missing_column_families:%b, no_slowdown:%b, low_pri:%b, memtable_insert_hint_per_batch:%b, rate_limiter_priority:%s, protection_bytes_per_key:%d]",
        sync(writeOptions),
        disableWAL(writeOptions),
        ignoreMissingColumnFamilies(writeOptions),
        noSlowdown(writeOptions),
        lowPri(writeOptions),
        memtableInsertHintPerBatch(writeOptions),
        rateLimiterPriority(writeOptions),
        protectionBytesPerKey(writeOptions));
  }

  public static MemorySegment rateLimiterPriority(
      @NonNull IOPriority ioPriority, @NonNull MemorySegment writeOptions) {
    RATE_LIMITER_PRIORITY.set(writeOptions, ioPriority.ordinal());
    return writeOptions;
  }

  public static MemorySegment protectionBytesPerKey(
      long protectionBytesPerKey, @NonNull MemorySegment writeOptions) {
    PROTECTION_BYTES_PER_KEY.set(writeOptions, protectionBytesPerKey);
    return writeOptions;
  }

  public static MemorySegment memtableInsertHintPerBatch(
      boolean memtableInsertHintPerBatch, @NonNull MemorySegment writeOptions) {
    MEMTABLE_INSERT_HINT_PER_BATCH.set(writeOptions, memtableInsertHintPerBatch);
    return writeOptions;
  }

  public static MemorySegment sync(boolean sync, @NonNull MemorySegment writeOptions) {
    SYNC.set(writeOptions, sync);
    return writeOptions;
  }

  public static MemorySegment lowPri(boolean lowPri, @NonNull MemorySegment writeOptions) {
    LOW_PRI.set(writeOptions, lowPri);
    return writeOptions;
  }

  public static MemorySegment noSlowdown(boolean noSlowdown, @NonNull MemorySegment writeOptions) {
    NO_SLOWDOWN.set(writeOptions, noSlowdown);
    return writeOptions;
  }

  public static MemorySegment disableWAL(boolean disableWAL, @NonNull MemorySegment writeOptions) {
    DISABLE_WAL.set(writeOptions, disableWAL);
    return writeOptions;
  }

  public static MemorySegment ignoreMissingColumnFamilies(
      boolean ignoreMissingColumnFamilies, @NonNull MemorySegment writeOptions) {
    IGNORE_MISSING_COLUMN_FAMILIES.set(writeOptions, ignoreMissingColumnFamilies);
    return writeOptions;
  }

  /**
   * 创建native WriteOptions，需要自行释放资源
   *
   * @return WriteOptions
   */
  public static MemorySegment create() {
    return rocksdb_writeoptions_create();
  }

  /**
   * 释放WriteOptions资源
   *
   * @param writeOptions native WriteOptions
   */
  public static void destroy(@NonNull MemorySegment writeOptions) {
    rocksdb_writeoptions_destroy(writeOptions);
  }

  public static boolean sync(@NonNull MemorySegment writeOptions) {
    return (boolean) SYNC.get(writeOptions);
  }

  public static boolean ignoreMissingColumnFamilies(@NonNull MemorySegment writeOptions) {
    return (boolean) IGNORE_MISSING_COLUMN_FAMILIES.get(writeOptions);
  }

  public static boolean noSlowdown(@NonNull MemorySegment writeOptions) {
    return (boolean) NO_SLOWDOWN.get(writeOptions);
  }

  public static boolean disableWAL(@NonNull MemorySegment writeOptions) {
    return (boolean) DISABLE_WAL.get(writeOptions);
  }

  public static boolean memtableInsertHintPerBatch(@NonNull MemorySegment writeOptions) {
    return (boolean) MEMTABLE_INSERT_HINT_PER_BATCH.get(writeOptions);
  }

  public static IOPriority rateLimiterPriority(@NonNull MemorySegment writeOptions) {
    return enumType((int) RATE_LIMITER_PRIORITY.get(writeOptions), IOPriority.class);
  }

  public static boolean lowPri(@NonNull MemorySegment writeOptions) {
    return (boolean) LOW_PRI.get(writeOptions);
  }

  public static long protectionBytesPerKey(@NonNull MemorySegment writeOptions) {
    return (long) PROTECTION_BYTES_PER_KEY.get(writeOptions);
  }
}
