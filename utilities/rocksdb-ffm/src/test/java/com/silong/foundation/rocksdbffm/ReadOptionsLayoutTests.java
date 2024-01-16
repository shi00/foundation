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

import static com.silong.foundation.rocksdbffm.Utils.enumType;
import static com.silong.foundation.rocksdbffm.generated.RocksDB.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.silong.foundation.rocksdbffm.config.RocksDbConfig;
import com.silong.foundation.rocksdbffm.enu.IOActivity;
import com.silong.foundation.rocksdbffm.enu.IOPriority;
import com.silong.foundation.rocksdbffm.enu.ReadTier;
import com.silong.foundation.rocksdbffm.options.ReadOptions;
import java.lang.foreign.MemorySegment;
import java.nio.file.Paths;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 内存布局测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-12-13 19:19
 */
public class ReadOptionsLayoutTests {

  private static RocksDbImpl rocksDb;

  /** 加载动态库 */
  @BeforeAll
  static void init() {
    RocksDbConfig config = new RocksDbConfig();
    config.setPersistDataPath(
        Paths.get(System.getProperty("user.dir"))
            .resolve("target")
            .resolve("rocksdb-ropl-data")
            .toFile()
            .getAbsolutePath());
    rocksDb = (RocksDbImpl) RocksDb.getInstance(config);
  }

  @AfterAll
  static void cleanup() {
    rocksDb.close();
  }

  @Test
  public void test1() {
    MemorySegment readOptions = ReadOptions.create();
    try {
      checkValues(new ReadOptions(), readOptions);
    } finally {
      ReadOptions.destroy(readOptions);
    }
  }

  @Test
  public void test2() {
    ReadOptions wp1 = new ReadOptions();
    wp1.setDeadline(RandomUtils.nextLong());
    MemorySegment readOptions = wp1.to();
    try {
      checkValues(wp1, readOptions);
    } finally {
      ReadOptions.destroy(readOptions);
    }
  }

  @Test
  public void test3() {
    ReadOptions wp1 = new ReadOptions();
    wp1.setIoTimeout(RandomUtils.nextLong());
    MemorySegment readOptions = wp1.to();
    try {
      checkValues(wp1, readOptions);
    } finally {
      ReadOptions.destroy(readOptions);
    }
  }

  @Test
  public void test4() {
    ReadOptions wp1 = new ReadOptions();
    wp1.setReadTier(enumType(RandomUtils.nextInt(0, 4), ReadTier.class));
    MemorySegment readOptions = wp1.to();
    try {
      checkValues(wp1, readOptions);
    } finally {
      ReadOptions.destroy(readOptions);
    }
  }

  @Test
  public void test5() {
    ReadOptions wp1 = new ReadOptions();
    wp1.setReadAheadSize(RandomUtils.nextLong());
    MemorySegment readOptions = wp1.to();
    try {
      checkValues(wp1, readOptions);
    } finally {
      ReadOptions.destroy(readOptions);
    }
  }

  @Test
  public void test6() {
    ReadOptions wp1 = new ReadOptions();
    wp1.setAdaptiveReadAhead(RandomUtils.nextBoolean());
    MemorySegment readOptions = wp1.to();
    try {
      checkValues(wp1, readOptions);
    } finally {
      ReadOptions.destroy(readOptions);
    }
  }

  @Test
  public void test7() {
    ReadOptions wp1 = new ReadOptions();
    wp1.setBackgroundPurgeOnIteratorCleanup(RandomUtils.nextBoolean());
    MemorySegment readOptions = wp1.to();
    try {
      checkValues(wp1, readOptions);
    } finally {
      ReadOptions.destroy(readOptions);
    }
  }

  @Test
  public void test8() {
    ReadOptions wp1 = new ReadOptions();
    wp1.setAsyncIO(RandomUtils.nextBoolean());
    MemorySegment readOptions = wp1.to();
    try {
      checkValues(wp1, readOptions);
    } finally {
      ReadOptions.destroy(readOptions);
    }
  }

  @Test
  public void test9() {
    ReadOptions wp1 = new ReadOptions();
    wp1.setFillCache(RandomUtils.nextBoolean());
    MemorySegment readOptions = wp1.to();
    try {
      checkValues(wp1, readOptions);
    } finally {
      ReadOptions.destroy(readOptions);
    }
  }

