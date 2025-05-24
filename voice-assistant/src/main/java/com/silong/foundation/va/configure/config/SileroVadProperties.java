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

package com.silong.foundation.va.configure.config;

import static com.silong.foundation.va.configure.config.SileroVadProperties.FrameSize.FRAME_SIZE_512;
import static com.silong.foundation.va.configure.config.SileroVadProperties.Mode.NORMAL;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

/**
 * SileroVAD配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-05-24 21:13
 */
@Data
public class SileroVadProperties {
  /** 模型文件路径 */
  @NotEmpty private String modelPath = "models/silero_vad.onnx";

  /** 采样检测窗口大小 */
  @NotNull private FrameSize frameSize = FRAME_SIZE_512;

  /** The confidence mode of the VAD model. */
  @NotNull private Mode mode = NORMAL;

  /** The minimum duration in milliseconds for silence segments */
  @Max(300000)
  @Min(0)
  private int silenceDurationMs = 300;

  /** The minimum duration in milliseconds for speech segments */
  @Max(300000)
  @Min(0)
  private int speechDurationMs = 50;

  /**
   * 描述信息
   *
   * @author louis sin
   * @version 1.0.0
   * @since 2024-05-25 15:56
   */
  @Getter
  @AllArgsConstructor
  public enum Mode {
    OFF,
    NORMAL,
    AGGRESSIVE,
    VERY_AGGRESSIVE;
  }

  @Getter
  @AllArgsConstructor
  public enum FrameSize {
    FRAME_SIZE_512(512),
    FRAME_SIZE_1024(1024),
    FRAME_SIZE_1536(1536);
    private final int value;
  }
}
