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

package com.silong.foundation.utilities.whispercpp;

import static com.silong.foundation.utilities.whispercpp.generated.WhisperCpp_1.C_INT;
import static com.silong.foundation.utilities.whispercpp.generated.whisper_full_params.beam_search.beam_size;
import static com.silong.foundation.utilities.whispercpp.generated.whisper_full_params.beam_search.patience;
import static com.silong.foundation.utilities.whispercpp.generated.whisper_full_params.greedy.best_of;
import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.*;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.joining;
import static javax.sound.sampled.AudioFileFormat.Type.WAVE;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import javax.sound.sampled.AudioFormat;
import lombok.NonNull;
import lombok.SneakyThrows;

/**
 * 工具类
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-04-28 9:16
 */
interface Utils {

  Linker LINKER = Linker.nativeLinker();

  // 临时目录
  Path TEMP_DIR = Paths.get(System.getProperty("java.io.tmpdir"));

  MethodHandle FREE =
      LINKER.downcallHandle(
          LINKER.defaultLookup().find("free").orElseThrow(), FunctionDescriptor.ofVoid(ADDRESS));

  /**
   * 释放内存空间(malloc分配)
   *
   * @param ptr 指针
   */
  @SneakyThrows
  static void free(@NonNull MemorySegment ptr) {
    FREE.invokeExact(ptr);
  }

  static String beamSearchToString(@NonNull MemorySegment beamSearch) {
    return NULL.equals(beamSearch)
        ? "null"
        : String.format(
            "beam_search{beam_size = %d, patience = %f}",
            beam_size(beamSearch), patience(beamSearch));
  }

  static String greedyToString(@NonNull MemorySegment greedy) {
    return NULL.equals(greedy) ? "null" : String.format("greedy{best_of = %d}", best_of(greedy));
  }

  static String valueArrayToString(
      @NonNull MemorySegment valueArrayPtr, @NonNull ValueLayout valueLayout, int elements) {
    if (elements < 0) {
      throw new IllegalArgumentException("elements must be greater than or equal to 0.");
    }
    return NULL.equals(valueArrayPtr)
        ? "null"
        : valueArrayPtr
            .elements(MemoryLayout.sequenceLayout(elements, valueLayout))
            .map(m -> String.valueOf(m.get(C_INT, 0)))
            .collect(joining(", ", "[", "]"));
  }

  static String charPtr2ToString(@NonNull MemorySegment charPtr, String returnValueWhenNULLPtr) {
    return NULL.equals(charPtr) ? returnValueWhenNULLPtr : charPtr.getString(0, UTF_8);
  }

  static String toString(String[] strings) {
    return strings == null ? "null" : Arrays.stream(strings).collect(joining(", ", "[", "]"));
  }

  /**
   * 音频格式比较
   *
   * @param af1 格式1
   * @param af2 格式2
   * @return true or false
   */
  static boolean equals(AudioFormat af1, AudioFormat af2) {
    return af1 != null
        && af2 != null
        && Objects.equals(af1.getEncoding(), af2.getEncoding())
        && af1.getChannels() == af2.getChannels()
        && af1.isBigEndian() == af2.isBigEndian()
        && af1.getFrameRate() == af2.getFrameRate()
        && af1.getSampleRate() == af2.getSampleRate()
        && af1.getFrameSize() == af2.getFrameSize()
        && af1.getSampleSizeInBits() == af2.getSampleSizeInBits();
  }

  /**
   * 时间格式化
   *
   * @param millis 毫秒时间
   * @return 格式化结果
   */
  static String format2HHmmssSSS(long millis) {
    long hours = MILLISECONDS.toHours(millis);
    long minutes = MILLISECONDS.toMinutes(millis);
    long seconds = MILLISECONDS.toSeconds(millis);
    return String.format(
        "%02d:%02d:%02d.%03d",
        hours,
        minutes - HOURS.toMinutes(hours),
        seconds - MINUTES.toSeconds(minutes),
        millis - SECONDS.toMillis(seconds));
  }

  /**
   * 调用无参数public构造方法构造实例
   *
   * @param aClass class
   * @return 实例
   * @param <T> 类型
   */
  @SneakyThrows
  @SuppressWarnings("unchecked")
  static <T> T newInstance(@NonNull Class<?> aClass) {
    return (T) aClass.getDeclaredConstructor().newInstance();
  }

  /**
   * 生成临时文件
   *
   * @return 临时文件
   * @throws IOException 异常
   */
  @SuppressFBWarnings(
      value = "PATH_TRAVERSAL_IN",
      justification = "Create a temporary file in the temporary directory.")
  static Path createTempFile() throws IOException {
    return Files.createTempFile(TEMP_DIR, "whisper-cpp", "." + WAVE.getExtension());
  }
}
