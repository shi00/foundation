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

package com.silong.foundation.springboot.starter.minio.handler;

import static java.nio.file.Files.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

/**
 * 测试类
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-03-27 21:33
 */
@Slf4j
public class FileUtilsTests {

  @Test
  public void test1() throws IOException {
    var path = Paths.get("target/test-data/test-dir");
    createDirectories(path);
    var paths = new ArrayList<Path>(32);
    createNestedDirectories(path, 10, paths);
    for (var p : paths) {
      String name = RandomStringUtils.randomAlphabetic(10);
      var file = p.resolve(name + ".docx").toFile();
      RandomFileGenerator.createRandomTempFile(file, DataSize.ofMegabytes(1));
      name = RandomStringUtils.randomAlphabetic(20);
      file = p.resolve(name + ".docx").toFile();
      RandomFileGenerator.createRandomTempFile(file, DataSize.ofMegabytes(10));
    }
    FileUtils.deleteRecursively(path);
    Assertions.assertTrue(Files.notExists(path));
  }

  static void createNestedDirectories(Path currentPath, int remainingLevels, List<Path> paths)
      throws IOException {
    if (remainingLevels == 0) {
      return;
    }
    Path newPath = currentPath.resolve("level" + remainingLevels);
    if (notExists(newPath)) {
      createDirectory(newPath);
      log.info("Successfully created : {}", newPath);
      paths.add(newPath);
    }
    createNestedDirectories(newPath, remainingLevels - 1, paths);
  }

  @Test
  void test2() throws Exception {
    var path = Paths.get("target/test-data/test-dir");
    createDirectories(path);

    var fp = path.resolve(RandomStringUtils.randomAlphabetic(32));
    RandomFileGenerator.createRandomTempFile(fp.toFile(), DataSize.ofMegabytes(2));
    Assertions.assertTrue(Files.exists(fp));

    FileUtils.deleteRecursively(fp);
    Assertions.assertTrue(Files.notExists(fp));
  }
}