  @Test
  public void test10() {
    ReadOptions wp1 = new ReadOptions();
    wp1.setAutoPrefixMode(RandomUtils.nextBoolean());
    MemorySegment readOptions = wp1.to();
    try {
      checkValues(wp1, readOptions);
    } finally {
      ReadOptions.destroy(readOptions);
    }
  }

  @Test
  public void test11() {
    ReadOptions wp1 = new ReadOptions();
    wp1.setIgnoreRangeDeletions(RandomUtils.nextBoolean());
    MemorySegment readOptions = wp1.to();
    try {
      checkValues(wp1, readOptions);
    } finally {
      ReadOptions.destroy(readOptions);
    }
  }

  @Test
  public void test12() {
    ReadOptions wp1 = new ReadOptions();
    wp1.setOptimizeMultiGetForIO(RandomUtils.nextBoolean());
    MemorySegment readOptions = wp1.to();
    try {
      checkValues(wp1, readOptions);
    } finally {
      ReadOptions.destroy(readOptions);
    }
  }

  @Test
  public void test13() {
    ReadOptions wp1 = new ReadOptions();
    wp1.setPinData(RandomUtils.nextBoolean());
    MemorySegment readOptions = wp1.to();
    try {
      checkValues(wp1, readOptions);
    } finally {
      ReadOptions.destroy(readOptions);
    }
  }

  @Test
  public void test14() {
    ReadOptions wp1 = new ReadOptions();
    wp1.setPrefixSameAsStart(RandomUtils.nextBoolean());
    MemorySegment readOptions = wp1.to();
    try {
      checkValues(wp1, readOptions);
    } finally {
      ReadOptions.destroy(readOptions);
    }
  }

  @Test
  public void test15() {
    ReadOptions wp1 = new ReadOptions();
    wp1.setTailing(RandomUtils.nextBoolean());
    MemorySegment readOptions = wp1.to();
    try {
      checkValues(wp1, readOptions);
    } finally {
      ReadOptions.destroy(readOptions);
    }
  }

  @Test
  public void test16() {
    ReadOptions wp1 = new ReadOptions();
    wp1.setTotalOrderSeek(RandomUtils.nextBoolean());
    MemorySegment readOptions = wp1.to();
    try {
      checkValues(wp1, readOptions);
    } finally {
      ReadOptions.destroy(readOptions);
    }
  }

  @Test
  public void test17() {
    ReadOptions wp1 = new ReadOptions();
    wp1.setVerifyChecksums(RandomUtils.nextBoolean());
    MemorySegment readOptions = wp1.to();
    try {
      checkValues(wp1, readOptions);
    } finally {
      ReadOptions.destroy(readOptions);
    }
  }

  @Test
  public void test18() {
    ReadOptions wp1 = new ReadOptions();
    wp1.setManaged(RandomUtils.nextBoolean());
    MemorySegment readOptions = wp1.to();
    try {
      checkValues(wp1, readOptions);
    } finally {
      ReadOptions.destroy(readOptions);
    }
  }

  @Test
  public void test19() {
    ReadOptions wp1 = new ReadOptions();
    wp1.setIoActivity(enumType(RandomUtils.nextInt(0, 4), IOActivity.class));
    MemorySegment readOptions = wp1.to();
    try {
      checkValues(wp1, readOptions);
    } finally {
      ReadOptions.destroy(readOptions);
    }
  }

  @Test
  public void test20() {
    ReadOptions wp1 = new ReadOptions();
    wp1.setRateLimiterPriority(enumType(RandomUtils.nextInt(0, 5), IOPriority.class));
    MemorySegment readOptions = wp1.to();
    try {
      checkValues(wp1, readOptions);
    } finally {
      ReadOptions.destroy(readOptions);
    }
  }

  @Test
  public void test21() {
    ReadOptions wp1 = new ReadOptions();
    wp1.setValueSizeSoftLimit(RandomUtils.nextLong());
    MemorySegment readOptions = wp1.to();
    try {
      checkValues(wp1, readOptions);
    } finally {
      ReadOptions.destroy(readOptions);
    }
  }

  @Test
  public void test22() {
    ReadOptions wp1 = new ReadOptions();
    MemorySegment snapshot = rocksDb.createSnapshot();
    wp1.setSnapshot(snapshot);
    MemorySegment readOptions = wp1.to();
    try {
      checkValues(wp1, readOptions);
    } finally {
      ReadOptions.destroy(readOptions);
      rocksDb.releaseSnapshot(snapshot);
    }
  }

