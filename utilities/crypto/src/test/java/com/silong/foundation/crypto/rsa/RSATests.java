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
package com.silong.foundation.crypto.rsa;

import static com.silong.foundation.crypto.rsa.RsaToolkit.*;
import static org.junit.jupiter.api.Assertions.*;

import com.silong.foundation.crypto.RootKey;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * RSA单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2021-12-27 21:59
 */
public class RSATests {

  private static RsaKeyPair rsaKeyPair;

  @BeforeAll
  static void init() throws NoSuchAlgorithmException, IOException {

    Path dir = new File("target/test-classes").toPath();
    RootKey.export(
        RootKey.DEFAULT_ROOT_KEY_PARTS.stream()
            .map(s -> dir.resolve(s).toFile())
            .toArray(File[]::new));
    RootKey.initialize();

    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RsaToolkit.RSA);
    keyPairGenerator.initialize(RsaKeySize.BITS_4096.getBits());
    KeyPair keyPair = keyPairGenerator.generateKeyPair();
    rsaKeyPair = new RsaKeyPair(keyPair.getPublic(), keyPair.getPrivate());

    // 定义文件路径
    File privateKeyFile = new File("target/test-classes/private.key");
    File publicKeyFile = new File("target/test-classes/public.key");

    // 导出密钥到文件
    rsaKeyPair.export(privateKeyFile, publicKeyFile);
  }

  @Test
  @DisplayName("测试公钥加密和私钥解密")
  void testEncryptDecrypt() {
    String plainText = "Test RSA Encryption";
    String cipherText = encryptByPublicKey(rsaKeyPair.getPublicKey(), plainText);
    String decryptedText = decryptByPrivateKey(rsaKeyPair.getPrivateKey(), cipherText);
    assertEquals(plainText, decryptedText);
  }

  @Test
  @DisplayName("测试私钥加密和公钥解密")
  void testEncryptDecryptReverse() {
    String plainText = "Test RSA Reverse Encryption";
    String cipherText = RsaToolkit.encryptByPrivateKey(rsaKeyPair.getPrivateKey(), plainText);
    String decryptedText = RsaToolkit.decryptByPublicKey(rsaKeyPair.getPublicKey(), cipherText);
    assertEquals(plainText, decryptedText);
  }

  @ParameterizedTest
  @MethodSource("provideInvalidPlainText")
  @DisplayName("测试加密时传入无效明文")
  void testEncryptWithInvalidPlainText(String invalidPlainText) {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> encryptByPublicKey(rsaKeyPair.getPublicKey(), invalidPlainText));
    assertEquals("plainText must not be null or empty.", exception.getMessage());
  }

  private static Stream<Arguments> provideInvalidPlainText() {
    return Stream.of(Arguments.of(""), Arguments.of("   "), Arguments.of((String) null));
  }

  @Test
  @DisplayName("测试导出公私钥到文件")
  void testExportKeys() throws IOException {
    File privateKeyFile = new File("target/test-classes/private.key");
    File publicKeyFile = new File("target/test-classes/public.key");
    rsaKeyPair.export(privateKeyFile, publicKeyFile);
    assertTrue(privateKeyFile.exists());
    assertTrue(publicKeyFile.exists());
  }

  @Test
  @DisplayName("测试导入公私钥")
  void testImportKeys() throws Exception {
    File privateKeyFile = new File("target/test-classes/private.key");
    File publicKeyFile = new File("target/test-classes/public.key");
    RsaKeyPair importedKeyPair = RsaKeyPair.importRsaKeyPair(privateKeyFile, publicKeyFile);
    assertEquals(rsaKeyPair.getPublicKey(), importedKeyPair.getPublicKey());
    assertEquals(rsaKeyPair.getPrivateKey(), importedKeyPair.getPrivateKey());
  }

  @Test
  @DisplayName("测试导入无效公私钥文件")
  void testImportInvalidKeys() {
    File invalidFile = new File("invalid/path.key");
    assertThrows(
        FileNotFoundException.class, () -> RsaKeyPair.importRsaKeyPair(invalidFile, invalidFile));
  }

  @Test
  @DisplayName("测试不同长度密钥的加密和解密")
  void testEncryptDecryptWithDifferentKeySizes() {
    String plainText = "testPlainText";
    for (RsaKeySize keySize : RsaKeySize.values()) {
      RsaKeyPair rsaKeyPair = generate(keySize);

      // 使用公钥加密，私钥解密
      String encryptedText = encryptByPublicKey(rsaKeyPair.getPublicKey(), plainText);
      String decryptedText = decryptByPrivateKey(rsaKeyPair.getPrivateKey(), encryptedText);
      assertEquals(plainText, decryptedText, "公钥加密/私钥解密失败");

      // 使用私钥加密，公钥解密
      encryptedText = encryptByPrivateKey(rsaKeyPair.getPrivateKey(), plainText);
      decryptedText = decryptByPublicKey(rsaKeyPair.getPublicKey(), encryptedText);
      assertEquals(plainText, decryptedText, "私钥加密/公钥解密失败");
    }
  }
}
