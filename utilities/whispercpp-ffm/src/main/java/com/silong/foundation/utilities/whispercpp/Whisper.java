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

import java.io.File;

/**
 * Whisper接口
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-04-22 18:45
 */
public interface Whisper {

  /**
   * 根据配置获取Whisper实例
   *
   * @param config 配置
   * @return 实例
   */
  static Whisper getInstance(WhisperConfig config) {
    return new WhisperCpp(config);
  }

  /**
   * wav语音文件识别
   *
   * @param wavFile 文件
   * @return 识别文本
   */
  String speech2Text(File wavFile);
}
