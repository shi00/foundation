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

import static com.silong.foundation.utilities.whispercpp.generated.WhisperCpp_1.*;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Whisper 头对齐预设值
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-04-22 16:27
 */
@AllArgsConstructor
@Getter
public enum WhisperAlignmentHeadsPreset {
  WHISPER_AHEADS_NONE(WHISPER_AHEADS_NONE()),
  WHISPER_AHEADS_N_TOP_MOST(
      WHISPER_AHEADS_N_TOP_MOST()), // All heads from the N-top-most text-layers
  WHISPER_AHEADS_CUSTOM(WHISPER_AHEADS_CUSTOM()),
  WHISPER_AHEADS_TINY_EN(WHISPER_AHEADS_TINY_EN()),
  WHISPER_AHEADS_TINY(WHISPER_AHEADS_TINY()),
  WHISPER_AHEADS_BASE_EN(WHISPER_AHEADS_BASE_EN()),
  WHISPER_AHEADS_BASE(WHISPER_AHEADS_BASE()),
  WHISPER_AHEADS_SMALL_EN(WHISPER_AHEADS_SMALL_EN()),
  WHISPER_AHEADS_SMALL(WHISPER_AHEADS_SMALL()),
  WHISPER_AHEADS_MEDIUM_EN(WHISPER_AHEADS_MEDIUM_EN()),
  WHISPER_AHEADS_MEDIUM(WHISPER_AHEADS_MEDIUM()),
  WHISPER_AHEADS_LARGE_V1(WHISPER_AHEADS_LARGE_V1()),
  WHISPER_AHEADS_LARGE_V2(WHISPER_AHEADS_LARGE_V2()),
  WHISPER_AHEADS_LARGE_V3(WHISPER_AHEADS_LARGE_V3());

  private final int value;
}
