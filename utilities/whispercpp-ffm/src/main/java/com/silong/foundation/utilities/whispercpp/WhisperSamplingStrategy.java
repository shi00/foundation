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

import static com.silong.foundation.utilities.whispercpp.generated.WhisperCpp_1.WHISPER_SAMPLING_BEAM_SEARCH;
import static com.silong.foundation.utilities.whispercpp.generated.WhisperCpp_1.WHISPER_SAMPLING_GREEDY;
import static java.util.stream.Collectors.joining;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * whisper 采样策略
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-04-22 16:25
 */
@AllArgsConstructor
@Getter
public enum WhisperSamplingStrategy {
  WHISPER_SAMPLING_GREEDY(WHISPER_SAMPLING_GREEDY()), // similar to OpenAI's GreedyDecoder
  WHISPER_SAMPLING_BEAM_SEARCH(
      WHISPER_SAMPLING_BEAM_SEARCH()); // similar to OpenAI's BeamSearchDecoder

  private final int value;

  /**
   * 根据value值解析枚举类型
   *
   * @param value 值
   * @return 枚举
   */
  public static WhisperSamplingStrategy parse(int value) {
    WhisperSamplingStrategy[] values = WhisperSamplingStrategy.values();
    return Arrays.stream(values)
        .filter(v -> v.getValue() == value)
        .findAny()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    String.format(
                        "value: %d, values: %s",
                        value,
                        Arrays.stream(values).map(Enum::name).collect(joining(", ", "[", "]")))));
  }
}
