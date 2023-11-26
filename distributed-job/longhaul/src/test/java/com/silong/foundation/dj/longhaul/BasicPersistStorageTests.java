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
package com.silong.foundation.dj.longhaul;

import static com.silong.foundation.dj.longhaul.RocksDbPersistStorage.DEFAULT_COLUMN_FAMILY_NAME;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.silong.foundation.common.lambda.Tuple2;
import com.silong.foundation.dj.longhaul.config.PersistStorageProperties;
import com.silong.foundation.dj.longhaul.config.PersistStorageProperties.DataScale;
import com.silong.foundation.dj.longhaul.serialize.Long2BytesSerializer;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import lombok.SneakyThrows;
import net.datafaker.Faker;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.*;

/**
 * 持久化存储单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-19 21:49
 */
public class BasicPersistStorageTests {

  private static final PersistStorageProperties CONFIG;

  private static final RocksDbPersistStorage PERSIST_STORAGE;

  private static final String JOBS = "jobs";

  private static final Faker FAKER = new Faker();

  private static final int LIMIT;

  static {
    CONFIG = new PersistStorageProperties();
    List<String> cls = new ArrayList<>(2048);
    LIMIT = RandomUtils.nextInt(0, 2048);
    for (int i = 0; i < LIMIT - 1; i++) {
      cls.add(FAKER.animal().name());
    }
    cls.add(JOBS);
    CONFIG.setColumnFamilyNames(cls);
    CONFIG.setPersistDataPath(String.format("./target/%s-data/", FAKER.ancient().god()));
    PERSIST_STORAGE = new RocksDbPersistStorage(CONFIG);
  }

  @AfterEach
  void clean() {
    PERSIST_STORAGE.deleteColumnFamily(JOBS);
    PERSIST_STORAGE.createColumnFamily(JOBS);
  }

  @AfterAll
  static void cleanUp() {
    PERSIST_STORAGE.close();
  }

  private static void printThreadStack(Thread thread) {
    for (StackTraceElement stackTraceElement : thread.getStackTrace()) {
      System.out.println(stackTraceElement);
    }
  }

  @Test
  @DisplayName("startBlocking")
  @SneakyThrows
  void test121() {
    PersistStorageProperties config = new PersistStorageProperties();
    config.setPersistDataPath(String.format("./target/%s-data/", FAKER.ancient().hero()));
    config.setDataScale(DataScale.BIG);
    config.setColumnFamilyNames(
        List.of(FAKER.artist().name(), DEFAULT_COLUMN_FAMILY_NAME, FAKER.artist().name()));
    AtomicReference<RocksDbPersistStorage> storage = new AtomicReference<>();
    Thread rocksdbThread =
        Thread.ofVirtual()
            .name(FAKER.funnyName().name())
            .start(() -> storage.set(new RocksDbPersistStorage(config, true)));
    Assertions.assertFalse(rocksdbThread.join(Duration.ofSeconds(3)));
    Assertions.assertNull(storage.get());
    rocksdbThread.interrupt();
  }

  @Test
  @DisplayName("startBlocking1")
  @SneakyThrows
  void test1211() {
    CountDownLatch latch = new CountDownLatch(1);
    CountDownLatch latch1 = new CountDownLatch(1);
    final PersistStorageProperties config = new PersistStorageProperties();
    config.setPersistDataPath(String.format("./target/%s-data/", FAKER.ancient().hero()));
    Thread rocksdbThread =
        Thread.ofVirtual()
            .name(FAKER.funnyName().name())
            .start(
                () -> {
                  try {
                    latch.await();
                  } catch (InterruptedException e) {
                    // 设置中断标志，异常后此标志位被清空，需要此处重置给后续使用
                    Thread.currentThread().interrupt();
                  }
                  Assertions.assertThrowsExactly(
                      IllegalStateException.class,
                      () -> {
                        try {
                          new RocksDbPersistStorage(config, true);
                        } finally {
                          latch1.countDown();
                        }
                      });
                });
    rocksdbThread.interrupt();
    latch.countDown();
    latch1.await();
  }

  @Test
  @DisplayName("ColumnFamilyExists")
  void test() {
    String aaaa = "AAAA";
    PERSIST_STORAGE.createColumnFamily(aaaa);
    Collection<String> allColumnFamilyNames = PERSIST_STORAGE.getAllColumnFamilyNames();
    Assertions.assertTrue(
        allColumnFamilyNames.contains(JOBS)
            && allColumnFamilyNames.contains(DEFAULT_COLUMN_FAMILY_NAME)
            && allColumnFamilyNames.contains(aaaa));
    PERSIST_STORAGE.deleteColumnFamily(aaaa);
  }

