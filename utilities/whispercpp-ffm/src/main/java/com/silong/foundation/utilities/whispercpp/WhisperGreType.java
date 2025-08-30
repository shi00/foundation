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
 * 语法元素类型
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-04-22 18:45
 */
public enum WhisperGreType {

  /** 结束规则定义 */
  WHISPER_GRETYPE_END,

  // start of alternate definition for rule
  /** 开始规则定义 */
  WHISPER_GRETYPE_ALT,

  /** 非终端元素：规则引用 */
  WHISPER_GRETYPE_RULE_REF,

  /** 终端元素：字符（代码点） */
  WHISPER_GRETYPE_CHAR,

  /** 反向字符（[^a]，[ ^a-b] [^abc]） */
  WHISPER_GRETYPE_CHAR_NOT,

  /** 修改前面的 WHISPER_GRETYPE_CHAR 或 LLAMA_GRETYPE_CHAR_ALT 以成为一个包含范围（[a-z]） */
  WHISPER_GRETYPE_CHAR_RNG_UPPER,

  /** 修改前面的 WHISPER_GRETYPE_CHAR 或 WHISPER_GRETYPE_CHAR_RNG_UPPER 以添加一个备用字符进行匹配（[ab]，[a-zA]） */
  WHISPER_GRETYPE_CHAR_ALT;

  public static WhisperGreType fromOriginal(int original) {
    return switch (original) {
      case 0 -> WHISPER_GRETYPE_END;
      case 1 -> WHISPER_GRETYPE_ALT;
      case 2 -> WHISPER_GRETYPE_RULE_REF;
      case 3 -> WHISPER_GRETYPE_CHAR;
      case 4 -> WHISPER_GRETYPE_CHAR_NOT;
      case 5 -> WHISPER_GRETYPE_CHAR_RNG_UPPER;
      case 6 -> WHISPER_GRETYPE_CHAR_ALT;
      default -> throw new IllegalArgumentException("Unknown WhisperGreType: " + original);
    };
  }
}
