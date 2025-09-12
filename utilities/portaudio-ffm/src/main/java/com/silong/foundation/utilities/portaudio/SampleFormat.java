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

import static com.silong.foundation.utilities.portaudio.generated.PortAudio.*;

/**
 * 音频数据采样格式
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-09-11 14:52
 */
public enum SampleFormat {
  /** 32 位浮点型格式（单精度浮点数）。 - 取值范围：-1.0（最小振幅）到 +1.0（最大振幅）； - 精度高，适合专业音频处理（如音效渲染、实时信号分析） */
  paFloat32,
  /** 32 位有符号整数格式。 - 用 32 位整数表示振幅，动态范围约为 ±2147483647； - 精度高于 16 位整数，适合高质量录音。 */
  paInt32,
  /** 24 位有符号整数格式（压缩存储）。 - 动态范围约为 ±8388607，精度介于 16 位和 32 位之间； - 通常以 3 字节（24 位）紧凑存储，不填充为 32 位。 */
  paInt24,
  /** 16 位有符号整数格式。 - 动态范围约为 ±32767，是最常用的格式之一； - 兼容性好，硬件支持广泛，存储开销适中。 */
  paInt16,
  /** 8 位有符号整数格式。 - 动态范围约为 ±127，精度较低，容易产生量化噪声； - 存储开销最小（1 字节 / 样本）。 */
  paInt8,
  /** 8 位无符号整数格式。 - 以 128 为基准值（表示 “静音”），0 表示最大负振幅，255 表示最大正振幅； - 精度与 paInt8 相同，但基准值不同。 */
  paUInt8,
  /** 自定义格式。 - 用于表示上述标准格式之外的特殊格式（如 32 位定点数、浮点扩展格式等）； - 需要配合特定硬件和驱动支持，需额外处理格式解析。 */
  paCustomFormat,
  /**
   * 非交错模式标志（仅作为 “标志位” 使用，需与上述基础格式组合）。 - 表示音频数据按 “声道分离存储”：每个声道单独占用一个缓冲区（如左声道一个数组，右声道另一个数组）； - 通过
   * “按位或” 与基础格式组合（如 `paFloat32 | paNonInterleaved` 表示 32 位浮点型非交错模式）。
   */
  paNonInterleaved;

  /**
   * 对应c代码中对应的枚举值
   *
   * @return 枚举值
   */
  int value() {
    return switch (this) {
      case paFloat32 -> paFloat32();
      case paInt32 -> paInt32();
      case paInt24 -> paInt24();
      case paInt16 -> paInt16();
      case paInt8 -> paInt8();
      case paUInt8 -> paUInt8();
      case paCustomFormat -> paCustomFormat();
      case paNonInterleaved -> paNonInterleaved();
    };
  }
}