  @Test
  public void test23() {
    MemorySegment snapshot = rocksDb.createSnapshot();
    ReadOptions wp1 = new ReadOptions();
    wp1.setAutoPrefixMode(RandomUtils.nextBoolean());
    wp1.setTailing(RandomUtils.nextBoolean());
    wp1.setManaged(RandomUtils.nextBoolean());
    wp1.setPinData(RandomUtils.nextBoolean());
    wp1.setSnapshot(snapshot);
    wp1.setBackgroundPurgeOnIteratorCleanup(RandomUtils.nextBoolean());
    wp1.setFillCache(RandomUtils.nextBoolean());
    wp1.setTotalOrderSeek(RandomUtils.nextBoolean());
    wp1.setVerifyChecksums(RandomUtils.nextBoolean());
    wp1.setValueSizeSoftLimit(RandomUtils.nextLong());
    wp1.setRateLimiterPriority(enumType(RandomUtils.nextInt(0, 5), IOPriority.class));
    wp1.setIoActivity(enumType(RandomUtils.nextInt(0, 4), IOActivity.class));
    wp1.setDeadline(RandomUtils.nextLong());
    wp1.setOptimizeMultiGetForIO(RandomUtils.nextBoolean());
    MemorySegment readOptions = wp1.to();
    try {
      checkValues(wp1, readOptions);
    } finally {
      ReadOptions.destroy(readOptions);
      rocksDb.releaseSnapshot(snapshot);
    }
  }

  private static void checkValues(ReadOptions wp1, MemorySegment wp2) {
    assertEquals(wp1.getReadAheadSize(), ReadOptions.readAheadSize(wp2));
    assertEquals(
        wp1.isBackgroundPurgeOnIteratorCleanup(),
        ReadOptions.backgroundPurgeOnIteratorCleanup(wp2));
    assertEquals(wp1.isIgnoreRangeDeletions(), ReadOptions.ignoreRangeDeletions(wp2));
    assertEquals(wp1.isVerifyChecksums(), ReadOptions.verifyChecksums(wp2));
    assertEquals(wp1.getValueSizeSoftLimit(), ReadOptions.valueSizeSoftLimit(wp2));
    assertEquals(wp1.isAdaptiveReadAhead(), ReadOptions.adaptiveReadAhead(wp2));
    assertEquals(wp1.isAsyncIO(), ReadOptions.asyncIO(wp2));
    assertEquals(wp1.isAutoPrefixMode(), ReadOptions.autoPrefixMode(wp2));
    assertEquals(wp1.isFillCache(), ReadOptions.fillCache(wp2));
    assertEquals(wp1.isOptimizeMultiGetForIO(), ReadOptions.optimizeMultiGetForIO(wp2));
    assertEquals(wp1.isTailing(), ReadOptions.tailing(wp2));
    assertEquals(wp1.isPrefixSameAsStart(), ReadOptions.prefixSameAsStart(wp2));
    assertEquals(wp1.getIoTimeout(), rocksdb_readoptions_get_io_timeout(wp2));
    assertEquals(wp1.getDeadline(), rocksdb_readoptions_get_deadline(wp2));
    assertEquals(wp1.isManaged(), ReadOptions.managed(wp2));
    assertEquals(wp1.getSnapshot(), ReadOptions.snapshot(wp2));
    //    assertEquals(wp1.getTableFilter(), ReadOptions.tableFilter(wp2));
    assertEquals(wp1.getIterStartTs(), ReadOptions.iterStartTs(wp2));
    assertEquals(wp1.getTimestamp(), ReadOptions.timestamp(wp2));
    assertEquals(wp1.getRateLimiterPriority(), ReadOptions.rateLimiterPriority(wp2));
    assertEquals(wp1.getIterateLowerBound(), ReadOptions.iterateLowerBound(wp2));
    assertEquals(wp1.getIterateUpperBound(), ReadOptions.iterateUpperBound(wp2));
    assertEquals(wp1.getReadTier(), ReadOptions.readTier(wp2));
    assertEquals(wp1.isPinData(), ReadOptions.pinData(wp2));
    assertEquals(wp1.isTotalOrderSeek(), ReadOptions.totalOrderSeek(wp2));
    assertEquals(wp1.getMaxSkippableInternalKeys(), ReadOptions.maxSkippableInternalKeys(wp2));
    assertEquals(wp1.getIoActivity(), ReadOptions.ioActivity(wp2));
  }
}
