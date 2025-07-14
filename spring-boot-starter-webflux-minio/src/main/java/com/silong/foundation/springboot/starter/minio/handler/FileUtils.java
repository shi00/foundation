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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;

/**
 * 文件工具
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-03-27 21:33
 */
@Slf4j
class FileUtils {

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
   * 按照minio生成eTag的计算方法生成文件eTag
   *
   * @param file 文件
   * @param partSize 分段大小
   * @return eTag
   * @throws IOException 异常
   */
  public static String calculateETag(File file, long partSize) throws IOException {
    long fileSize = file.length();
    int partCount = (int) Math.ceil((double) fileSize / partSize);

    // 存储每个分片的原始MD5字节
    byte[][] partMD5s = new byte[partCount][];

    try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
      for (int i = 0; i < partCount; i++) {
        long offset = i * partSize;
        long currentPartSize = Math.min(partSize, fileSize - offset);

        raf.seek(offset);
        partMD5s[i] = calculatePartMD5(raf, currentPartSize);
      }
    }

    // 合并所有分片的MD5字节
    MessageDigest combinedDigest = getMD5Digest();
    for (byte[] partMD5 : partMD5s) {
      combinedDigest.update(partMD5);
    }

    // 格式化为 "MD5-分片数"
    String combinedMD5Hex = Hex.encodeHexString(combinedDigest.digest());
    return partCount == 1 ? combinedMD5Hex : String.format("\"%s-%d\"", combinedMD5Hex, partCount);
  }

  private static byte[] calculatePartMD5(RandomAccessFile raf, long partSize) throws IOException {
    MessageDigest digest = getMD5Digest();
    byte[] buffer = new byte[8192];
    long remaining = partSize;

    while (remaining > 0) {
      int read = raf.read(buffer, 0, (int) Math.min(buffer.length, remaining));
      if (read == -1) break;
      digest.update(buffer, 0, read);
      remaining -= read;
    }

    return digest.digest();
  }

  private static MessageDigest getMD5Digest() {
    try {
      return MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("MD5 algorithm not found", e);
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
