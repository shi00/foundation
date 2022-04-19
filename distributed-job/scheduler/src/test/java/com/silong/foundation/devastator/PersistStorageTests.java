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
package com.silong.foundation.devastator;

import com.silong.foundation.devastator.config.PersistStorageConfig;
import com.silong.foundation.devastator.core.RocksDBPersistStorage;
import com.silong.foundation.devastator.utils.KvPair;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 持久化存储单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-19 21:49
 */
public class PersistStorageTests {

  private static final PersistStorageConfig config;

  private static final PersistStorage persistStorage;

  private static final String JOBS = "jobs";

  static {
    config = new PersistStorageConfig();
    config.columnFamilyNames(List.of(JOBS)).persistDataPath("./target/devastator-data/");
    persistStorage = new RocksDBPersistStorage(config);
  }

  @Test
  @DisplayName("put")
  void test1() {
    byte[] key = RandomStringUtils.random(100).getBytes(StandardCharsets.UTF_8);
    byte[] value = RandomStringUtils.random(100).getBytes(StandardCharsets.UTF_8);
    persistStorage.put(key, value);
    byte[] bytes = persistStorage.get(key);
    Assertions.assertArrayEquals(value, bytes);
  }

  @Test
  @DisplayName("putAll")
  void test11() {
    List<KvPair<byte[], byte[]>> kvPairs = new ArrayList<>();
    for (int i = 0; i < 1000; i++) {
      byte[] key = RandomStringUtils.random(100).getBytes(StandardCharsets.UTF_8);
      byte[] value = RandomStringUtils.random(100).getBytes(StandardCharsets.UTF_8);
      kvPairs.add(new KvPair<>(key, value));
    }
    persistStorage.putAll(kvPairs);

    kvPairs.forEach(
        kvPair -> Assertions.assertArrayEquals(kvPair.value(), persistStorage.get(kvPair.key())));
  }

  @Test
  @DisplayName("putAll-jobs")
  void test111() {
    List<KvPair<byte[], byte[]>> kvPairs = new ArrayList<>();
    for (int i = 0; i < 1000; i++) {
      byte[] key = RandomStringUtils.random(100).getBytes(StandardCharsets.UTF_8);
      byte[] value = RandomStringUtils.random(100).getBytes(StandardCharsets.UTF_8);
      kvPairs.add(new KvPair<>(key, value));
    }
    persistStorage.putAll(JOBS, kvPairs);

    kvPairs.forEach(
        kvPair ->
            Assertions.assertArrayEquals(kvPair.value(), persistStorage.get(JOBS, kvPair.key())));
  }

  @Test
  @DisplayName("put-jobs")
  void test2() {
    byte[] key = RandomStringUtils.random(100).getBytes(StandardCharsets.UTF_8);
    byte[] value = RandomStringUtils.random(100).getBytes(StandardCharsets.UTF_8);
    persistStorage.put(JOBS, key, value);
    byte[] bytes = persistStorage.get(JOBS, key);
    Assertions.assertArrayEquals(value, bytes);
  }

  @Test
  @DisplayName("remove")
  void test3() {
    byte[] key = RandomStringUtils.random(100).getBytes(StandardCharsets.UTF_8);
    byte[] value = RandomStringUtils.random(100).getBytes(StandardCharsets.UTF_8);
    persistStorage.put(key, value);
    persistStorage.remove(key);
    byte[] bytes = persistStorage.get(key);
    Assertions.assertNull(bytes);
  }

  @Test
  @DisplayName("remove-jobs")
  void test4() {
    byte[] key = RandomStringUtils.random(100).getBytes(StandardCharsets.UTF_8);
    byte[] value = RandomStringUtils.random(100).getBytes(StandardCharsets.UTF_8);
    persistStorage.put(JOBS, key, value);
    persistStorage.remove(JOBS, key);
    byte[] bytes = persistStorage.get(JOBS, key);
    Assertions.assertNull(bytes);
  }
}
