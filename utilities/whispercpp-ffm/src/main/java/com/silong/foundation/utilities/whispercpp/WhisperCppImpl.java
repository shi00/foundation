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

import static com.silong.foundation.utilities.nlloader.NativeLibLoader.loadLibrary;
import static com.silong.foundation.utilities.whispercpp.ParamsValidator.validateModelPath;
import static com.silong.foundation.utilities.whispercpp.Utils.*;
import static com.silong.foundation.utilities.whispercpp.generated.WhisperCpp.*;
import static java.lang.foreign.MemorySegment.NULL;
import static java.nio.charset.StandardCharsets.UTF_8;

import jakarta.annotation.Nullable;
import java.io.*;
import java.lang.foreign.*;
import java.nio.FloatBuffer;
import java.util.stream.IntStream;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.*;

/**
 * 语音识别
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-04-20 15:33
 */
@Slf4j
class WhisperCppImpl implements WhisperCpp {

  static {
    // 加载共享库
    loadLibrary("libwhisper", "libs");
  }

  /** 根据配置生成的全量参数 */
  private final MemorySegment whisperFullParams;

  /** 根据配置生成的上下文参数 */
  private final MemorySegment whisperContextParams;

  /** 模型文件路径 */
  private final MemorySegment modelPath;

  /** 配置参数 */
  private final WhisperConfig config;

  @Override
  public void close() {
    whisper_free_context_params(MemorySegment.ofAddress(whisperContextParams.address()));
    whisper_free_params(MemorySegment.ofAddress(whisperFullParams.address()));
    free(modelPath);
  }

  /**
   * 构造方法
   *
   * @param config whisper配置
   */
  WhisperCppImpl(@NonNull WhisperConfig config) {
    this.config = config;
    MemorySegment systemInfo = NULL;
    try {
      systemInfo = whisper_print_system_info();
      log.info("system_info: {}", systemInfo == NULL ? null : systemInfo.getString(0, UTF_8));
      whisperFullParams = config.buildWhisperFullParams();
      log.info("{}", whisperFullParams2String(whisperFullParams));

      whisperContextParams = config.buildWhisperContextParams();
      log.info("{}", whisperContextParams2String(whisperContextParams));

      modelPath = Arena.global().allocateFrom(validateModelPath(config.getModelPath()), UTF_8);
      log.info("modelPath: {}", modelPath.getString(0, UTF_8));
    } finally {
      free(systemInfo);
    }
  }

  @Nullable
  @Override
  public String recognizeLanguage(File wavFile) throws Exception {
    return "";
  }

  @Nullable
  @Override
  public String[] speech2Text(@NonNull File wavFile) throws Exception {
    return speech2Text(new FileInputStream(wavFile));
  }

  @Nullable
  @Override
  public String[] speech2Text(@NonNull InputStream inputStream) throws Exception {
    MemorySegment ctxPtr = NULL;
    try (Arena arena = Arena.ofConfined()) {
      ctxPtr = whisper_init_from_file_with_params(modelPath, whisperContextParams);
      float[] pcmf32 = convertToPcmf32(inputStream);
      MemorySegment pcmf32Ptr = arena.allocateFrom(C_FLOAT, pcmf32);
      int retCode =
          whisper_full_parallel(
              ctxPtr, whisperFullParams, pcmf32Ptr, pcmf32.length, config.getNThreads());
      if (retCode != 0) {
        log.error("Failed to execute whisper_full_parallel with errCode:{}", retCode);
        return null;
      }
      return collectResultFromCtx(ctxPtr);
    } finally {
      whisper_free(ctxPtr);
    }
  }

  private static String[] collectResultFromCtx(MemorySegment ctxPtr) {
    return IntStream.range(0, whisper_full_n_segments(ctxPtr))
        .mapToObj(i -> whisper_full_get_segment_text(ctxPtr, i))
        .filter(ms -> ms != NULL && ms != null)
        .map(ms -> ms.getString(0, UTF_8))
        .toArray(String[]::new);
  }

  /**
   * 直接将输入音频文件转换为whisper_full所需的float[]（pcmf32格式） 无需中间文件，全程内存处理
   *
   * @param inputStream 输入音频流
   * @return 符合要求的float[]数组（单声道、16000Hz、范围[-1.0, 1.0]）
   * @throws Exception 处理过程中发生错误时抛出
   */
  private static float[] convertToPcmf32(InputStream inputStream) throws Exception {
    try ( // 1. 初始化抓取器，读取输入音频
    var grabber = new FFmpegFrameGrabber(inputStream)) {

      // 配置抓取器直接输出目标格式
      grabber.setSampleRate(SUPPORTED_SAMPLED_RATE); // 强制采样率为16000Hz
      grabber.setAudioChannels(SUPPORTED_CHANNELS); // 强制单声道
      grabber.setAudioBitrate(SUPPORTED_BIT_RATE); // 设置比特率

      log.info(
          "Start initializing the audio grabber, sampleRate: {}Hz, audioChannels: {}, audioBitrate: {}bps",
          grabber.getSampleRate(),
          grabber.getAudioChannels(),
          grabber.getAudioBitrate());

      grabber.start();

      // 预估容量，避免频繁扩容
      int estimatedFrames = grabber.getLengthInFrames();
      int initialCapacity = estimatedFrames > 0 ? (int) (estimatedFrames * 1.1) : 1024 * 1024;
      FloatBuffer buffer = FloatBuffer.allocate(initialCapacity);
      log.info("Audio conversion started, initial buffer capacity: {}", initialCapacity);

      Frame frame;
      long startTime = System.currentTimeMillis();
      while ((frame = grabber.grabFrame()) != null) {
        if (frame.samples == null || frame.samples.length == 0) {
          continue;
        }

        // 检查缓冲区类型
        if (!(frame.samples[0] instanceof FloatBuffer frameBuffer)) {
          throw new IllegalStateException(
              "Unsupported buffer type: " + frame.samples[0].getClass());
        }
        frameBuffer.flip();

        int frameSamples = frameBuffer.remaining();
        if (frameSamples > 0) {
          // 确保缓冲区容量
          buffer = ensureCapacity(buffer, frameSamples);
          buffer.put(frameBuffer);
        }
      }

      // 转换为最终数组
      buffer.flip();
      float[] pcmf32 = new float[buffer.limit()];
      buffer.get(pcmf32);

      long duration = System.currentTimeMillis() - startTime;
      log.info(
          "Audio conversion completed, total samples: {}, duration: {}ms, rate: {}/ms",
          pcmf32.length,
          duration,
          (int) (pcmf32.length / Math.max(1, duration)));
      return pcmf32;
    }
  }

  // 新增缓冲区扩容方法
  private static FloatBuffer ensureCapacity(FloatBuffer buffer, int required) {
    if (buffer.remaining() >= required) {
      return buffer;
    }
    int newCapacity = Math.max(buffer.capacity() + required, buffer.capacity() * 2);
    FloatBuffer newBuffer = FloatBuffer.allocate(newCapacity);
    buffer.flip();
    newBuffer.put(buffer);
    log.debug("Buffer expanded to {} (required: {})", newCapacity, required);
    return newBuffer;
  }
}
