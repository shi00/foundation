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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.util.unit.DataSize;

class RandomFileGenerator {

  /** 禁止实例化 */
  private RandomFileGenerator() {}

  /**
   * 生成指定大小的随机内容临时文件
   *
   * @param file 生成文件
   * @param sizeInBytes 文件大小（字节）
   * @throws IOException 创建文件失败时抛出
   */
  public static void createRandomTempFile(File file, DataSize sizeInBytes) throws IOException {

    try (RandomAccessFile raf = new RandomAccessFile(file, "rw");
        FileChannel channel = raf.getChannel()) {

      raf.setLength(sizeInBytes.toBytes());
      Random random = ThreadLocalRandom.current();
      byte[] buffer = new byte[1024 * 1024];

      // 分块写入数据，但不使用内存映射（避免直接内存管理）
      for (long position = 0; position < sizeInBytes.toBytes(); position += buffer.length) {
        int bytesToWrite = (int) Math.min(buffer.length, sizeInBytes.toBytes() - position);
        random.nextBytes(buffer);
        channel.write(ByteBuffer.wrap(buffer, 0, bytesToWrite), position);
      }
    } finally {
      file.deleteOnExit();
    }
  }
}
