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

package com.silong.foundation.utilities.portaudio;

import static java.lang.foreign.ValueLayout.ADDRESS;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.time.Duration;
import lombok.SneakyThrows;

/**
 * 工具类
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-09-11 14:52
 */
class Utils {

  private static final Linker LINKER = Linker.nativeLinker();

  private static final MethodHandle FREE =
      LINKER.downcallHandle(
          LINKER.defaultLookup().find("free").orElseThrow(), FunctionDescriptor.ofVoid(ADDRESS));

  private static final MethodHandle MALLOC =
      LINKER.downcallHandle(
          LINKER.defaultLookup().find("malloc").orElseThrow(),
          FunctionDescriptor.of(
              ValueLayout.ADDRESS, // 返回值：void* 指针
              ValueLayout.JAVA_LONG // 参数：size_t（分配的字节数）
              ));

  /** 禁止实例化 */
  private Utils() {}

  private static boolean isPowerOfTwo(int n) {
    return n > 0 && (n & (n - 1)) == 0;
  }

  static void validateParameters(
      int sampleRate,
      SampleFormat sampleFormat,
      int channels,
      Duration audioChunkDuration,
      int framesPerRead,
      int ringBufferSize,
      AudioChunkProcessor processor) {
    if (sampleRate <= 0) {
      throw new IllegalArgumentException("sampleRate must be greater than 0.");
    }
    if (sampleFormat == null) {
      throw new IllegalArgumentException("sampleFormat must not be null.");
    }
    if (channels <= 0) {
      throw new IllegalArgumentException("channels must be greater than 0");
    }
    if (audioChunkDuration == null) {
      throw new IllegalArgumentException("audioChunkDuration must not be null.");
    }
    if (framesPerRead <= 0) {
      throw new IllegalArgumentException("framesPerRead must be greater than 0.");
    }
    if (!isPowerOfTwo(ringBufferSize)) {
      throw new IllegalArgumentException("ringBufferSize must be a power of two.");
    }
    if (processor == null) {
      throw new IllegalArgumentException("processor must not be null.");
    }
  }

  /**
   * 释放内存空间(malloc分配)
   *
   * @param ptr 指针
   */
  @SneakyThrows
  static void free(MemorySegment ptr) {
    FREE.invokeExact(ptr);
  }

  /**
   * 分配内存
   *
   * @param size 大小
   * @return 指针
   */
  @SneakyThrows
  static MemorySegment malloc(long size) {
    return (MemorySegment) MALLOC.invokeExact(size);
  }
}
