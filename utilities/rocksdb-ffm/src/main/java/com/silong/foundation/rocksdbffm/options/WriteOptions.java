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

import static com.silong.foundation.rocksdbffm.Utils.boolean2Byte;
import static com.silong.foundation.rocksdbffm.Utils.byte2Boolean;
import static com.silong.foundation.rocksdbffm.enu.IOActivity.K_UNKNOWN;
import static com.silong.foundation.rocksdbffm.enu.IOPriority.IO_TOTAL;
import static com.silong.foundation.rocksdbffm.generated.RocksDB.rocksdb_writeoptions_create;
import static com.silong.foundation.rocksdbffm.generated.RocksDB.rocksdb_writeoptions_destroy;
import static com.silong.foundation.rocksdbffm.generated.RocksDB_1.*;

import com.silong.foundation.rocksdbffm.enu.IOActivity;
import com.silong.foundation.rocksdbffm.enu.IOPriority;
import java.io.Serial;
import java.io.Serializable;
import java.lang.foreign.MemorySegment;
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

  /**
   * If true, the write will be flushed from the operating system buffer cache (by calling
   * WritableFile::Sync()) before the write is considered complete. If this flag is true, writes
   * will be slower. If this flag is false, and the machine crashes, some recent writes may be lost.
   * Note that if it is just the process that crashes (i.e., the machine does not reboot), no writes
   * will be lost even if sync==false. In other words, a DB write with sync==false has similar crash
   * semantics as the "write()" system call. A DB write with sync==true has similar crash semantics
   * to a "write()" system call followed by "fdatasync()". Default: false
   */
  private Boolean sync = Boolean.FALSE;

  /**
   * If true, writes will not first go to the write ahead log, and the write may get lost after a
   * crash. The backup engine relies on write-ahead logs to back up the memtable, so if you disable
   * write-ahead logs, you must create backups with flush_before_backup=true to avoid losing
   * unflushed memtable data. Default: false
   */
  private Boolean disableWAL = Boolean.FALSE;

  /**
   * If true and if user is trying to write to column families that don't exist (they were dropped),
   * ignore the write (don't return an error). If there are multiple writes in a WriteBatch, other
   * writes will succeed. Default: false
   */
  private Boolean ignoreMissingColumnFamilies = Boolean.FALSE;

  /**
   * If true and we need to wait or sleep for the write request, fails immediately with
   * Status::Incomplete(). Default: false
   */
  private Boolean noSlowdown = Boolean.FALSE;

  /**
   * If true, this write request is of lower priority if compaction is behind. In this case,
   * no_slowdown = true, the request will be canceled immediately with Status::Incomplete()
   * returned. Otherwise, it will be slowed down. The slowdown value is determined by RocksDB to
   * guarantee it introduces minimum impacts to high priority writes.
   *
   * <p>Default: false
   */
  private Boolean lowPri = Boolean.FALSE;

  /**
   * If true, this writebatch will maintain the last insert positions of each memtable as hints in
   * concurrent write. It can improve write performance in concurrent writes if keys in one
   * writebatch are sequential. In non-concurrent writes (when concurrent_memtable_writes is false)
   * this option will be ignored.
   *
   * <p>Default: false
   */
  private Boolean memtableInsertHintPerBatch = Boolean.FALSE;

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
  private long protectionBytesPerKey = 0;

  // For RocksDB internal use only
  // Default: Env::IOActivity::kUnknown.
  private IOActivity ioActivity = K_UNKNOWN;

  @Override
  public WriteOptions from(@NonNull MemorySegment writeOptionsPtr) {
    this.sync = getSync(writeOptionsPtr);
    this.disableWAL = getDisableWAL(writeOptionsPtr);
    this.noSlowdown = getNoSlowdown(writeOptionsPtr);
    this.memtableInsertHintPerBatch = getMemtableInsertHintPerBatch(writeOptionsPtr);
    this.lowPri = getLowPri(writeOptionsPtr);
    this.ignoreMissingColumnFamilies = getIgnoreMissingColumnFamilies(writeOptionsPtr);
    return this;
  }

  /**
   * 分配的native options必须由调用方自行释放内存
   *
   * @return native options
   */
  @Override
  public MemorySegment to() {
    MemorySegment writeOptionsPtr = create();
    setDisableWal(writeOptionsPtr);
    setLowPri(writeOptionsPtr);
    setIgnoreMissingColumnFamilies(writeOptionsPtr);
    setMemtableInsertHintPerBatch(writeOptionsPtr);
    setNoSlowDown(writeOptionsPtr);
    setSync(writeOptionsPtr);
    return writeOptionsPtr;
  }

  public static String toString(@NonNull MemorySegment writeOptions) {
    return String.format(
        "writeOptions:[sync:%b, disableWAL:%b, ignore_missing_column_families:%b, no_slowdown:%b, low_pri:%b, memtable_insert_hint_per_batch:%b]",
        getSync(writeOptions),
        getDisableWAL(writeOptions),
        getIgnoreMissingColumnFamilies(writeOptions),
        getNoSlowdown(writeOptions),
        getLowPri(writeOptions),
        getMemtableInsertHintPerBatch(writeOptions));
  }

  private void setSync(MemorySegment writeOptionsPtr) {
    rocksdb_writeoptions_set_sync(writeOptionsPtr, boolean2Byte(sync));
  }

  private void setNoSlowDown(MemorySegment writeOptionsPtr) {
    rocksdb_writeoptions_set_no_slowdown(writeOptionsPtr, boolean2Byte(noSlowdown));
  }

  private void setMemtableInsertHintPerBatch(MemorySegment writeOptionsPtr) {
    rocksdb_writeoptions_set_memtable_insert_hint_per_batch(
        writeOptionsPtr, boolean2Byte(memtableInsertHintPerBatch));
  }

  private void setLowPri(MemorySegment writeOptionsPtr) {
    rocksdb_writeoptions_set_low_pri(writeOptionsPtr, boolean2Byte(lowPri));
  }

  private void setIgnoreMissingColumnFamilies(MemorySegment writeOptionsPtr) {
    rocksdb_writeoptions_set_ignore_missing_column_families(
        writeOptionsPtr, boolean2Byte(ignoreMissingColumnFamilies));
  }

  private void setDisableWal(MemorySegment writeOptionsPtr) {
    rocksdb_writeoptions_disable_WAL(writeOptionsPtr, boolean2Byte(disableWAL));
  }

  private static Boolean getIgnoreMissingColumnFamilies(MemorySegment writeOptionsPtr) {
    return byte2Boolean(rocksdb_writeoptions_get_ignore_missing_column_families(writeOptionsPtr));
  }

  private static Boolean getLowPri(MemorySegment writeOptionsPtr) {
    return byte2Boolean(rocksdb_writeoptions_get_low_pri(writeOptionsPtr));
  }

  private static Boolean getMemtableInsertHintPerBatch(MemorySegment writeOptionsPtr) {
    return byte2Boolean(rocksdb_writeoptions_get_memtable_insert_hint_per_batch(writeOptionsPtr));
  }

  private static Boolean getNoSlowdown(MemorySegment writeOptionsPtr) {
    return byte2Boolean(rocksdb_writeoptions_get_no_slowdown(writeOptionsPtr));
  }

  private static Boolean getDisableWAL(MemorySegment writeOptionsPtr) {
    return byte2Boolean(rocksdb_writeoptions_get_disable_WAL(writeOptionsPtr));
  }

  private static Boolean getSync(MemorySegment writeOptionsPtr) {
    return byte2Boolean(rocksdb_writeoptions_get_sync(writeOptionsPtr));
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
}
