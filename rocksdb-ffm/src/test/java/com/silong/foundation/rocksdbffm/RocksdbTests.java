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

import static com.silong.foundation.rocksdbffm.RocksDb.DEFAULT_COLUMN_FAMILY_NAME;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.apache.commons.lang3.RandomUtils.nextInt;

import com.silong.foundation.common.lambda.Tuple2;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;
import net.datafaker.Faker;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-12-02 11:01
 */
public class RocksdbTests {

  private static final Faker FAKER = new Faker();
  public static final String NOW_CF = "20231207";

  private final RocksDbConfig config = new RocksDbConfig();

  private RocksDbImpl rocksDb;

  @BeforeEach
  void init() {
    config.setPersistDataPath(
        Paths.get(System.getProperty("user.dir"))
            .resolve("target")
            .resolve(FAKER.animal().name())
            .toFile()
            .getAbsolutePath());
    Map<String, Duration> columns = new HashMap<>();
    columns.put(NOW_CF, Duration.ZERO);
    IntStream.range(0, nextInt(1, 10))
        .forEach(
            i -> columns.put(FAKER.ancient().titan(), Duration.of(nextInt(0, 100000000), SECONDS)));
    config.setColumnFamilyNameWithTTL(columns);
    rocksDb = (RocksDbImpl) RocksDb.getInstance(config);
  }

  @AfterEach
  void cleanUp() {
    rocksDb.close();
  }

  @Test
  public void test1() {
    config.setPersistDataPath(
        Paths.get(FAKER.ancient().god())
            .resolve("target")
            .resolve(FAKER.animal().name())
            .toFile()
            .getAbsolutePath());
    Map<String, Duration> columns = new HashMap<>();
    IntStream.range(0, nextInt(1, 10))
        .forEach(
            i -> columns.put(FAKER.ancient().titan(), Duration.of(nextInt(0, 100000000), SECONDS)));
    config.setColumnFamilyNameWithTTL(columns);
    try (RocksDbImpl rocksDb = (RocksDbImpl) RocksDb.getInstance(config)) {
      Assertions.assertFalse(rocksDb.isOpen());
    }
  }

  @Test
  public void test2() {
    String cf = "aa";
    Assertions.assertTrue(rocksDb.createColumnFamily(cf));
    rocksDb.dropColumnFamily(cf);
    Assertions.assertFalse(rocksDb.isColumnFamilyExist(cf));
  }

  @Test
  public void test3() {
    String k = "a";
    String v = "b";
    rocksDb.put(DEFAULT_COLUMN_FAMILY_NAME, k.getBytes(UTF_8), v.getBytes(UTF_8));
    byte[] vbs = rocksDb.get(DEFAULT_COLUMN_FAMILY_NAME, k.getBytes(UTF_8));
    Assertions.assertArrayEquals(vbs, v.getBytes(UTF_8));
  }

  @Test
  public void test4() {
    byte[] key = RandomStringUtils.random(1024).getBytes(UTF_8);
    byte[] val = RandomStringUtils.random(1024).getBytes(UTF_8);
    rocksDb.put(DEFAULT_COLUMN_FAMILY_NAME, key, val);
    byte[] bytes = rocksDb.get(key);
    Assertions.assertArrayEquals(bytes, val);
    rocksDb.delete(key);
    bytes = rocksDb.get(key);
    Assertions.assertNull(bytes);
  }

  @Test
  public void test5() {
    IntStream.range(0, 1000)
        .forEach(
            i -> {
              byte[] key = String.valueOf(i).getBytes(UTF_8);
              byte[] val = RandomStringUtils.random(1024).getBytes(UTF_8);
              rocksDb.put(key, val);
            });

    byte[] startKey = String.valueOf(0).getBytes(UTF_8);
    byte[] endKey = String.valueOf(999).getBytes(UTF_8);
    rocksDb.deleteRange(startKey, endKey);

    byte[] key = String.valueOf(nextInt(0, 1000)).getBytes(UTF_8);
    byte[] bytes = rocksDb.get(key);
    Assertions.assertNull(bytes);
  }

  @Test
  public void test6() throws RocksDbException {
    HashMap<String, String> map = new HashMap<>();
    IntStream.range(0, 50)
        .forEach(
            i -> {
              String k = RandomStringUtils.random(512);
              byte[] key = k.getBytes(UTF_8);
              String v = RandomStringUtils.random(1024);
              byte[] val = v.getBytes(UTF_8);
              rocksDb.put(key, val);
              map.put(k, v);
            });
    try (RocksDbIterator iterator = rocksDb.iterator()) {
      int count = 0;
      for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
        Tuple2<byte[], byte[]> tuple2 = iterator.get();
        String val = map.get(new String(tuple2.t1(), UTF_8));
        Assertions.assertArrayEquals(val.getBytes(UTF_8), tuple2.t2());
        count++;
      }
      iterator.checkStatus();
      Assertions.assertEquals(50, count);
    }
  }

  @Test
  public void test7() throws RocksDbException {
    HashMap<String, String> map = new HashMap<>();
    IntStream.range(0, 50)
        .forEach(
            i -> {
              String k = RandomStringUtils.random(512);
              byte[] key = k.getBytes(UTF_8);
              String v = RandomStringUtils.random(1024);
              byte[] val = v.getBytes(UTF_8);
              if (i >= 20) {
                rocksDb.put(key, val);
              } else {
                rocksDb.put(NOW_CF, key, val);
              }
              map.put(k, v);
            });

    try (RocksDbIterator iterator1 = rocksDb.iterator();
        RocksDbIterator iterator2 = rocksDb.iterator(NOW_CF)) {
      int count = 0;
      for (iterator1.seekToLast(); iterator1.isValid(); iterator1.prev()) {
        Tuple2<byte[], byte[]> tuple2 = iterator1.get();
        String val = map.get(new String(tuple2.t1(), UTF_8));
        Assertions.assertArrayEquals(val.getBytes(UTF_8), tuple2.t2());
        count++;
      }
      iterator1.checkStatus();

      for (iterator2.seekToFirst(); iterator2.isValid(); iterator2.next()) {
        Tuple2<byte[], byte[]> tuple2 = iterator2.get();
        String val = map.get(new String(tuple2.t1(), UTF_8));
        Assertions.assertArrayEquals(val.getBytes(UTF_8), tuple2.t2());
        count++;
      }
      iterator2.checkStatus();
      Assertions.assertEquals(50, count);
    }
  }
}
