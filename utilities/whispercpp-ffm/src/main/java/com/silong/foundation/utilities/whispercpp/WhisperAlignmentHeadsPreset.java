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

/**
 * 参数校验工具
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-04-22 18:45
 */
public enum WhisperAlignmentHeadsPreset {
  /** 含义：不使用任何预设的注意力头集合。 作用：通常配合 dtw_aheads 参数使用，即完全依赖用户手动指定的注意力头（whisper_aheads 结构体）进行对齐计算。 */
  WHISPER_AHEADS_NONE,

  /**
   * 含义：使用 “顶部 N 层” 文本编码器中所有的注意力头。 作用：“顶部 N 层” 指模型中靠近输出的高层（通常捕捉全局语义特征），适用于对全局时序对齐要求较高的场景（如长句时间戳划分）。
   */
  WHISPER_AHEADS_N_TOP_MOST, // All heads from the N-top-most text-layers

  /** 含义：自定义模式（需结合其他参数手动配置）。 作用：预留的扩展模式，可能用于未来支持更灵活的自定义策略（目前使用较少）。 */
  WHISPER_AHEADS_CUSTOM,

  /** 适用模型：tiny.en（英文专用）、tiny（多语言）。 含义：针对 tiny 规模模型优化的注意力头集合（tiny 模型参数最少，层数和注意力头数量较少）。 */
  WHISPER_AHEADS_TINY_EN,
  WHISPER_AHEADS_TINY,

  /** 适用模型：base.en（英文专用）、base（多语言）。 含义：针对 base 规模模型的预设，相比 tiny 模型包含更多层和注意力头，对齐精度更高。 */
  WHISPER_AHEADS_BASE_EN,
  WHISPER_AHEADS_BASE,

  /** 适用模型：small.en（英文专用）、small（多语言）。 含义：针对 small 规模模型的预设，适合对精度有一定要求但算力有限的场景。 */
  WHISPER_AHEADS_SMALL_EN,
  WHISPER_AHEADS_SMALL,

  /** 适用模型：medium.en（英文专用）、medium（多语言）。 含义：针对 medium 规模模型的预设，平衡精度和计算成本，适合中等算力设备。 */
  WHISPER_AHEADS_MEDIUM_EN,
  WHISPER_AHEADS_MEDIUM,

  /**
   * 适用模型：large-v1、large-v2、large-v3（均为多语言大模型，版本迭代）。 含义：针对 large
   * 系列大模型的预设，包含更多注意力头和更深层的配置，对齐精度最高（但计算成本也最高），适合对时间戳精度要求极高的场景（如专业字幕生成）。
   */
  WHISPER_AHEADS_LARGE_V1,
  WHISPER_AHEADS_LARGE_V2,
  WHISPER_AHEADS_LARGE_V3,

  /**
   * 适用模型：large-v3-turbo（大模型的加速版本，推理更快）。 含义：针对 large-v3-turbo 模型优化的预设，在保证较高精度的同时，适配模型的加速特性，减少计算耗时。
   */
  WHISPER_AHEADS_LARGE_V3_TURBO;

  public static WhisperAlignmentHeadsPreset fromOriginal(int original) {
    return switch (original) {
      case 0 -> WHISPER_AHEADS_NONE;
      case 1 -> WHISPER_AHEADS_N_TOP_MOST;
      case 2 -> WHISPER_AHEADS_CUSTOM;
      case 3 -> WHISPER_AHEADS_TINY_EN;
      case 4 -> WHISPER_AHEADS_TINY;
      case 5 -> WHISPER_AHEADS_BASE_EN;
      case 6 -> WHISPER_AHEADS_BASE;
      case 7 -> WHISPER_AHEADS_SMALL_EN;
      case 8 -> WHISPER_AHEADS_SMALL;
      case 9 -> WHISPER_AHEADS_MEDIUM_EN;
      case 10 -> WHISPER_AHEADS_MEDIUM;
      case 11 -> WHISPER_AHEADS_LARGE_V1;
      case 12 -> WHISPER_AHEADS_LARGE_V2;
      case 13 -> WHISPER_AHEADS_LARGE_V3;
      case 14 -> WHISPER_AHEADS_LARGE_V3_TURBO;
      default -> throw new IllegalArgumentException("Unknown alignment heads preset: " + original);
    };
  }
}
