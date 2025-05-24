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

import lombok.Data;

/**
 * 音频采样配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-05-23 21:32
 */
@Data
public class SoundRecorderProperties {

  /** the number of samples per second */
  private float samplingRate = 16000.0f;

  /** the number of bits in each sample */
  private int sampleSizeInBits = 16;

  /** the number of channels (1 for mono, 2 for stereo, and so on) */
  private int channels = 1;

  /** indicates whether the data is signed or unsigned */
  private boolean signed = true;

  /**
   * indicates whether the data for a single sample is stored in big-endian byte order (false means
   * little-endian)
   */
  private boolean bigEndian = false;
}
