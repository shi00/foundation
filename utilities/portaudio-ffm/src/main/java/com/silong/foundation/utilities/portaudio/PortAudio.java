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

import java.time.Duration;

/**
 * portaudio
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-09-11 14:52
 */
public interface PortAudio extends AutoCloseable {
  /**
   * 获取实例
   *
   * @return 实例
   */
  static PortAudio getInstance() {
    return PortAudioImpl.INSTANCE;
  }

  /**
   * 开始录音
   *
   * @param sampleRate 采样率
   * @param channels 声道数
   * @param audioChunkDuration 音频块时长
   * @param parallel 是否并行处理，回调函数是否多线程并行调用处理
   * @param processor 音频块回调函数
   * @throws Exception 异常
   */
  void start(
      int sampleRate,
      int channels,
      Duration audioChunkDuration,
      boolean parallel,
      AudioChunkProcessor processor)
      throws Exception;
}
