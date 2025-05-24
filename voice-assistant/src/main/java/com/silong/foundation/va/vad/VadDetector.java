/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 *  under the License.
 *
 */

package com.silong.foundation.va.vad;

/**
 * Voice Activity Detector
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-04-18 16:12
 */
public interface VadDetector extends AutoCloseable {

  /**
   * 检测音频数据是否包含语音
   *
   * @param audioData 音频采样数据
   * @throws Exception 异常
   * @return 是否有语音对话，true or false
   */
  boolean isSpeech(byte[] audioData) throws Exception;

  /**
   * 检测音频数据是否包含语音
   *
   * @param audioData 音频采样数据
   * @param offset offset
   * @param length length
   * @throws Exception 异常
   * @return 是否有语音对话，true or false
   */
  boolean isSpeech(byte[] audioData, int offset, int length) throws Exception;
}
