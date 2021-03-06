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
package com.silong.foundation.crypto;

import com.silong.foundation.crypto.digest.HmacToolkit;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static com.silong.foundation.crypto.digest.HmacToolkit.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-16 19:22
 */
public class HmacTests {
  static RootKey rootKey;

  String workKey;

  String plaintext;

  @BeforeAll
  static void init() throws IOException {
    Path dir = new File("target/test-classes").toPath();
    RootKey.export(
        RootKey.DEFAULT_ROOT_KEY_PARTS.stream()
            .map(s -> dir.resolve(s).toFile())
            .toArray(File[]::new));
    rootKey = RootKey.initialize();
  }

  @BeforeEach
  void initEatch() {
    plaintext = RandomStringUtils.random(RandomUtils.nextInt(1, Short.SIZE));
    workKey = rootKey.encryptWorkKey(RandomStringUtils.random(RandomUtils.nextInt(1, Short.SIZE)));
  }

  @Test
  void test1() {
    assertEquals(
        HmacToolkit.hmacSha256(plaintext, workKey), HmacToolkit.hmacSha256(plaintext, workKey));
  }

  @Test
  void test2() {
    assertArrayEquals(hmacSha256Hash(plaintext, workKey), hmacSha256Hash(plaintext, workKey));
  }

  @Test
  void test3() {
    assertEquals(hmacSha512(plaintext, workKey), hmacSha512(plaintext, workKey));
  }

  @Test
  void test4() {
    assertArrayEquals(hmacSha512Hash(plaintext, workKey), hmacSha512Hash(plaintext, workKey));
  }

  @Test
  void test5() {
    assertNotEquals(
        hmacSha512(RandomStringUtils.random(RandomUtils.nextInt(1, Short.SIZE)), workKey),
        hmacSha512(plaintext, workKey));
  }

  @Test
  void test6() {
    assertNotEquals(
        hmacSha256(RandomStringUtils.random(RandomUtils.nextInt(1, Short.SIZE)), workKey),
        hmacSha256(plaintext, workKey));
  }

  @Test
  void test7() {
    assertNotEquals(hmacSha512(plaintext, workKey), hmacSha256(plaintext, workKey));
  }
}
