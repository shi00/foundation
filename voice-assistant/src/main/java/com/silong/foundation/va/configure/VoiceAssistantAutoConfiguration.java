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

package com.silong.foundation.va.configure;

import com.silong.foundation.springboot.starter.crypto.config.CryptoProperties;
import com.silong.foundation.utilities.whispercpp.Whisper;
import com.silong.foundation.va.configure.config.*;
import com.silong.foundation.va.vad.SlieroVadDetector;
import com.silong.foundation.va.vad.VadDetector;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.config.EnableWebFlux;

/**
 * 服务端点路由配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-13 21:23
 */
@Configuration
@EnableWebFlux
@AutoConfigureBefore(WebFluxAutoConfiguration.class)
@EnableConfigurationProperties({VoiceAssistantProperties.class, CryptoProperties.class})
@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP2"},
    justification = "Read-only configuration")
public class VoiceAssistantAutoConfiguration {

  private VoiceAssistantProperties voiceAssistantProperties;

  @Bean
  RestClient.Builder builder() {
    return RestClient.builder();
  }

  @Bean
  Whisper registerWhisper() {
    return Whisper.getInstance(voiceAssistantProperties.getWhisper());
  }

  @Bean(destroyMethod = "close")
  VadDetector registerVadDetector(VoiceAssistantProperties properties) {
    return new SlieroVadDetector(properties);
  }

  @Autowired
  public void setVoiceAssistantProperties(VoiceAssistantProperties voiceAssistantProperties) {
    this.voiceAssistantProperties = voiceAssistantProperties;
  }
}