  @Test
  @DisplayName("put")
  void test1() {
    byte[] key = RandomStringUtils.random(100).getBytes(UTF_8);
    byte[] value = RandomStringUtils.random(100).getBytes(UTF_8);
    PERSIST_STORAGE.put(key, value);
    byte[] bytes = PERSIST_STORAGE.get(key);
    Assertions.assertArrayEquals(value, bytes);
  }

  @Test
  @DisplayName("putAll")
  void test11() {
    List<Tuple2<byte[], byte[]>> kvPairs = new ArrayList<>();
    for (int i = 0; i < 1000; i++) {
      byte[] key = RandomStringUtils.random(100).getBytes(UTF_8);
      byte[] value = RandomStringUtils.random(100).getBytes(UTF_8);
      kvPairs.add(new Tuple2<>(key, value));
    }
    PERSIST_STORAGE.putAll(kvPairs.toArray(Tuple2[]::new));

    kvPairs.forEach(
        kvPair -> Assertions.assertArrayEquals(kvPair.t2(), PERSIST_STORAGE.get(kvPair.t1())));
  }

  @Test
  @DisplayName("putAll-jobs")
  void test111() {
    List<Tuple2<byte[], byte[]>> kvPairs = new ArrayList<>();
    for (int i = 0; i < 1000; i++) {
      byte[] key = RandomStringUtils.random(100).getBytes(UTF_8);
      byte[] value = RandomStringUtils.random(100).getBytes(UTF_8);
      kvPairs.add(new Tuple2<>(key, value));
    }
    PERSIST_STORAGE.putAll(JOBS, kvPairs.toArray(Tuple2[]::new));

    kvPairs.forEach(
        kvPair ->
            Assertions.assertArrayEquals(kvPair.t2(), PERSIST_STORAGE.get(JOBS, kvPair.t1())));
  }

  @Test
  @DisplayName("put-jobs")
  void test2() {
    byte[] key = RandomStringUtils.random(100).getBytes(UTF_8);
    byte[] value = RandomStringUtils.random(100).getBytes(UTF_8);
    PERSIST_STORAGE.put(JOBS, key, value);
    byte[] bytes = PERSIST_STORAGE.get(JOBS, key);
    Assertions.assertArrayEquals(value, bytes);
  }

  @Test
  @DisplayName("remove")
  void test3() {
    byte[] key = RandomStringUtils.random(100).getBytes(UTF_8);
    byte[] value = RandomStringUtils.random(100).getBytes(UTF_8);
    PERSIST_STORAGE.put(key, value);
    PERSIST_STORAGE.remove(key);
    byte[] bytes = PERSIST_STORAGE.get(key);
    Assertions.assertNull(bytes);
  }

  @Test
  @DisplayName("remove-jobs")
  void test4() {
    byte[] key = RandomStringUtils.random(100).getBytes(UTF_8);
    byte[] value = RandomStringUtils.random(100).getBytes(UTF_8);
    PERSIST_STORAGE.put(JOBS, key, value);
    PERSIST_STORAGE.remove(JOBS, key);
    byte[] bytes = PERSIST_STORAGE.get(JOBS, key);
    Assertions.assertNull(bytes);
  }

  @Test
  @DisplayName("multiRemove-jobs")
  void test44() {
    List<Tuple2<byte[], byte[]>> kvPairs = new ArrayList<>();
    for (int i = 0; i < 1000; i++) {
      byte[] key = RandomStringUtils.random(100).getBytes(UTF_8);
      byte[] value = RandomStringUtils.random(100).getBytes(UTF_8);
      kvPairs.add(new Tuple2<>(key, value));
    }
    PERSIST_STORAGE.putAll(JOBS, kvPairs.toArray(Tuple2[]::new));
    List<byte[]> keys = kvPairs.stream().map(Tuple2::t1).toList();
    PERSIST_STORAGE.multiRemove(JOBS, keys.toArray(byte[][]::new));
    Assertions.assertTrue(
        PERSIST_STORAGE.multiGet(JOBS, keys.toArray(byte[][]::new)).stream()
            .allMatch(kvPair -> kvPair.t2() == null));
  }

