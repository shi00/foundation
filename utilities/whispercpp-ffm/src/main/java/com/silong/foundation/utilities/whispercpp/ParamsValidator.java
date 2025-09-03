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

import static com.silong.foundation.utilities.whispercpp.generated.WhisperCpp.whisper_lang_id;
import static java.lang.foreign.MemorySegment.NULL;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Locale.ROOT;

import java.io.File;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

/**
 * 参数校验工具
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-04-22 18:45
 */
interface ParamsValidator {

  /**
   * 校验模型文件路径有效性
   *
   * @param modelPath 模型文件路径
   */
  static String validateModelPath(String modelPath) {
    var f = new File(modelPath);
    if (!f.exists() || !f.canRead() || !f.isFile()) {
      throw new IllegalArgumentException("Invalid model file: " + f.getAbsolutePath());
    }
    return f.getAbsolutePath();
  }

  /**
   * 校验模型是否支持指定语言
   *
   * @param arena 内存区域
   * @param language 语言
   */
  static MemorySegment validateSupportedLanguage(Arena arena, String language) {
    MemorySegment lang;
    if (language == null || "auto".equals(language.trim().toLowerCase(ROOT))) {
      lang = NULL;
    } else {
      lang = arena.allocateFrom(language.trim().toLowerCase(ROOT), UTF_8);
      if (whisper_lang_id(lang) == -1) {
        throw new IllegalArgumentException("Unsupported language: " + language);
      }
    }
    return lang;
  }
}
