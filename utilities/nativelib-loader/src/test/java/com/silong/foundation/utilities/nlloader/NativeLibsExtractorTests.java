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

package com.silong.foundation.utilities.nlloader;

import static com.silong.foundation.utilities.nlloader.NativeLibLoader.*;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.rocksdb.CompressionType;
import org.rocksdb.RocksDB;
import org.rocksdb.WriteBatch;

/**
 * 单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-13 23:03
 */
public class NativeLibsExtractorTests {
  @Test
  void test1() throws Exception {
    Path path2 = NativeLibsExtractor.byGetResource(RocksDB.class);
    Path path1 = NativeLibsExtractor.byGetProtectionDomain(RocksDB.class);
    Assertions.assertEquals(path1, path2);
  }

  @Test
  void test2() throws Exception {
    Path path2 = NativeLibsExtractor.locate(WriteBatch.class);
    Path path1 = NativeLibsExtractor.byGetProtectionDomain(WriteBatch.class);
    Assertions.assertEquals(path1, path2);
  }

  @Test
  void test3() throws Exception {
    Path path2 = NativeLibsExtractor.locate(CompressionType.class);
    Path path1 = NativeLibsExtractor.byGetResource(CompressionType.class);
    Assertions.assertEquals(path1, path2);
  }

  @Test
  void test4() {
    Path path = NativeLibsExtractor.locate(RocksDB.class);
    String dir = UUID.randomUUID().toString();
    Path targetDir = TEMP_DIR.resolve(dir);
    NativeLibsExtractor.extractNativeLibs(path, targetDir);
    var files = targetDir.toFile().list();
    Assertions.assertNotNull(files);
    File file = targetDir.resolve(files[0]).toFile();
    Assertions.assertTrue(file.exists() && file.isFile());
  }

  @Test
  void test5() {
    Path path = NativeLibsExtractor.locate(RocksDB.class);
    String dir = UUID.randomUUID().toString();
    Path targetDir = TEMP_DIR.resolve(dir);
    NativeLibsExtractor.extractNativeLibs(path, targetDir);
    var files = targetDir.toFile().list();
    Assertions.assertNotNull(files);
    File file = targetDir.resolve(files[0]).toFile();
    Assertions.assertTrue(file.exists() && file.isFile());

    NativeLibsExtractor.extractNativeLibs(path, targetDir);
    var files1 = targetDir.toFile().list();
    Assertions.assertNotNull(files1);
    file = targetDir.resolve(files1[0]).toFile();
    Assertions.assertTrue(file.exists() && file.isFile());
  }

  @Test
  void test6() {
    Path path = NativeLibsExtractor.locate(NativeLibLoader.class);
    String dir = UUID.randomUUID().toString();
    Path targetDir = TEMP_DIR.resolve(dir);
    NativeLibsExtractor.extractNativeLibs(path, targetDir);
    Assertions.assertEquals(0, Objects.requireNonNull(targetDir.toFile().list()).length);
  }

  @Test
  void testLocateWithValidClass() {
    Path path = NativeLibsExtractor.locate(RocksDB.class);
    Assertions.assertNotNull(path);
  }

  @Test
  void testLocateWithInvalidClass() {
    Assertions.assertThrows(
        NullPointerException.class, () -> NativeLibsExtractor.locate(String.class));
  }

  @Test
  void testExtractNativeLibsWithValidJar() {
    Path path = NativeLibsExtractor.locate(RocksDB.class);
    String dir = UUID.randomUUID().toString();
    Path targetDir = TEMP_DIR.resolve(dir);
    NativeLibsExtractor.extractNativeLibs(path, targetDir);
    var files = targetDir.toFile().list();
    Assertions.assertNotNull(files);
    Assertions.assertTrue(files.length > 0);
  }

  @Test
  void testExtractNativeLibsWithInvalidPath() {
    Path invalidPath = Path.of("invalid/path/to/jar");
    String dir = UUID.randomUUID().toString();
    Path targetDir = TEMP_DIR.resolve(dir);
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> NativeLibsExtractor.extractNativeLibs(invalidPath, targetDir));
  }

  @Test
  void testByGetProtectionDomainWithValidClass() throws URISyntaxException {
    Path path = NativeLibsExtractor.byGetProtectionDomain(RocksDB.class);
    Assertions.assertNotNull(path);
  }

  @Test
  void testByGetProtectionDomainWithInvalidClass() {
    Assertions.assertThrows(
        NullPointerException.class, () -> NativeLibsExtractor.byGetProtectionDomain(String.class));
  }

  @Test
  void testByGetResourceWithValidClass() throws URISyntaxException {
    Path path = NativeLibsExtractor.byGetResource(CompressionType.class);
    Assertions.assertNotNull(path);
  }

  @Test
  void testByGetResourceWithInvalidClass() {
    Assertions.assertThrows(
        IllegalStateException.class, () -> NativeLibsExtractor.byGetResource(String.class));
  }
}
