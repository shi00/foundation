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
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

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
    loadLibrary("libwhisper", "native-libs/" + getOSDetectedClassifier());
  }

  /** 上下文缓存 */
  private final GenericObjectPool<MemorySegment> whisperContextPtrPool;

  /** 根据配置生成的全量参数 */
  private final MemorySegment whisperFullParams;

  /** 根据配置生成的上下文参数 */
  private final MemorySegment whisperContextParams;

  /** 模型文件路径 */
  private final MemorySegment modelPath;

  /** 配置参数 */
  private final WhisperConfig config;

  /** whisper_context上下文对象池工厂 */
  private class WhisperContextFactory extends BasePooledObjectFactory<MemorySegment> {

    @Override
    public MemorySegment create() {
      MemorySegment ctx = whisper_init_from_file_with_params(modelPath, whisperContextParams);
      if (ctx == null || NULL.equals(ctx)) {
        throw new IllegalStateException(
            "Failed to create whisper_context by function of whisper_init_from_file_with_params.");
      }
      return ctx;
    }

    @Override
    public PooledObject<MemorySegment> wrap(MemorySegment ctx) {
      return new DefaultPooledObject<>(ctx);
    }

    @Override
    public void destroyObject(PooledObject<MemorySegment> pooledObject) {
      whisper_free(pooledObject.getObject());
    }
  }

  @Override
  public void close() {
    whisper_free_context_params(MemorySegment.ofAddress(whisperContextParams.address()));
    whisper_free_params(MemorySegment.ofAddress(whisperFullParams.address()));
    free(modelPath);
    whisperContextPtrPool.close();
    log.info("Shut down the whisper.cpp service and release all associated resources.");
  }

  private GenericObjectPool<MemorySegment> newWhisperContextPtrPool(
      WhisperContextPoolConfig config) {
    // 1. 配置池参数（GenericObjectPoolConfig）
    GenericObjectPoolConfig<MemorySegment> poolConfig = new GenericObjectPoolConfig<>();
    // 核心参数配置（根据业务调整）
    poolConfig.setMaxTotal(config.getMaxTotal()); // 池内对象的最大总数（最多创建5个连接）
    poolConfig.setMaxIdle(config.getMaxIdle()); // 池内最大空闲对象数（空闲时最多保留3个连接）
    poolConfig.setMinIdle(config.getMinIdle()); // 池内最小空闲对象数（空闲时至少保留1个连接，避免频繁创建）
    poolConfig.setBlockWhenExhausted(
        config.isBlockWhenExhausted()); // 当池无可用对象时，是否阻塞等待（true：阻塞；false：立即抛异常）
    poolConfig.setMaxWait(config.getMaxWait()); // 阻塞等待的最大时间（3秒，超时抛NoSuchElementException）

    // 2. 创建对象工厂实例
    WhisperContextFactory factory = new WhisperContextFactory();

    // 3. 创建对象池（将配置和工厂传入）
    return new GenericObjectPool<>(factory, poolConfig);
  }

  /**
   * 构造方法
   *
   * @param config whisper配置
   */
  WhisperCppImpl(@NonNull WhisperConfig config) {
    this.config = config;
    this.whisperContextPtrPool = newWhisperContextPtrPool(config.getPoolConfig());
    MemorySegment systemInfo = NULL;
    try {
      systemInfo = whisper_print_system_info();
      if (systemInfo == null || NULL.equals(systemInfo)) {
        throw new IllegalStateException("Failed to collect the information of system.");
      }
      log.info("system_info: {}", systemInfo.getString(0, UTF_8));
      whisperFullParams = config.buildWhisperFullParams();
      if (whisperFullParams == null || NULL.equals(whisperFullParams)) {
        throw new IllegalStateException("Failed to build whisper_full_params for whisper.cpp.");
      }
      log.info("{}", whisperFullParams2String(whisperFullParams));

      whisperContextParams = config.buildWhisperContextParams();
      if (whisperContextParams == null || NULL.equals(whisperContextParams)) {
        throw new IllegalStateException("Failed to build whisper_context_params for whisper.cpp.");
      }
      log.info("{}", whisperContextParams2String(whisperContextParams));

      modelPath = Arena.global().allocateFrom(validateModelPath(config.getModelPath()), UTF_8);
      if (modelPath == null || NULL.equals(modelPath)) {
        throw new IllegalStateException("Failed to build model_path for whisper.cpp.");
      }
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
          int index = whisper_lang_auto_detect(ctxPtr, 0, config.getNThreads(), NULL);
          if (index == -1) {
            log.error(
                "Failed to detect the language of the multimedia file: {}",
                wavFile.getAbsolutePath());
            return null;
          }
          return whisper_lang_str(index).getString(0, UTF_8);
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
    MemorySegment ctxPtr = null;
    try (Arena arena = Arena.ofConfined()) {
      ctxPtr = whisperContextPtrPool.borrowObject();

      MemorySegment pcmf32Ptr = arena.allocateFrom(C_FLOAT, pcmf32);
      int retCode = whisper_full(ctxPtr, whisperFullParams, pcmf32Ptr, pcmf32.length);
      if (retCode != 0) {
        log.error("Failed to execute whisper_full with errCode:{}", retCode);
        return null;
      }
      return contextProcessor.apply(arena, ctxPtr);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to borrow whisper_context from objectsPool.", e);
    } finally {
      if (ctxPtr != null) {
        whisperContextPtrPool.returnObject(ctxPtr);
      }
    }
  }

  private static String[] collectResultFromCtx(MemorySegment ctxPtr) {
    return IntStream.range(0, whisper_full_n_segments(ctxPtr))
        .mapToObj(i -> whisper_full_get_segment_text(ctxPtr, i))
        .filter(ms -> !NULL.equals(ms) && ms != null)
        .map(ms -> ms.getString(0, UTF_8))
        .peek(str -> log.debug("{}", str))
        .toArray(String[]::new);
  }
}
