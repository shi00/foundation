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

import com.silong.foundation.crypto.aes.AesGcmToolkit;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * 单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2021-12-26 21:34
 */
public class AesGCMTests {

  private static String workKey;

  String plaintext;

  @BeforeAll
  static void init() throws IOException {
    Path dir = new File("target/test-classes").toPath();
    RootKey.export(
        RootKey.DEFAULT_ROOT_KEY_PARTS.stream()
            .map(s -> dir.resolve(s).toFile())
            .toArray(File[]::new));
    RootKey rootKey = RootKey.initialize();
    workKey = rootKey.encryptWorkKey(RandomStringUtils.random(RandomUtils.nextInt(1, Short.SIZE)));
  }

  @BeforeEach
  void initEatch() {
    plaintext = RandomStringUtils.random(RandomUtils.nextInt(1, Short.SIZE));
  }

  @Test
  @DisplayName("AESGCM-1")
  void test1() {
    String encrypt = AesGcmToolkit.encrypt(plaintext, workKey);
    String decrypt = AesGcmToolkit.decrypt(encrypt, workKey);
    assertEquals(plaintext, decrypt);
  }

  @Test
  @DisplayName("AESGCM-2")
  void test2() {
    for (int i = 0; i < 100000; i++) {
      String encrypt1 = AesGcmToolkit.encrypt(plaintext, workKey);
      String encrypt2 = AesGcmToolkit.encrypt(plaintext, workKey);
      assertNotEquals(encrypt1, encrypt2);
      String decrypt1 = AesGcmToolkit.decrypt(encrypt1, workKey);
      String decrypt2 = AesGcmToolkit.decrypt(encrypt2, workKey);
      assertEquals(decrypt1, decrypt2);
    }
  }
}
