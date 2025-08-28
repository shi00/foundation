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

import static java.lang.Float.MAX_VALUE;

import lombok.Data;

/**
 * 声音检测配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-04-22 18:45
 */
@Data
public class VadConfig {
  /**
   * 判断一段音频为 “语音” 的概率阈值。当模型预测某段音频是语音的概率高于此值时，判定为语音。 取值范围：0.0 ~ 1.0。典型值：0.5（默认值，平衡灵敏度）；如需更灵敏（减少漏检）可设为
   * 0.3~0.4；如需更严格（减少误检）可设为 0.6~0.7
   */
  private float threshold = 0.5f;

  /** min_speech_duration_ms表示一段音频被判定为 “有效语音片段” 的最小持续时间，单位：毫秒。短于该时长的语音会被视为噪音过滤。 */
  private int minSpeechDurationMs = 250;

  /** min_silence_duration_ms表示语音片段结束后，需要持续静音多久才判定为 “语音结束”，单位：毫秒。用于分割连续语音（如短句间的停顿）。 */
  private int minSilenceDurationMs = 100;

  /** max_speech_duration_s表示强制分割长语音片段的最大时长，单位：秒。即使没有检测到足够长的静音，超过此时长的语音也会被截断为新片段。 */
  private float maxSpeechDurationS = MAX_VALUE;

  /** speech_pad_ms表示在检测到的语音片段前后额外添加的静音时长，避免截断语音开头 / 结尾（如说话前的吸气、结尾的拖音）。单位：毫秒 */
  private int speechPadMs = 30;

  /** samples_overlap表示相邻语音片段之间的重叠时长，用于避免分割点处的语音信息丢失（如单词被截断）。 */
  private float samplesOverlap = 0.1f;

  /** 是否开启VAD功能，默认不开启 */
  private boolean enable;

  /** vad模型路径 */
  private String modelPath;
}
