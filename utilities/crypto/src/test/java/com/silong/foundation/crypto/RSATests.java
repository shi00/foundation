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

import com.silong.foundation.crypto.rsa.RsaKeyPair;
import com.silong.foundation.crypto.rsa.RsaKeySize;
import com.silong.foundation.crypto.rsa.RsaToolkit;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.*;

import java.io.File;
import java.nio.file.Path;

import static com.silong.foundation.crypto.rsa.RsaToolkit.*;

/**
 * 单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2021-12-26 21:34
 */
public class RSATests {

  static RsaKeyPair keyPair;

  static RootKey rootKey;

  String plaintext;

  @BeforeAll
  static void init() throws Exception {
    Path dir = new File("target/test-classes").toPath();
    RootKey.export(
        RootKey.DEFAULT_ROOT_KEY_PARTS.stream()
            .map(s -> dir.resolve(s).toFile())
            .toArray(File[]::new));
    rootKey = RootKey.initialize();
    RsaKeyPair rsaKeyPair = RsaToolkit.generate(RsaKeySize.BITS_3072);
    File pubkey = dir.resolve("pubkey").toFile();
    File prikey = dir.resolve("prikey").toFile();
    rsaKeyPair.export(prikey, pubkey);
    keyPair = RsaKeyPair.importRsaKeyPair(prikey, pubkey);
  }

  @BeforeEach
  void initEatch() {
    plaintext = RandomStringUtils.random(RandomUtils.nextInt(1, Short.MAX_VALUE));
  }

  @Test
  @DisplayName("PrivateKey-encrypt|PublicKey-decrypt")
  void test1() {
    String encrypt = encryptByPrivateKey(keyPair.getPrivateKey(), plaintext);
    String decrypt = decryptByPublicKey(keyPair.getPublicKey(), encrypt);
    Assertions.assertEquals(plaintext, decrypt);
  }

  @Test
  @DisplayName("PublicKey-encrypt|PrivateKey-decrypt")
  void test2() {
    String encrypt = encryptByPublicKey(keyPair.getPublicKey(), plaintext);
    String decrypt = decryptByPrivateKey(keyPair.getPrivateKey(), encrypt);
    Assertions.assertEquals(plaintext, decrypt);
  }

  @Test
  @DisplayName("PrivateKey-encrypt|PublicKey-decrypt|long-str")
  void test3() {
    for (int i = 0; i < 30; i++) {
      plaintext = RandomStringUtils.random(RandomUtils.nextInt(1000, Short.MAX_VALUE));
      String encrypt = encryptByPrivateKey(keyPair.getPrivateKey(), plaintext);
      String decrypt = decryptByPublicKey(keyPair.getPublicKey(), encrypt);
      Assertions.assertEquals(plaintext, decrypt);
    }
  }

  @Test
  @DisplayName("PublicKey-encrypt|PrivateKey-decrypt|long-str")
  void test4() {
    for (int i = 0; i < 20; i++) {
      plaintext = RandomStringUtils.random(RandomUtils.nextInt(2000, Short.MAX_VALUE));
      String encrypt = encryptByPublicKey(keyPair.getPublicKey(), plaintext);
      String decrypt = decryptByPrivateKey(keyPair.getPrivateKey(), encrypt);
      Assertions.assertEquals(plaintext, decrypt);
    }
  }
}
