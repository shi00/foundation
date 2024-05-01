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

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * grammar element type
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-05-01 11:53
 */
@Getter
@AllArgsConstructor
public enum WhisperGreType {

  // end of rule definition
  WHISPER_GRETYPE_END(0),

  // start of alternate definition for rule
  WHISPER_GRETYPE_ALT(1),

  // non-terminal element: reference to rule
  WHISPER_GRETYPE_RULE_REF(2),

  // terminal element: character (code point)
  WHISPER_GRETYPE_CHAR(3),

  // inverse char(s) ([^a], [^a-b] [^abc])
  WHISPER_GRETYPE_CHAR_NOT(4),

  // modifies a preceding WHISPER_GRETYPE_CHAR or LLAMA_GRETYPE_CHAR_ALT to
  // be an inclusive range ([a-z])
  WHISPER_GRETYPE_CHAR_RNG_UPPER(5),

  // modifies a preceding WHISPER_GRETYPE_CHAR or
  // WHISPER_GRETYPE_CHAR_RNG_UPPER to add an alternate char to match ([ab], [a-zA])
  WHISPER_GRETYPE_CHAR_ALT(6);

  private final int value;
}
