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

package com.silong.foundation.crypto;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * RootKey单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-16 19:22
 */
public class RootKeyTests {

  private static RootKey rootKey;

  @BeforeAll
  static void init() throws IOException {
    // 初始化RootKey
    File[] keyParts = {
      new File("target/test-classes/zoo/tiger"),
      new File("target/test-classes/zoo/north/penguin"),
      new File("target/test-classes/zoo/south/skunk"),
      new File("target/test-classes/zoo/west/peacock")
    };
    RootKey.export(keyParts);
    rootKey =
        RootKey.initialize("zoo/tiger", "zoo/north/penguin", "zoo/south/skunk", "zoo/west/peacock");
  }

  @Test
  @DisplayName("测试加密工作密钥")
  void testEncryptWorkKey() {
    String workKey = "testWorkKey";
    String encryptedKey = rootKey.encryptWorkKey(workKey);
    assertNotNull(encryptedKey);
  }

  @Test
  @DisplayName("测试解密工作密钥")
  void testDecryptWorkKey() {
    String workKey = "testWorkKey";
    String encryptedKey = rootKey.encryptWorkKey(workKey);
    byte[] decryptedKey = rootKey.decryptWorkKey(encryptedKey);
    assertNotNull(decryptedKey);
  }

  @Test
  @DisplayName("测试导出根密钥")
  void testExportRootKey() throws IOException {
    File[] keyParts = {
      new File("target/test-classes/zoo/tiger"),
      new File("target/test-classes/zoo/north/penguin"),
      new File("target/test-classes/zoo/south/skunk"),
      new File("target/test-classes/zoo/west/peacock")
    };
    RootKey.export(keyParts);
    for (File file : keyParts) {
      assertTrue(file.exists());
    }
  }

  @Test
  @DisplayName("测试初始化根密钥")
  void testInitializeRootKey() {
    RootKey initializedKey =
        RootKey.initialize("zoo/tiger", "zoo/north/penguin", "zoo/south/skunk", "zoo/west/peacock");
    assertNotNull(initializedKey);
  }

  @Test
  @DisplayName("测试生成根密钥")
  void testGenerateRootKey() {
    String[] keyParts = RootKey.generate();
    assertEquals(4, keyParts.length);
    for (String part : keyParts) {
      assertNotNull(part);
      assertFalse(part.isEmpty());
    }
  }

  @Test
  @DisplayName("测试异常情况：解密空工作密钥")
  void testDecryptNullWorkKey() {
    assertThrows(IllegalArgumentException.class, () -> rootKey.decryptWorkKey(null));
  }

  @Test
  @DisplayName("测试异常情况：加密空工作密钥")
  void testEncryptNullWorkKey() {
    assertThrows(IllegalArgumentException.class, () -> rootKey.encryptWorkKey((String) null));
  }

  @Test
  @DisplayName("测试异常情况：导出根密钥路径不足")
  void testExportInvalidPaths() {
    assertThrows(IllegalArgumentException.class, () -> RootKey.export(new File("path1")));
  }

  @Test
  @DisplayName("测试异常情况：初始化根密钥路径不足")
  void testInitializeInvalidPaths() {
    RootKey.clear();
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> RootKey.initialize("invalidPath1"));
    assertEquals("rootKeyParts must be 4.", exception.getMessage());
  }
}