  @Test
  @DisplayName("multiRemove")
  void test444() {
    List<Tuple2<byte[], byte[]>> kvPairs = new ArrayList<>();
    for (int i = 0; i < 1000; i++) {
      byte[] key = RandomStringUtils.random(100).getBytes(UTF_8);
      byte[] value = RandomStringUtils.random(100).getBytes(UTF_8);
      kvPairs.add(new Tuple2<>(key, value));
    }
    PERSIST_STORAGE.putAll(kvPairs.toArray(Tuple2[]::new));
    List<byte[]> keys = kvPairs.stream().map(Tuple2::t1).toList();
    PERSIST_STORAGE.multiRemove(keys.toArray(byte[][]::new));
    Assertions.assertTrue(
        PERSIST_STORAGE.multiGet(keys.toArray(byte[][]::new)).stream()
            .allMatch(kvPair -> kvPair.t2() == null));
  }

  @Test
  @DisplayName("multiGet")
  void test5() {
    List<Tuple2<byte[], byte[]>> kvPairs = new ArrayList<>(1000);
    for (int i = 0; i < 1000; i++) {
      byte[] key = RandomStringUtils.random(100).getBytes(UTF_8);
      byte[] value = RandomStringUtils.random(100).getBytes(UTF_8);
      kvPairs.add(new Tuple2<>(key, value));
    }
    PERSIST_STORAGE.putAll(kvPairs.toArray(Tuple2[]::new));

    List<Tuple2<byte[], byte[]>> kvPairs1 =
        PERSIST_STORAGE.multiGet(kvPairs.stream().map(Tuple2::t1).toArray(byte[][]::new));

    Assertions.assertEquals(kvPairs1.size(), kvPairs.size());

    for (int i = 0; i < kvPairs1.size(); i++) {
      Tuple2<byte[], byte[]> kvPair1 = kvPairs1.get(i);
      Tuple2<byte[], byte[]> kvPair = kvPairs.get(i);
      Assertions.assertTrue(
          Arrays.equals(kvPair1.t1(), kvPair.t1()) && Arrays.equals(kvPair1.t2(), kvPair.t2()));
    }
  }

  @Test
  @DisplayName("multiGet-jobs")
  void test55() {
    List<Tuple2<byte[], byte[]>> kvPairs = new ArrayList<>(1000);
    for (int i = 0; i < 1000; i++) {
      byte[] key = RandomStringUtils.random(100).getBytes(UTF_8);
      byte[] value = RandomStringUtils.random(100).getBytes(UTF_8);
      kvPairs.add(new Tuple2<>(key, value));
    }
    PERSIST_STORAGE.putAll(JOBS, kvPairs.toArray(Tuple2[]::new));

    List<Tuple2<byte[], byte[]>> kvPairs1 =
        PERSIST_STORAGE.multiGet(JOBS, kvPairs.stream().map(Tuple2::t1).toArray(byte[][]::new));

    Assertions.assertEquals(kvPairs1.size(), kvPairs.size());

    for (int i = 0; i < kvPairs1.size(); i++) {
      Tuple2<byte[], byte[]> kvPair1 = kvPairs1.get(i);
      Tuple2<byte[], byte[]> kvPair = kvPairs.get(i);
      Assertions.assertTrue(
          Arrays.equals(kvPair1.t1(), kvPair.t1()) && Arrays.equals(kvPair1.t2(), kvPair.t2()));
    }
  }

  @Test
  @DisplayName("create-column-family")
  void test6() {
    String columnFamilyName = "cf1";
    PERSIST_STORAGE.createColumnFamily(columnFamilyName);
    byte[] key = RandomStringUtils.random(1000).getBytes(UTF_8);
    byte[] value = RandomStringUtils.random(2000).getBytes(UTF_8);
    PERSIST_STORAGE.put(columnFamilyName, key, value);
    byte[] bytes = PERSIST_STORAGE.get(columnFamilyName, key);
    Assertions.assertArrayEquals(value, bytes);

    PERSIST_STORAGE.deleteColumnFamily(columnFamilyName);
  }

  @Test
  @DisplayName("drop-column-family")
  void test7() {
    String columnFamilyName = "cf2";
    PERSIST_STORAGE.createColumnFamily(columnFamilyName);
    byte[] key = RandomStringUtils.random(2000).getBytes(UTF_8);
    byte[] value = RandomStringUtils.random(2000).getBytes(UTF_8);
    PERSIST_STORAGE.put(columnFamilyName, key, value);
    byte[] bytes = PERSIST_STORAGE.get(columnFamilyName, key);
    Assertions.assertArrayEquals(value, bytes);

    PERSIST_STORAGE.deleteColumnFamily(columnFamilyName);

    Assertions.assertThrows(Exception.class, () -> PERSIST_STORAGE.get(columnFamilyName, key));
  }

