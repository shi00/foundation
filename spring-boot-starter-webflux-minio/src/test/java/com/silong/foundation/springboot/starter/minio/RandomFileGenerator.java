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

package com.silong.foundation.springboot.starter.minio;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import org.springframework.util.unit.DataSize;

class RandomFileGenerator {

  /** 禁止实例化 */
  private RandomFileGenerator() {}

  /**
   * 生成指定大小的随机内容临时文件
   *
   * @param f 生成文件
   * @param sizeInBytes 文件大小（字节）
   * @throws IOException 创建文件失败时抛出
   */
  public static void createRandomTempFile(File f, DataSize sizeInBytes) throws IOException {
    // 确保父目录存在
    Path path = f.toPath();
    Files.createDirectories(path.getParent());

    try (RandomAccessFile raf = new RandomAccessFile(f, "rw");
        FileChannel channel = raf.getChannel()) {

      // 预先分配文件空间（避免后续扩展文件大小带来的性能开销）
      raf.setLength(sizeInBytes.toBytes());

      // 每次映射 1GB（可根据系统内存调整）
      long chunkSize = 1024L * 1024L * 1024L; // 1GB
      Random random = new Random();

      for (long position = 0; position < sizeInBytes.toBytes(); position += chunkSize) {
        long currentChunkSize = Math.min(chunkSize, sizeInBytes.toBytes() - position);

        // 映射文件区域
        MappedByteBuffer buffer =
            channel.map(FileChannel.MapMode.READ_WRITE, position, currentChunkSize);

        // 使用 8KB 缓冲区填充随机数据
        byte[] fillBuffer = new byte[8192];
        while (buffer.remaining() > 0) {
          random.nextBytes(fillBuffer);
          int length = Math.min(fillBuffer.length, buffer.remaining());
          buffer.put(fillBuffer, 0, length);
        }

        // 强制刷新到磁盘（可选，取决于是否需要立即持久化）
        //        buffer.force();
      }
    }
    f.deleteOnExit();
  }
}
