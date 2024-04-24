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

import static com.silong.foundation.utilities.whispercpp.WhisperCppImpl.free;
import static com.silong.foundation.utilities.whispercpp.generated.WhisperCpp_1.*;
import static java.lang.foreign.Arena.global;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.lang.foreign.MemorySegment;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支持的语言枚举
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-04-24 20:51
 */
@Getter
@AllArgsConstructor
public enum Language {
  en(global().allocateFrom("en", UTF_8)),
  zh(global().allocateFrom("zh", UTF_8)),
  es(global().allocateFrom("es", UTF_8)),
  ru(global().allocateFrom("ru", UTF_8)),
  de(global().allocateFrom("de", UTF_8));

  /** c指针 */
  private final MemorySegment charPtr;

  /**
   * 返回语言id
   *
   * @return 语言id
   */
  public int getLangId() {
    return whisper_lang_id(charPtr);
  }

  /**
   * 获取语言缩写，如：en,zh,de等等
   *
   * @return 语言
   */
  public String getLangStr() {
    return getLangStr(getLangId());
  }

  /**
   * 根据语言id查找语言缩写
   *
   * @param langId 语言id
   * @return 语言
   */
  public static String getLangStr(int langId) {
    MemorySegment charPtr = whisper_lang_str(langId);
    try {
      return charPtr.getString(0, UTF_8);
    } finally {
      free(charPtr);
    }
  }
}
