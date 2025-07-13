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

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * 文件工具
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-03-27 21:33
 */
@Slf4j
public class FileUtils {

  /** 工具类，禁止实例化 */
  private FileUtils() {}

  /**
   * 创建目录
   *
   * @param dir 目录
   * @throws IOException 异常
   */
  public static void createDirectories(Path dir) throws IOException {
    if (!Files.exists(dir)) {
      Files.createDirectories(dir);
    }
  }

  /**
   * 删除目录或文件，如果是目录则递归删除，如果是文件则直接删除
   *
   * @param path path
   */
  public static void deleteRecursively(Path path) {
    try {
      if (Files.isDirectory(path)) {
        Files.walkFileTree(
            path,
            new SimpleFileVisitor<>() {
              @Override
              @NonNull
              public FileVisitResult visitFile(
                  @NonNull Path file, @NonNull BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                log.info("Deleted file: {}", file);
                return FileVisitResult.CONTINUE;
              }

              @Override
              @NonNull
              public FileVisitResult postVisitDirectory(@NonNull Path dir, IOException exc)
                  throws IOException {
                Files.delete(dir);
                log.info("Deleted directory: {}", dir);
                return FileVisitResult.CONTINUE;
              }
            });
      } else {
        Files.delete(path);
      }
    } catch (IOException e) {
      log.error("Failed to delete {}", path, e);
    }
  }
}
