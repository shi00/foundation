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
package com.silong.foundation.crypto.aes;

import static org.junit.jupiter.api.Assertions.*;

import com.silong.foundation.crypto.RootKey;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2021-12-26 21:34
 */
public class AesGCMTests {

  private static String workKey;

  @BeforeAll
  static void init() throws IOException {
    Path dir = new File("target/test-classes").toPath();
    RootKey.export(
        RootKey.DEFAULT_ROOT_KEY_PARTS.stream()
            .map(s -> dir.resolve(s).toFile())
            .toArray(File[]::new));
    RootKey rootKey = RootKey.initialize();
    workKey = rootKey.encryptWorkKey(RandomStringUtils.randomAlphanumeric(16));
  }

  @Test
  @DisplayName("测试加密时传入空明文")
  void testEncryptWithNullPlainText() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              AesGcmToolkit.encrypt(null, workKey);
            });
    assertEquals("plainText must not be null or empty.", exception.getMessage());
  }

  @Test
  @DisplayName("测试解密时传入空密文")
  void testDecryptWithNullCipherText() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              AesGcmToolkit.decrypt(null, workKey);
            });
    assertEquals("cipherText must not be null or empty.", exception.getMessage());
  }

  @Test
  @DisplayName("测试加密时传入无效工作密钥")
  void testEncryptWithInvalidWorkKey() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              AesGcmToolkit.encrypt("plaintext", "");
            });
    assertEquals("workKey must not be null or empty.", exception.getMessage());
  }

  @Test
  @DisplayName("测试解密时传入无效工作密钥")
  void testDecryptWithInvalidWorkKey() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              AesGcmToolkit.decrypt("cipherText", "");
            });
    assertEquals("workKey must not be null or empty.", exception.getMessage());
  }

  @Test
  @DisplayName("测试加密时传入无效IV")
  void testEncryptWithInvalidIv() {
    SecretKeySpec secretKey = new SecretKeySpec(workKey.getBytes(), "AES");
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              AesGcmToolkit.encrypt(new byte[16], 0, 16, secretKey, null);
            });
    assertEquals("src array is null", exception.getMessage());
  }

  @Test
  @DisplayName("测试解密时传入无效IV")
  void testDecryptWithInvalidIv() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              AesGcmToolkit.decrypt(new byte[16], 0, 16, null);
            });
    assertEquals("key must not be null.", exception.getMessage());
  }

  @Test
  @DisplayName("测试使用工作密钥加密字符串")
  void testEncryptWithWorkKey() {
    String plainText = "testPlainText";
    String encryptedText = AesGcmToolkit.encrypt(plainText, workKey);
    assertNotNull(encryptedText);
    assertFalse(encryptedText.isEmpty());
  }

  @Test
  @DisplayName("测试使用工作密钥解密字符串")
  void testDecryptWithWorkKey() {
    String plainText = "testPlainText";
    String encryptedText = AesGcmToolkit.encrypt(plainText, workKey);
    String decryptedText = AesGcmToolkit.decrypt(encryptedText, workKey);
    assertEquals(plainText, decryptedText);
  }

  @Test
  @DisplayName("测试解密工作密钥")
  void testDecryptWorkKey() {
    byte[] decryptedKey = RootKey.getInstance().decryptWorkKey(workKey);
    assertNotNull(decryptedKey);
    assertEquals(AesKeySize.BITS_256.getBytes(), decryptedKey.length);
  }

  @Test
  @DisplayName("测试加密时传入空工作密钥")
  void testEncryptWithNullWorkKey() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              AesGcmToolkit.encrypt("plaintext", null);
            });
    assertEquals("workKey must not be null or empty.", exception.getMessage());
  }

  @Test
  @DisplayName("测试解密时传入空工作密钥")
  void testDecryptWithNullWorkKey() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              AesGcmToolkit.decrypt("cipherText", (String) null);
            });
    assertEquals("workKey must not be null or empty.", exception.getMessage());
  }

  @Test
  @DisplayName("测试解密时传入空密文")
  void testDecryptWithEmptyCipherText() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              AesGcmToolkit.decrypt("", workKey);
            });
    assertEquals("cipherText must not be null or empty.", exception.getMessage());
  }

  @Test
  @DisplayName("测试加密时传入空明文")
  void testEncryptWithEmptyPlainText() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              AesGcmToolkit.encrypt("", workKey);
            });
    assertEquals("plainText must not be null or empty.", exception.getMessage());
  }

  @Test
  @DisplayName("测试解密工作密钥时传入空密钥")
  void testDecryptWorkKeyWithNullKey() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              RootKey.getInstance().decryptWorkKey(null);
            });
    assertEquals("workKey must not be null or empty.", exception.getMessage());
  }

  @Test
  @DisplayName("测试解密工作密钥时传入空字符串")
  void testDecryptWorkKeyWithEmptyKey() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              RootKey.getInstance().decryptWorkKey("");
            });
    assertEquals("workKey must not be null or empty.", exception.getMessage());
  }

  @Test
  @DisplayName("测试解密工作密钥时传入无效格式的密钥")
  void testDecryptWorkKeyWithInvalidFormat() {
    String invalidWorkKey = "invalidKeyFormat";
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> RootKey.getInstance().decryptWorkKey(invalidWorkKey));
    assertEquals(
        "securityText format is not valid, and valid securityText must start with security:.",
        exception.getMessage());
  }

  @Test
  @DisplayName("测试解密工作密钥时传入过短的密钥")
  void testDecryptWorkKeyWithShortKey() {
    String shortWorkKey = "shortKey";
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> RootKey.getInstance().decryptWorkKey(shortWorkKey));
    assertEquals(
        "securityText format is not valid, and valid securityText must start with security:.",
        exception.getMessage());
  }

  @Test
  @DisplayName("测试解密工作密钥时传入过长的密钥")
  void testDecryptWorkKeyWithLongKey() {
    String longWorkKey = RandomStringUtils.randomAlphanumeric(1024);
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> RootKey.getInstance().decryptWorkKey(longWorkKey));
    assertEquals(
        "securityText format is not valid, and valid securityText must start with security:.",
        exception.getMessage());
  }

  @Test
  @DisplayName("测试解密工作密钥时传入有效密钥")
  void testDecryptWorkKeyWithValidKey() {
    String validWorkKey = workKey; // 使用初始化的工作密钥
    byte[] decryptedKey = RootKey.getInstance().decryptWorkKey(validWorkKey);
    assertNotNull(decryptedKey);
    assertEquals(AesKeySize.BITS_256.getBytes(), decryptedKey.length);
  }
}
