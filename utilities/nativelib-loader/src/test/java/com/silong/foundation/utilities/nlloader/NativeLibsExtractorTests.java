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
    Assertions.assertEquals(1, Objects.requireNonNull(targetDir.toFile().list()).length);
    File file = targetDir.resolve(Objects.requireNonNull(targetDir.toFile().list())[0]).toFile();
    Assertions.assertTrue(file.exists() && file.isFile());
  }

  @Test
  void test5() {
    Path path = NativeLibsExtractor.locate(RocksDB.class);
    String dir = UUID.randomUUID().toString();
    Path targetDir = TEMP_DIR.resolve(dir);
    NativeLibsExtractor.extractNativeLibs(path, targetDir);
    Assertions.assertEquals(1, Objects.requireNonNull(targetDir.toFile().list()).length);
    File file = targetDir.resolve(Objects.requireNonNull(targetDir.toFile().list())[0]).toFile();
    Assertions.assertTrue(file.exists() && file.isFile());

    NativeLibsExtractor.extractNativeLibs(path, targetDir);
    Assertions.assertEquals(1, Objects.requireNonNull(targetDir.toFile().list()).length);
    file = targetDir.resolve(Objects.requireNonNull(targetDir.toFile().list())[0]).toFile();
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
}
