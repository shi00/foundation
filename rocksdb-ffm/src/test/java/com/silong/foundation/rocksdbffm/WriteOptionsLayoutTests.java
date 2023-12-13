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

import static com.silong.foundation.rocksdbffm.enu.IOPriority.IO_HIGH;
import static com.silong.foundation.rocksdbffm.enu.IOPriority.IO_MID;

import com.silong.foundation.rocksdbffm.options.WriteOptions;
import java.lang.foreign.MemorySegment;
import java.nio.file.Paths;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 描述信息
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-12-13 19:19
 */
public class WriteOptionsLayoutTests {

  /** 加载动态库 */
  @BeforeAll
  static void init() {
    RocksDbConfig config = new RocksDbConfig();
    config.setPersistDataPath(
        Paths.get(System.getProperty("user.dir"))
            .resolve("target")
            .resolve("rocksdb-wopl-data")
            .toFile()
            .getAbsolutePath());
    RocksDbImpl rocksDb = (RocksDbImpl) RocksDb.getInstance(config);
    rocksDb.close();
  }

  @Test
  public void test1() {
    WriteOptions writeOptions1 = new WriteOptions();
    MemorySegment writeOptions = WriteOptions.create();
    try {
      checkValues(writeOptions1, writeOptions);
    } finally {
      WriteOptions.destroy(writeOptions);
    }
  }

  @Test
  public void test2() {
    WriteOptions writeOptions1 = new WriteOptions();
    writeOptions1.setSync(true);
    MemorySegment writeOptions = writeOptions1.to();
    try {
      checkValues(writeOptions1, writeOptions);
    } finally {
      WriteOptions.destroy(writeOptions);
    }
  }

  @Test
  public void test3() {
    WriteOptions writeOptions1 = new WriteOptions();
    writeOptions1.setDisableWAL(true);
    MemorySegment writeOptions = writeOptions1.to();
    try {
      checkValues(writeOptions1, writeOptions);
    } finally {
      WriteOptions.destroy(writeOptions);
    }
  }

  @Test
  public void test4() {
    WriteOptions writeOptions1 = new WriteOptions();
    writeOptions1.setLowPri(true);
    MemorySegment writeOptions = writeOptions1.to();
    try {
      checkValues(writeOptions1, writeOptions);
    } finally {
      WriteOptions.destroy(writeOptions);
    }
  }

  @Test
  public void test5() {
    WriteOptions writeOptions1 = new WriteOptions();
    writeOptions1.setNoSlowdown(true);
    MemorySegment writeOptions = writeOptions1.to();
    try {
      checkValues(writeOptions1, writeOptions);
    } finally {
      WriteOptions.destroy(writeOptions);
    }
  }

  @Test
  public void test6() {
    WriteOptions writeOptions1 = new WriteOptions();
    writeOptions1.setIgnoreMissingColumnFamilies(true);
    MemorySegment writeOptions = writeOptions1.to();
    try {
      checkValues(writeOptions1, writeOptions);
    } finally {
      WriteOptions.destroy(writeOptions);
    }
  }

  @Test
  public void test7() {
    WriteOptions writeOptions1 = new WriteOptions();
    writeOptions1.setMemtableInsertHintPerBatch(true);
    MemorySegment writeOptions = writeOptions1.to();
    try {
      checkValues(writeOptions1, writeOptions);
    } finally {
      WriteOptions.destroy(writeOptions);
    }
  }

  @Test
  public void test8() {
    WriteOptions writeOptions1 = new WriteOptions();
    writeOptions1.setRateLimiterPriority(IO_HIGH);
    MemorySegment writeOptions = writeOptions1.to();
    try {
      checkValues(writeOptions1, writeOptions);
    } finally {
      WriteOptions.destroy(writeOptions);
    }
  }

  @Test
  public void test9() {
    WriteOptions writeOptions1 = new WriteOptions();
    writeOptions1.setProtectionBytesPerKey(182);
    MemorySegment writeOptions = writeOptions1.to();
    try {
      checkValues(writeOptions1, writeOptions);
    } finally {
      WriteOptions.destroy(writeOptions);
    }
  }

  @Test
  public void test10() {
    WriteOptions writeOptions1 = new WriteOptions();
    writeOptions1.setProtectionBytesPerKey(182);
    writeOptions1.setRateLimiterPriority(IO_MID);
    writeOptions1.setMemtableInsertHintPerBatch(true);
    writeOptions1.setSync(true);
    writeOptions1.setLowPri(true);
    writeOptions1.setNoSlowdown(true);
    writeOptions1.setDisableWAL(true);
    writeOptions1.setIgnoreMissingColumnFamilies(true);
    MemorySegment writeOptions = writeOptions1.to();
    try {
      checkValues(writeOptions1, writeOptions);
    } finally {
      WriteOptions.destroy(writeOptions);
    }
  }

  private static void checkValues(WriteOptions wp1, MemorySegment wp2) {
    Assertions.assertEquals(wp1.isSync(), WriteOptions.sync(wp2));
    Assertions.assertEquals(wp1.isDisableWAL(), WriteOptions.disableWAL(wp2));
    Assertions.assertEquals(
        wp1.isIgnoreMissingColumnFamilies(), WriteOptions.ignoreMissingColumnFamilies(wp2));
    Assertions.assertEquals(wp1.isNoSlowdown(), WriteOptions.noSlowdown(wp2));
    Assertions.assertEquals(wp1.isLowPri(), WriteOptions.lowPri(wp2));
    Assertions.assertEquals(
        wp1.isMemtableInsertHintPerBatch(), WriteOptions.memtableInsertHintPerBatch(wp2));
    Assertions.assertEquals(wp1.getRateLimiterPriority(), WriteOptions.rateLimiterPriority(wp2));
    Assertions.assertEquals(
        wp1.getProtectionBytesPerKey(), WriteOptions.protectionBytesPerKey(wp2));
  }
}
