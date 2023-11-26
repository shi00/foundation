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

package com.silong.foundation.dj.longhaul;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.silong.foundation.dj.longhaul.config.PersistStorageProperties;
import com.silong.foundation.dj.longhaul.serialize.Int2BytesSerializer;
import com.silong.foundation.dj.longhaul.serialize.Long2BytesSerializer;
import com.silong.foundation.dj.longhaul.serialize.String2BytesSerializer;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import net.datafaker.Faker;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 测试用例
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-26 13:48
 */
public class SerializerTests {

  private RocksDbPersistStorage storage;

  private static final Faker FAKER = new Faker();

  @Test
  void test1() {
    for (int i = 0; i < 10000; i++) {
      int a = RandomUtils.nextInt();
      a = a * (a % 2 == 0 ? 1 : -1);
      byte[] bytes = Int2BytesSerializer.INSTANCE.to(a);
      Integer b = Int2BytesSerializer.INSTANCE.from(bytes);
      Assertions.assertEquals(a, b);
    }
  }

  @Test
  void test2() {
    for (int i = 0; i < 10000; i++) {
      long a = RandomUtils.nextLong();
      a = a * (a % 2 == 0 ? 1 : -1);
      byte[] bytes = Long2BytesSerializer.INSTANCE.to(a);
      Long b = Long2BytesSerializer.INSTANCE.from(bytes);
      Assertions.assertEquals(a, b);
    }
  }

  @Test
  void test3() {
    for (int i = 0; i < 10000; i++) {
      String a = RandomStringUtils.random(RandomUtils.nextInt(0, 1024));
      byte[] bytes = String2BytesSerializer.INSTANCE.to(a);
      String b = String2BytesSerializer.INSTANCE.from(bytes);
      Assertions.assertEquals(a, b);
    }
  }

  @Test
  @SneakyThrows
  void test4() {
    var key = new com.silong.foundation.dj.longhaul.SerializerTests.B(1111);
    var value =
        new com.silong.foundation.dj.longhaul.SerializerTests.A(RandomStringUtils.random(512));
    var aClass = com.silong.foundation.dj.longhaul.SerializerTests.A.class;
    storage.put(key, value);
    A a = storage.get(key, aClass);
    Assertions.assertEquals(a, value);
  }

  @Test
  @SneakyThrows
  void test5() {
    var key = new com.silong.foundation.dj.longhaul.SerializerTests.B(2312312);
    var value =
        new com.silong.foundation.dj.longhaul.SerializerTests.A(RandomStringUtils.random(1028));
    var aClass = com.silong.foundation.dj.longhaul.SerializerTests.A.class;
    storage.put(key, value);
    A a = storage.get(key, aClass);
    Assertions.assertEquals(a, value);

    storage.remove(key);
    a = storage.get(key, aClass);
    Assertions.assertNull(a);
  }

  @BeforeEach
  void init() {
    PersistStorageProperties config = new PersistStorageProperties();
    List<String> cls = new ArrayList<>(1000);
    for (int i = 0; i < 1000; i++) {
      cls.add(FAKER.animal().name());
    }
    config.setColumnFamilyNames(cls);
    config.setPersistDataPath(String.format("./target/%s-data/", FAKER.ancient().titan()));
    storage = new RocksDbPersistStorage(config, false);
  }

  @AfterEach
  void cleanup() {
    if (storage != null) {
      storage.close();
    }
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  private static class B implements TypeConverter<B> {
    private long aLong;

    @Override
    public byte[] serialize() {
      return Long2BytesSerializer.INSTANCE.to(aLong);
    }

    @Override
    public B deserialize(byte[] bytes) {
      if (bytes == null) {
        return null;
      }
      return new B(Long2BytesSerializer.INSTANCE.from(bytes));
    }
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  private static class A implements TypeConverter<A> {
    private String str;

    @Override
    public byte[] serialize() {
      return str.getBytes(UTF_8);
    }

    @Override
    public A deserialize(byte[] bytes) {
      if (bytes == null) {
        return null;
      }
      return new A(new String(bytes, UTF_8));
    }
  }
}
