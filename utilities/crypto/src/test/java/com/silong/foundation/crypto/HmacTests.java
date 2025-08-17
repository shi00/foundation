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

import static com.silong.foundation.crypto.digest.HmacToolkit.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

/**
 * 单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-16 19:22
 */
public class HmacTests {
  private static String workKey;

  @BeforeAll
  static void init() {
    RootKey rootKey = RootKey.initialize();
    workKey = rootKey.encryptWorkKey("testWorkKey");
  }

  @Test
  @DisplayName("测试hmacSha256Hash方法")
  void testHmacSha256Hash() {
    String plainText = "testPlainText";
    byte[] result = hmacSha256Hash(plainText, workKey);
    assertNotNull(result);
    assertTrue(result.length > 0);
  }

  @Test
  @DisplayName("测试hmacSha256方法")
  void testHmacSha256() {
    String plainText = "testPlainText";
    String result = hmacSha256(plainText, workKey);
    assertNotNull(result);
    assertFalse(result.isEmpty());
  }

  @Test
  @DisplayName("测试hmacSha512方法")
  void testHmacSha512() {
    String plainText = "testPlainText";
    String result = hmacSha512(plainText, workKey);
    assertNotNull(result);
    assertFalse(result.isEmpty());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @DisplayName("测试传入空明文时抛出异常")
  void testHmacWithNullOrEmptyPlainText(String plainText) {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> hmacSha256(plainText, workKey));
    assertEquals("plainText must not be null or empty.", exception.getMessage());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @DisplayName("测试传入空工作密钥时抛出异常")
  void testHmacWithNullOrEmptyWorkKey(String workKey) {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> hmacSha256("testPlainText", workKey));
    assertEquals("workKey must not be null or empty.", exception.getMessage());
  }

  @Test
  @DisplayName("测试hmacSha512Hash方法正常情况")
  void testHmacSha512Hash() {
    String plainText = "testPlainText";
    byte[] result = hmacSha512Hash(plainText, workKey);
    assertNotNull(result);
    assertTrue(result.length > 0);
  }

  @Test
  @DisplayName("测试hmacSha512Hash方法传入空明文")
  void testHmacSha512HashWithNullPlainText() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> hmacSha512Hash(null, workKey));
    assertEquals("plainText must not be null or empty.", exception.getMessage());
  }

  @Test
  @DisplayName("测试hmacSha512Hash方法传入空工作密钥")
  void testHmacSha512HashWithNullWorkKey() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> hmacSha512Hash("testPlainText", null));
    assertEquals("workKey must not be null or empty.", exception.getMessage());
  }

  @Test
  @DisplayName("测试hmacSha512Hash方法传入空字符串明文")
  void testHmacSha512HashWithEmptyPlainText() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> hmacSha512Hash("", workKey));
    assertEquals("plainText must not be null or empty.", exception.getMessage());
  }

  @Test
  @DisplayName("测试hmacSha512Hash方法传入空字符串工作密钥")
  void testHmacSha512HashWithEmptyWorkKey() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> hmacSha512Hash("testPlainText", ""));
    assertEquals("workKey must not be null or empty.", exception.getMessage());
  }
}
