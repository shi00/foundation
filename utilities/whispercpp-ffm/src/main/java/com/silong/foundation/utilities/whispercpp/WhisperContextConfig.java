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

import static com.silong.foundation.utilities.whispercpp.WhisperAlignmentHeadsPreset.WHISPER_AHEADS_NONE;

import lombok.Data;

/**
 * whisper上下文配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-04-22 18:45
 */
@Data
public class WhisperContextConfig {

  /** 是否启用gpu加速，默认启用 */
  private boolean useGpu = true;

  /** 是否启用 Flash Attention 优化。Flash Attention 是一种高效的注意力机制实现，可减少 GPU 内存占用并提升注意力计算速度。 */
  private boolean flashAttn;

  /** 指定用于加速的 CUDA 设备编号。 当系统存在多个 NVIDIA GPU 时，通过该参数选择具体设备（默认通常为 0，即第一块 GPU）。 */
  private int gpuDevice;

  // [EXPERIMENTAL] Token-level timestamps with DTW
  /** 是否启用 DTW 算法优化令牌级时间戳。默认时间戳预测可能存在偏差，DTW 可通过对齐音频特征与文本令牌，提升每个单词 / 子词的时间戳精度（尤其适合字幕生成等场景）。 */
  private boolean dtwTokenTimestamps;

  /** DTW 对齐时使用的注意力头（attention heads）预设模式。默认值： WHISPER_AHEADS_NONE */
  private WhisperAlignmentHeadsPreset dtwAHeadsPreset = WHISPER_AHEADS_NONE;

  /** DTW 对齐时考虑的 “Top N” 候选令牌数量。 */
  private int dtwNTop;

  /** 手动指定参与 DTW 对齐的注意力头集合（覆盖 dtw_aheads_preset）。 */
  private WhisperAHeads dtwAHeads;

  /** DTW 计算时分配的内存大小。 */
  private long dtwMemSize = 1024 * 1024 * 128;
}
