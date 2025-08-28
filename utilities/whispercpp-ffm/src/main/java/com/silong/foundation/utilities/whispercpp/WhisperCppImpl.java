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
import static com.silong.foundation.utilities.whispercpp.Pcmf32Extractor.extract;
import static com.silong.foundation.utilities.whispercpp.Utils.*;
import static com.silong.foundation.utilities.whispercpp.generated.WhisperCpp.*;
import static java.lang.foreign.MemorySegment.NULL;
import static java.nio.charset.StandardCharsets.UTF_8;

import jakarta.annotation.Nullable;
import java.io.*;
import java.lang.foreign.*;
import java.util.function.BiFunction;
import java.util.stream.IntStream;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

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
    loadLibrary("libwhisper", "native_libs");
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
      log.info("system_info: {}", NULL.equals(systemInfo) ? null : systemInfo.getString(0, UTF_8));
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
    return analyze(
        extract(wavFile),
        (arena, ctxPtr) -> {
          int retCode = whisper_lang_auto_detect(ctxPtr, 0, config.getNThreads(), NULL);
          if (retCode == -1) {
            log.error(
                "Failed to detect the language of the multimedia file: {}",
                wavFile.getAbsolutePath());
            return null;
          }
          return whisper_lang_str(retCode).getString(0, UTF_8);
        });
  }

  @Nullable
  @Override
  public String[] speech2Text(@NonNull File wavFile) throws Exception {
    return speech2Text(new FileInputStream(wavFile));
  }

  @Nullable
  @Override
  public String[] speech2Text(@NonNull InputStream inputStream) throws Exception {
    return analyze(extract(inputStream), (arena, ctxPtr) -> collectResultFromCtx(ctxPtr));
  }

  private <T> T analyze(
      float[] pcmf32, @NonNull BiFunction<Arena, MemorySegment, T> contextProcessor) {
    MemorySegment ctxPtr = NULL;
    try (Arena arena = Arena.ofConfined()) {
      ctxPtr = whisper_init_from_file_with_params(modelPath, whisperContextParams);
      if (ctxPtr == null || NULL.equals(ctxPtr)) {
        log.error("Failed to initialize whisperContext with model:{}", modelPath);
        return null;
      }

      MemorySegment pcmf32Ptr = arena.allocateFrom(C_FLOAT, pcmf32);
      int retCode =
          whisper_full_parallel(
              ctxPtr, whisperFullParams, pcmf32Ptr, pcmf32.length, config.getNThreads());
      if (retCode != 0) {
        log.error("Failed to execute whisper_full_parallel with errCode:{}", retCode);
        return null;
      }
      return contextProcessor.apply(arena, ctxPtr);
    } finally {
      whisper_free(ctxPtr);
    }
  }

  private static String[] collectResultFromCtx(MemorySegment ctxPtr) {
    return IntStream.range(0, whisper_full_n_segments(ctxPtr))
        .mapToObj(i -> whisper_full_get_segment_text(ctxPtr, i))
        .filter(ms -> !NULL.equals(ms) && ms != null)
        .map(ms -> ms.getString(0, UTF_8))
        .toArray(String[]::new);
  }
}
