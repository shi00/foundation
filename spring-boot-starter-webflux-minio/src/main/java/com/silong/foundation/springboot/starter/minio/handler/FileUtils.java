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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.util.DigestUtils;

/**
 * 文件工具
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-03-27 21:33
 */
@Slf4j
class FileUtils {

  @SuppressFBWarnings(
      value = "WEAK_MESSAGE_DIGEST_MD5",
      justification = "MD5 used for non-security purposes")
  private static final ThreadLocal<MessageDigest> MD5_DIGEST =
      ThreadLocal.withInitial(
          () -> {
            try {
              return MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
              throw new RuntimeException(e);
            }
          });

  private static final ThreadLocal<ByteBuffer> BUFFER_THREAD_LOCAL =
      ThreadLocal.withInitial(() -> ByteBuffer.allocateDirect(8192));

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
   * @throws NoSuchAlgorithmException 异常
   */
  public static String calculateETag(File file, long partSize) throws Exception {
    long fileSize = file.length();

    // 单片直接计算，无需考虑分段计算
    if (partSize >= fileSize) {
      try (InputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
        return DigestUtils.md5DigestAsHex(inputStream);
      }
    }

    int partCount = (int) Math.ceil((double) fileSize / partSize);

    assert partCount > 1;

    try (FileChannel channel = FileChannel.open(file.toPath())) {
      CompletableFuture<byte[]>[] futures = new CompletableFuture[partCount];
      for (int i = 0; i < partCount; i++) {
        long offset = i * partSize;
        long length = Math.min(partSize, fileSize - offset);
        futures[i] = CompletableFuture.supplyAsync(() -> computeChunkMD5(channel, offset, length));
      }

      String combinedMD5Hex =
          CompletableFuture.allOf(futures)
              .thenApply(
                  v -> {
                    var digest = getMd5Digest();
                    Arrays.stream(futures).map(CompletableFuture::join).forEach(digest::update);
                    return Hex.encodeHexString(digest.digest());
                  })
              .get();
      return String.format("\"%s-%d\"", combinedMD5Hex, partCount);
    }
  }

  @SneakyThrows
  private static byte[] computeChunkMD5(FileChannel channel, long offset, long length) {
    MessageDigest digest = getMd5Digest();
    ByteBuffer buf = BUFFER_THREAD_LOCAL.get(); // 直接缓冲区
    long pos = offset;
    long remaining = length;

    while (remaining > 0) {
      buf.clear();
      int bytesRead = channel.read(buf, pos);
      buf.flip();
      digest.update(buf);
      pos += bytesRead;
      remaining -= bytesRead;
    }
    return digest.digest();
  }

  private static MessageDigest getMd5Digest() {
    MessageDigest digest = MD5_DIGEST.get();
    digest.reset();
    return digest;
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
