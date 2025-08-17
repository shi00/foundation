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
package com.silong.foundation.crypto.pbkdf2;

import static com.silong.foundation.crypto.pbkdf2.Pbkdf2.MIN_SALT_LENGTH;
import static org.junit.jupiter.api.Assertions.*;

import com.silong.foundation.crypto.utils.ThreadLocalSecureRandom;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class Pbkdf2Tests {

  @ParameterizedTest
  @MethodSource("provideValidInputs")
  @DisplayName("测试PBKDF2生成密钥的有效输入")
  void testGenerateWithValidInputs(char[] chars, int iterations, byte[] salt, int keyLength)
      throws NoSuchAlgorithmException, InvalidKeySpecException {
    byte[] key = Pbkdf2.generate(chars, iterations, salt, keyLength);
    assertEquals(keyLength / Byte.SIZE, key.length);
  }

  @ParameterizedTest
  @MethodSource("provideInvalidInputs")
  @DisplayName("测试PBKDF2生成密钥的无效输入")
  void testGenerateWithInvalidInputs(
      char[] chars, int iterations, byte[] salt, int keyLength, String expectedMessage) {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> Pbkdf2.generate(chars, iterations, salt, keyLength));
    assertEquals(expectedMessage, exception.getMessage());
  }

  @Test
  @DisplayName("测试默认迭代次数生成密钥")
  void testGenerateWithDefaultIterations()
      throws NoSuchAlgorithmException, InvalidKeySpecException {
    char[] chars = "test".toCharArray();
    byte[] key = Pbkdf2.generate(chars, ThreadLocalSecureRandom.random(MIN_SALT_LENGTH), 256);
    assertEquals(256 / Byte.SIZE, key.length);
  }

  @Test
  @DisplayName("测试盐值长度不足时抛出异常")
  void testGenerateWithShortSalt() {
    char[] chars = "test".toCharArray();
    byte[] shortSalt = new byte[MIN_SALT_LENGTH - 1];
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> Pbkdf2.generate(chars, 10000, shortSalt, 256));
    assertEquals(
        String.format("length of salt must be greater than %d.", MIN_SALT_LENGTH),
        exception.getMessage());
  }

  @Test
  @DisplayName("测试输入字符数组为null时生成密钥")
  void testGenerateWithNullChars() throws NoSuchAlgorithmException, InvalidKeySpecException {
    byte[] salt = ThreadLocalSecureRandom.random(MIN_SALT_LENGTH);
    byte[] key = Pbkdf2.generate(null, 10000, salt, 256);
    assertEquals(256 / Byte.SIZE, key.length);
  }

  @Test
  @DisplayName("测试输入字符数组为空时生成密钥")
  void testGenerateWithEmptyChars() throws NoSuchAlgorithmException, InvalidKeySpecException {
    char[] chars = new char[0];
    byte[] salt = ThreadLocalSecureRandom.random(MIN_SALT_LENGTH);
    byte[] key = Pbkdf2.generate(chars, 10000, salt, 256);
    assertEquals(256 / Byte.SIZE, key.length);
  }

  @Test
  @DisplayName("测试随机盐值生成密钥")
  void testGenerateWithRandomSalt() throws NoSuchAlgorithmException, InvalidKeySpecException {
    char[] chars = "test".toCharArray();
    byte[] key = Pbkdf2.generate(chars, 256);
    assertEquals(256 / Byte.SIZE, key.length);
  }

  private static Stream<Arguments> provideValidInputs() {
    return Stream.of(
        Arguments.of(null, 10000, ThreadLocalSecureRandom.random(MIN_SALT_LENGTH), 256),
        Arguments.of(new char[0], 20000, ThreadLocalSecureRandom.random(MIN_SALT_LENGTH), 512),
        Arguments.of(
            "test".toCharArray(), 10000, ThreadLocalSecureRandom.random(MIN_SALT_LENGTH), 256),
        Arguments.of(
            "password".toCharArray(), 20000, ThreadLocalSecureRandom.random(MIN_SALT_LENGTH), 512));
  }

  private static Stream<Arguments> provideInvalidInputs() {
    return Stream.of(
        Arguments.of(
            "test".toCharArray(),
            10000,
            null,
            256,
            String.format("length of salt must be greater than %d.", MIN_SALT_LENGTH)),
        Arguments.of(
            "test".toCharArray(),
            10000,
            new byte[MIN_SALT_LENGTH - 1],
            256,
            String.format("length of salt must be greater than %d.", MIN_SALT_LENGTH)));
  }
}
