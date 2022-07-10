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
package com.silong.foundation.devastator.core;

import com.silong.foundation.devastator.utils.TypeConverter;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.silong.foundation.devastator.utils.TypeConverter.String2Bytes.INSTANCE;

/**
 * 单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-07-10 09:34
 */
public class TypeConverterTests {
  @Test
  @DisplayName("long-to-bytes")
  void test1() {
    for (int i = 0; i < 10; i++) {
      long num = RandomUtils.nextLong();
      byte[] to = TypeConverter.Long2Bytes.INSTANCE.to(num);
      Long from = TypeConverter.Long2Bytes.INSTANCE.from(to);
      Assertions.assertEquals(num, from);
    }
  }

  @Test
  @DisplayName("String-to-bytes")
  void test2() {
    for (int i = 0; i < 10; i++) {
      String str = RandomStringUtils.random(1024);
      byte[] to = INSTANCE.to(str);
      String from = INSTANCE.from(to);
      Assertions.assertEquals(str, from);
    }
  }

  @Test
  @DisplayName("String2Bytes-reverse")
  void test3() throws IOException {
    var reverser = INSTANCE.reverse();
    for (int i = 0; i < 10; i++) {
      String str = RandomStringUtils.random(1024);
      byte[] to = INSTANCE.to(str);
      byte[] from = reverser.from(str);
      Assertions.assertArrayEquals(to, from);

      String from1 = INSTANCE.from(to);
      String to1 = reverser.to(from);
      Assertions.assertEquals(from1, to1);
    }
  }

  @Test
  @DisplayName("Long2Bytes-reverse")
  void test4() throws IOException {
    var reverser = TypeConverter.Long2Bytes.INSTANCE.reverse();
    for (int i = 0; i < 10; i++) {
      long num = RandomUtils.nextLong();
      byte[] to = TypeConverter.Long2Bytes.INSTANCE.to(num);
      byte[] from = reverser.from(num);
      Assertions.assertArrayEquals(to, from);

      Long from1 = TypeConverter.Long2Bytes.INSTANCE.from(to);
      Long to1 = reverser.to(from);
      Assertions.assertEquals(from1, to1);
    }
  }

  @Test
  @DisplayName("identity")
  void test5() throws IOException {
    TypeConverter<String, String> identity = TypeConverter.identity();
    for (int i = 0; i < 10; i++) {
      String str = RandomStringUtils.random(1024);
      Assertions.assertEquals(identity.to(str), str);
      Assertions.assertEquals(identity.from(str), str);
    }
  }
}
