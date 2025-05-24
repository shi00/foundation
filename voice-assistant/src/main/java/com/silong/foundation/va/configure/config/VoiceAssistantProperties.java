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

import com.silong.foundation.utilities.whispercpp.WhisperConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

/**
 * 服务配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-03 11:31
 */
@Data
@Validated
@ConfigurationProperties(prefix = "voice-assistant")
public class VoiceAssistantProperties {

  /** 服务启动后是否默认开启语音交互，默认：true */
  private boolean autoStart = true;

  /** vad配置 */
  @NestedConfigurationProperty private SileroVadProperties vad = new SileroVadProperties();

  /** 音频采样配置 */
  @NestedConfigurationProperty
  private SoundRecorderProperties soundRecorder = new SoundRecorderProperties();

  /** whisper.cpp配置 */
  @NestedConfigurationProperty private WhisperConfig whisper = new WhisperConfig();
}
