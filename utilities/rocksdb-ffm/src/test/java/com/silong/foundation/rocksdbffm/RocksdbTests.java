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
import static java.lang.foreign.MemorySegment.NULL;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.apache.commons.lang3.RandomUtils.nextInt;

import com.google.common.primitives.Longs;
import com.silong.foundation.common.lambda.Tuple2;
import com.silong.foundation.rocksdbffm.config.ColumnFamilyConfig;
import com.silong.foundation.rocksdbffm.config.RocksDbConfig;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
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

  public static final String BEFORE_CF = "20231206";

  public static final List<String> CFS =
      List.of(
          "default",
          "Helios",
          "Atlas",
          "Iapetus",
          BEFORE_CF,
          "Styx",
          "Eos",
          "Perses",
          "Epimetheus",
          "Coeus",
          "Menoetius",
          "Clymene",
          "Theia",
          NOW_CF,
          "Eurynome");

  private RocksDbImpl rocksDb;

  private RocksDbConfig config = getConfig();

  private RocksDbConfig getConfig() {
    RocksDbConfig config = new RocksDbConfig();
    List<ColumnFamilyConfig> columns = new LinkedList<>();
    CFS.forEach(
        cfn ->
            columns.add(
                new ColumnFamilyConfig(cfn, Duration.of(nextInt(0, 100000000), SECONDS), null)));
    config.setColumnFamilyConfigs(columns);
    config.setPersistDataPath(
        Paths.get(System.getProperty("user.dir"))
            .resolve("target")
            .resolve("rocksdb-test-data")
            .toFile()
            .getAbsolutePath());
    return config;
  }

  @BeforeEach
  void init() {
    rocksDb = (RocksDbImpl) RocksDb.getInstance(config);
  }

  @AfterEach
  void cleanUp() throws RocksDbException {
    rocksDb.dropColumnFamily(NOW_CF);
    rocksDb.dropColumnFamily(BEFORE_CF);
    rocksDb.close();
  }

  @Test
  public void test1() {
    RocksDbConfig config = new RocksDbConfig();
    List<ColumnFamilyConfig> columns = new LinkedList<>();
    CFS.forEach(
        cfn ->
            columns.add(
                new ColumnFamilyConfig(cfn, Duration.of(nextInt(0, 100000000), SECONDS), null)));
    IntStream.range(0, 5)
        .forEach(
            i ->
                columns.add(
                    new ColumnFamilyConfig(
                        FAKER.app().name(), Duration.of(nextInt(0, 100000000), SECONDS), null)));
    config.setColumnFamilyConfigs(columns);
    config.setPersistDataPath(
        Paths.get(System.getProperty("user.dir"))
            .resolve("target")
            .resolve("rocksdb-test1-data")
            .toFile()
            .getAbsolutePath());
    try (RocksDbImpl rocksDb = (RocksDbImpl) RocksDb.getInstance(config)) {
      Assertions.assertTrue(rocksDb.isOpen());
    }
  }

  @Test
  public void test2() throws RocksDbException {
    Assertions.assertTrue(rocksDb.isOpen());
    String cf = "aa";
    rocksDb.createColumnFamily(cf);
    Assertions.assertTrue(rocksDb.isColumnFamilyExist(cf));
    rocksDb.dropColumnFamily(cf);
    Assertions.assertFalse(rocksDb.isColumnFamilyExist(cf));
  }

  @Test
  public void test3() throws RocksDbException {
    Assertions.assertTrue(rocksDb.isOpen());
    String k = "a";
    String v = "b";
    rocksDb.put(DEFAULT_COLUMN_FAMILY_NAME, k.getBytes(UTF_8), v.getBytes(UTF_8));
    byte[] vbs = rocksDb.get(DEFAULT_COLUMN_FAMILY_NAME, k.getBytes(UTF_8));
    Assertions.assertArrayEquals(vbs, v.getBytes(UTF_8));
  }

  @Test
  public void test4() throws RocksDbException {
    Assertions.assertTrue(rocksDb.isOpen());
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
  public void test5() throws RocksDbException {
    Assertions.assertTrue(rocksDb.isOpen());
    for (int i = 0; i < 1000; i++) {
      byte[] key = String.valueOf(i).getBytes(UTF_8);
      byte[] val = RandomStringUtils.random(1024).getBytes(UTF_8);
      rocksDb.put(key, val);
    }

    byte[] startKey = String.valueOf(0).getBytes(UTF_8);
    byte[] endKey = String.valueOf(999).getBytes(UTF_8);
    rocksDb.deleteRange(startKey, endKey);

    byte[] key = String.valueOf(nextInt(0, 1000)).getBytes(UTF_8);
    byte[] bytes = rocksDb.get(key);
    Assertions.assertNull(bytes);
  }

  @Test
  public void test6() throws RocksDbException {
    Assertions.assertTrue(rocksDb.isOpen());
    HashMap<String, String> map = new HashMap<>();
    for (int i = 0; i < 10; i++) {
      String k = RandomStringUtils.random(512);
      byte[] key = k.getBytes(UTF_8);
      String v = RandomStringUtils.random(1024);
      byte[] val = v.getBytes(UTF_8);
      rocksDb.put(NOW_CF, key, val);
      map.put(k, v);
    }
    try (RocksDbIterator iterator = rocksDb.iterator(NOW_CF)) {
      int count = 0;
      for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
        Tuple2<byte[], byte[]> tuple2 = iterator.get();
        String val = map.get(new String(tuple2.t1(), UTF_8));
        Assertions.assertArrayEquals(val.getBytes(UTF_8), tuple2.t2());
        count++;
      }
      iterator.checkStatus();
      Assertions.assertEquals(10, count);
    }
  }

  @Test
  public void test7() throws RocksDbException {
    Assertions.assertTrue(rocksDb.isOpen());
    HashMap<String, String> map = new HashMap<>();
    for (int i = 0; i < 50; i++) {
      String k = RandomStringUtils.random(512);
      byte[] key = k.getBytes(UTF_8);
      String v = RandomStringUtils.random(1024);
      byte[] val = v.getBytes(UTF_8);
      if (i >= 20) {
        rocksDb.put(BEFORE_CF, key, val);
      } else {
        rocksDb.put(NOW_CF, key, val);
      }
      map.put(k, v);
    }

    try (RocksDbIterator iterator1 = rocksDb.iterator(BEFORE_CF);
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

  @Test
  public void test8() throws RocksDbException {
    byte[] a = "a".getBytes(UTF_8);
    byte[] b = "b".getBytes(UTF_8);
    byte[] c = "c".getBytes(UTF_8);
    rocksDb.put(NOW_CF, a, a);
    rocksDb.put(NOW_CF, b, b);
    rocksDb.put(NOW_CF, c, c);

    try (RocksDbIterator iterator = rocksDb.iterator(NOW_CF)) {
      iterator.seekForPrev("a1".getBytes(UTF_8));
      Assertions.assertTrue(iterator.isValid());
      Tuple2<byte[], byte[]> tuple2 = iterator.get();
      Assertions.assertArrayEquals(a, tuple2.t2());
      Assertions.assertArrayEquals(a, tuple2.t1());

      iterator.next();
      Assertions.assertTrue(iterator.isValid());
      tuple2 = iterator.get();
      Assertions.assertArrayEquals(b, tuple2.t2());
      Assertions.assertArrayEquals(b, tuple2.t1());

      iterator.checkStatus();
    }
  }

  @Test
  public void test9() {
    RocksDbConfig config = new RocksDbConfig();
    config.setPersistDataPath(
        Paths.get(FAKER.ancient().god())
            .resolve("target")
            .resolve(FAKER.animal().name())
            .toFile()
            .getAbsolutePath());
    List<ColumnFamilyConfig> columns = new LinkedList<>();
    IntStream.range(0, nextInt(1, 10))
        .forEach(
            i ->
                columns.add(
                    new ColumnFamilyConfig(
                        FAKER.ancient().titan(),
                        Duration.of(nextInt(0, 100000000), SECONDS),
                        null)));
    config.setColumnFamilyConfigs(columns);
    try (RocksDbImpl rocksDb = (RocksDbImpl) RocksDb.getInstance(config)) {
      Assertions.assertFalse(rocksDb.isOpen());
    }
  }

  @Test
  public void test10() throws RocksDbException {
    byte[] a = "a".getBytes(UTF_8);
    byte[] b = "b".getBytes(UTF_8);
    byte[] c = "c".getBytes(UTF_8);
    rocksDb.put(NOW_CF, a, a);
    rocksDb.put(NOW_CF, b, b);
    rocksDb.put(NOW_CF, c, c);

    List<Tuple2<byte[], byte[]>> tuple2s = rocksDb.multiGet(NOW_CF, a, b, c);
    Assertions.assertEquals(3, tuple2s.size());
    Assertions.assertArrayEquals(a, tuple2s.get(0).t2());
    Assertions.assertArrayEquals(b, tuple2s.get(1).t2());
    Assertions.assertArrayEquals(c, tuple2s.get(2).t2());
  }

  @Test
  public void test11() {
    Collection<String> list = rocksDb.openedColumnFamilies();
    Assertions.assertTrue(CFS.containsAll(list));
  }

  @Test
  public void test12() throws RocksDbException {
    List<Tuple2<String, String>> list =
        IntStream.range(0, nextInt(1, 100))
            .mapToObj(
                i ->
                    new Tuple2<>(
                        RandomStringUtils.randomAlphabetic(128), RandomStringUtils.random(512)))
            .toList();

    rocksDb.putAll(
        NOW_CF,
        list.stream()
            .map(t -> new Tuple2<>(t.t1().getBytes(UTF_8), t.t2().getBytes(UTF_8)))
            .toArray(Tuple2[]::new));

    List<Tuple2<byte[], byte[]>> tuple2s =
        rocksDb.multiGet(
            NOW_CF, list.stream().map(t -> t.t1().getBytes(UTF_8)).toArray(byte[][]::new));

    for (Tuple2<String, String> t : list) {
      boolean condition =
          tuple2s.stream()
              .anyMatch(
                  p ->
                      t.t1().equals(new String(p.t1(), UTF_8))
                          && t.t2().equals(new String(p.t2(), UTF_8)));
      Assertions.assertTrue(condition);
    }
  }

  @Test
  public void test13() throws RocksDbException {
    byte[] a = "aaa".getBytes(UTF_8);
    byte[] b = "bbb".getBytes(UTF_8);
    byte[] c = "ccc".getBytes(UTF_8);
    rocksDb.put(BEFORE_CF, a, a);
    rocksDb.put(BEFORE_CF, b, b);
    rocksDb.put(BEFORE_CF, c, c);

    try (RocksDbIterator iterator = rocksDb.iterator(BEFORE_CF)) {
      iterator.seek("ccc".getBytes(UTF_8));
      Assertions.assertTrue(iterator.isValid());
      Tuple2<byte[], byte[]> tuple2 = iterator.get();
      Assertions.assertArrayEquals(c, tuple2.t2());
      Assertions.assertArrayEquals(c, tuple2.t1());
      Assertions.assertDoesNotThrow(iterator::checkStatus);
    }
  }

  @Test
  public void test14() throws RocksDbException {
    byte[] a = "aaa".getBytes(UTF_8);
    byte[] b = "bbb".getBytes(UTF_8);
    byte[] c = "ccc".getBytes(UTF_8);
    rocksDb.put(a, a);
    rocksDb.put(b, b);
    rocksDb.put(c, c);

    try (RocksDbIterator iterator = rocksDb.iterator()) {
      iterator.seek("ccc".getBytes(UTF_8));
      Assertions.assertTrue(iterator.isValid());
      Tuple2<byte[], byte[]> tuple2 = iterator.get();
      Assertions.assertArrayEquals(c, tuple2.t2());
      Assertions.assertArrayEquals(c, tuple2.t1());
      Assertions.assertDoesNotThrow(iterator::checkStatus);
    }
  }

  @Test
  public void test15() throws RocksDbException, InterruptedException {
    String cf = "yoyo";
    rocksDb.createColumnFamily(
        cf,
        new RocksDbComparator() {
          @Override
          public void release() {}

          @Override
          public int compare(byte[] a, byte[] b) {
            return Long.compare(Longs.fromByteArray(a), Longs.fromByteArray(b));
          }

          @Override
          public String name() {
            return "test-comparator";
          }
        });

    try {
      List<byte[]> keys = new LinkedList<>();
      for (int i = 0; i < 1000; i++) {
        long now = System.currentTimeMillis();
        byte[] array = Longs.toByteArray(now);
        rocksDb.put(cf, array, RandomStringUtils.random(24).getBytes(UTF_8));
        keys.add(array);
        Thread.sleep(1);
      }

      List<byte[]> result = new LinkedList<>();
      try (RocksDbIterator iterator = rocksDb.iterator(cf)) {
        for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
          Tuple2<byte[], byte[]> tuple2 = iterator.get();
          result.add(tuple2.t1());
        }
        iterator.checkStatus();
      }

      Assertions.assertEquals(keys.size(), result.size());
      for (int i = 0; i < keys.size(); i++) {
        long t = Longs.fromByteArray(result.get(i));
        Assertions.assertEquals(Longs.fromByteArray(keys.get(i)), t);
        System.out.println(t);
      }
    } finally {
      rocksDb.dropColumnFamily(cf);
    }
  }

  @Test
  public void test16() throws RocksDbException {
    byte[] a = "a".getBytes(UTF_8);
    byte[] b = "b".getBytes(UTF_8);
    byte[] c = "c".getBytes(UTF_8);
    rocksDb.put(a, a);
    rocksDb.put(b, b);
    rocksDb.put(c, c);

    List<Tuple2<byte[], byte[]>> tuple2s = rocksDb.multiGet(a, b, c);
    Assertions.assertEquals(3, tuple2s.size());
    Assertions.assertArrayEquals(a, tuple2s.get(0).t2());
    Assertions.assertArrayEquals(b, tuple2s.get(1).t2());
    Assertions.assertArrayEquals(c, tuple2s.get(2).t2());
  }

  @Test
  public void test17() {
    RocksDbConfig config = new RocksDbConfig();
    MemorySegment rocksdbOptions = rocksDb.createRocksdbOptions(config);
    Assertions.assertNotEquals(NULL, rocksdbOptions);
    Assertions.assertDoesNotThrow(() -> rocksDb.destroyRocksdbOptions(rocksdbOptions));
  }

  @Test
  public void test18() {
    try (Arena arena = Arena.ofConfined()) {
      MemorySegment rocksdbOptions = rocksDb.createRocksdbOptions(config);
      List<String> cfns =
          rocksDb.listExistColumnFamilies(
              rocksdbOptions, arena.allocateUtf8String(config.getPersistDataPath()));
      rocksDb.destroyRocksdbOptions(rocksdbOptions);
      Assertions.assertEquals(CFS.stream().sorted().toList(), cfns.stream().sorted().toList());
    }
  }
}
