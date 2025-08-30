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

import com.silong.foundation.utilities.whispercpp.generated.whisper_encoder_begin_callback;
import com.silong.foundation.utilities.whispercpp.generated.whisper_encoder_begin_callback.Function;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;
import lombok.SneakyThrows;

/**
 * whisper_encoder_begin_callback
 * 是一个与模型编码阶段相关的回调函数，主要用于在编码器（Encoder）开始处理音频数据前触发特定逻辑，为开发者提供了在编码阶段启动时介入处理流程的机会。<br>
 * whisper.cpp 的语音转文字（ASR）过程主要分为两大阶段：<br>
 * 1、编码器（Encoder）阶段：对输入的音频数据进行特征提取和编码，将原始音频转换为模型可理解的特征表示。<br>
 * 2、解码器（Decoder）阶段：基于编码器输出的特征，生成对应的文本转录结果。<br>
 * whisper_encoder_begin_callback 专门在编码器开始工作前被调用，其核心价值在于：<br>
 * 1、允许开发者在编码启动前执行初始化操作（如日志记录、资源预热、参数校验等）。<br>
 * 2、支持在编码开始前决定是否中断整个处理流程（通过返回值控制）。<br>
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-04-22 18:45
 */
public record WhisperEncoderBeginCallback(
    String encoderBeginCallbackClass, String encoderBeginCallbackUserDataClass) {

  /**
   * 创建新的回调函数指针
   *
   * @return 回调函数指针
   */
  @SneakyThrows({
    ClassNotFoundException.class,
    NoSuchMethodException.class,
    InstantiationException.class,
    IllegalAccessException.class,
    IllegalArgumentException.class,
    InvocationTargetException.class
  })
  public MemorySegment encoderBeginCallback() {
    if (encoderBeginCallbackClass == null || encoderBeginCallbackClass.isBlank()) {
      throw new IllegalArgumentException("encoderBeginCallbackClass must not be null or blank.");
    }
    Class<?> clazz = Class.forName(encoderBeginCallbackClass);
    if (!clazz.isAssignableFrom(Function.class)) {
      throw new IllegalArgumentException(
          "encoderBeginCallbackClass must be a class that implements com.silong.foundation.utilities.whispercpp.generated.whisper_encoder_begin_callback.Function.");
    }
    return whisper_encoder_begin_callback.allocate(
        (Function) clazz.getDeclaredConstructor().newInstance(), Arena.global());
  }

  /**
   * 创建新的回调函数用户数据指针
   *
   * @return 回调函数用户数据指针
   */
  @SneakyThrows({
    ClassNotFoundException.class,
    NoSuchMethodException.class,
    InstantiationException.class,
    IllegalAccessException.class,
    IllegalArgumentException.class,
    InvocationTargetException.class
  })
  @SuppressWarnings("unchecked")
  public MemorySegment encoderBeginCallbackUserData() {
    if (encoderBeginCallbackUserDataClass == null || encoderBeginCallbackUserDataClass.isBlank()) {
      return MemorySegment.NULL;
    }
    Class<?> clazz = Class.forName(encoderBeginCallbackUserDataClass);
    if (!clazz.isAssignableFrom(Supplier.class)) {
      throw new IllegalArgumentException(
          "encoderBeginCallbackUserDataClass must be a class that implements java.util.function.Supplier<MemorySegment>.");
    }
    return ((Supplier<MemorySegment>) clazz.getDeclaredConstructor().newInstance()).get();
  }
}