  @Test
  @DisplayName("delete-range")
  void test8() {
    List<byte[]> keys =
        IntStream.range(0, 1000)
            .mapToObj(i -> Long2BytesSerializer.INSTANCE.to((long) i))
            .filter(Objects::nonNull)
            .toList();
    keys.forEach(key -> PERSIST_STORAGE.put(key, RandomStringUtils.random(100).getBytes(UTF_8)));
    PERSIST_STORAGE.deleteRange(keys.get(0), keys.get(keys.size() - 1));

    List<Tuple2<byte[], byte[]>> kvPairs = PERSIST_STORAGE.multiGet(keys.toArray(byte[][]::new));
    for (int i = 0; i < 999; i++) {
      Assertions.assertNull(kvPairs.get(0).t2());
    }
    Assertions.assertArrayEquals(kvPairs.get(999).t2(), PERSIST_STORAGE.get(keys.get(999)));
  }

  @Test
  @DisplayName("delete-range-jobs")
  void test88() {
    List<byte[]> keys =
        IntStream.range(0, 1000)
            .mapToObj(i -> Long2BytesSerializer.INSTANCE.to((long) i))
            .filter(Objects::nonNull)
            .toList();
    keys.forEach(
        key -> PERSIST_STORAGE.put(JOBS, key, RandomStringUtils.random(100).getBytes(UTF_8)));
    PERSIST_STORAGE.deleteRange(JOBS, keys.get(0), keys.get(keys.size() - 1));

    List<Tuple2<byte[], byte[]>> kvPairs =
        PERSIST_STORAGE.multiGet(JOBS, keys.toArray(byte[][]::new));
    for (int i = 0; i < 999; i++) {
      Assertions.assertNull(kvPairs.get(0).t2());
    }
    Assertions.assertArrayEquals(kvPairs.get(999).t2(), PERSIST_STORAGE.get(JOBS, keys.get(999)));
  }

  @Test
  @DisplayName("iterate-database-columnfamily")
  void test9() {
    List<Tuple2<byte[], byte[]>> kvPairs = new ArrayList<>(1000);
    for (int i = 0; i < 1000; i++) {
      byte[] key = RandomStringUtils.random(100).getBytes(UTF_8);
      byte[] value = RandomStringUtils.random(100).getBytes(UTF_8);
      kvPairs.add(new Tuple2<>(key, value));
    }
    PERSIST_STORAGE.putAll(JOBS, kvPairs.toArray(Tuple2[]::new));

    List<Tuple2<byte[], byte[]>> kvPairs1 = new ArrayList<>(1000);
    PERSIST_STORAGE.iterate(JOBS, (k, v) -> kvPairs1.add(new Tuple2<>(k, v)));

    Assertions.assertEquals(kvPairs1.size(), kvPairs.size());
    for (int i = 0; i < 1000; i++) {
      Tuple2<byte[], byte[]> pair = kvPairs.get(i);
      Assertions.assertTrue(
          kvPairs1.stream()
              .anyMatch(
                  pair1 ->
                      Arrays.equals(pair.t1(), pair1.t1())
                          && Arrays.equals(pair.t2(), pair1.t2())));
    }
  }

  @Test
  @DisplayName("iterate-database")
  void test99() {

    List<byte[]> keys = new ArrayList<>(1000);
    PERSIST_STORAGE.iterate(DEFAULT_COLUMN_FAMILY_NAME, (k, v) -> keys.add(k));
    if (!keys.isEmpty()) {
      PERSIST_STORAGE.multiRemove(DEFAULT_COLUMN_FAMILY_NAME, keys.toArray(byte[][]::new));
    }

    List<Tuple2<byte[], byte[]>> kvPairs = new ArrayList<>(1000);
    for (int i = 0; i < 1000; i++) {
      byte[] key = RandomStringUtils.random(100).getBytes(UTF_8);
      byte[] value = RandomStringUtils.random(100).getBytes(UTF_8);
      kvPairs.add(new Tuple2<>(key, value));
    }
    PERSIST_STORAGE.putAll(kvPairs.toArray(Tuple2[]::new));

    List<Tuple2<byte[], byte[]>> kvPairs1 = new ArrayList<>(1000);
    PERSIST_STORAGE.iterate((k, v) -> kvPairs1.add(new Tuple2<>(k, v)));

    Assertions.assertEquals(kvPairs1.size(), kvPairs.size());
    for (int i = 0; i < 1000; i++) {
      Tuple2<byte[], byte[]> pair = kvPairs.get(i);
      Assertions.assertTrue(
          kvPairs1.stream()
              .anyMatch(
                  pair1 ->
                      Arrays.equals(pair.t1(), pair1.t1())
                          && Arrays.equals(pair.t2(), pair1.t2())));
    }
  }
}
