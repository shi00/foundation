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

import static com.silong.foundation.utilities.whispercpp.generated.WhisperCpp_1.WHISPER_SAMPLE_RATE;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.File;
import java.io.InputStream;

/**
 * WhisperCpp接口
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-04-22 18:45
 */
public interface WhisperCpp extends AutoCloseable {

  /** 支持的音频采样率 */
  int SUPPORTED_SAMPLED_RATE = WHISPER_SAMPLE_RATE();

  /** 支持的声道数量 */
  int SUPPORTED_CHANNELS = 1;

  /** 支持的比特率 */
  int SUPPORT_BIT_RATE = SUPPORTED_CHANNELS * SUPPORTED_SAMPLED_RATE * 16;

  /**
   * 根据配置获取Whisper实例
   *
   * @param config 配置
   * @return 实例
   */
  @Nonnull
  static WhisperCpp getInstance(WhisperConfig config) {
    return new WhisperCppImpl(config);
  }

  /**
   * 识别语言文件语种，返回zh,en,es等语言缩写
   *
   * @param wavFile 文件
   * @return 语种
   * @throws Exception 异常
   */
  @Nullable
  String recognizeLanguage(File wavFile) throws Exception;

  /**
   * wav语音文件识别
   *
   * @param wavFile 文件
   * @return 识别文本
   * @throws Exception 异常
   */
  @Nullable
  String[] speech2Text(File wavFile) throws Exception;

  /**
   * wav语音识别
   *
   * @param inputStream 数据输入流
   * @return 识别文本
   * @throws Exception 异常
   */
  String[] speech2Text(InputStream inputStream) throws Exception;
}
