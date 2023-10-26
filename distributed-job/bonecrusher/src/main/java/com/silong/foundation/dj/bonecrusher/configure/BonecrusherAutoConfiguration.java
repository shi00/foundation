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

package com.silong.foundation.dj.bonecrusher.configure;

import com.auth0.jwt.algorithms.Algorithm;
import com.silong.foundation.crypto.RootKey;
import com.silong.foundation.crypto.aes.AesGcmToolkit;
import com.silong.foundation.dj.bonecrusher.Bonecrusher;
import com.silong.foundation.dj.bonecrusher.configure.config.BonecrusherProperties;
import com.silong.foundation.dj.bonecrusher.handler.AuthChannelHandler;
import com.silong.foundation.dj.bonecrusher.handler.FileServerHandler;
import com.silong.foundation.utilities.jwt.JwtAuthenticator;
import com.silong.foundation.utilities.jwt.SimpleJwtAuthenticator;
import io.netty.handler.logging.LoggingHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 自动配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-24 14:41
 */
@Configuration
@EnableConfigurationProperties(BonecrusherProperties.class)
public class BonecrusherAutoConfiguration {

  static {
    RootKey.initialize();
  }

  /** 配置 */
  private BonecrusherProperties properties;

  /**
   * jwt 鉴权处理器
   *
   * @return 鉴权处理器
   */
  @Bean
  public JwtAuthenticator jwtAuthenticator() {
    return SimpleJwtAuthenticator.builder()
        // 设置签名密钥
        .signatureAlgorithm(
            Algorithm.HMAC256(
                AesGcmToolkit.decrypt(
                    properties.getAuth().getSignKey(), properties.getAuth().getWorkKey())))
        // 设置超期时间
        .period(properties.getAuth().getExpires())
        .build();
  }

  @Bean
  public AuthChannelHandler authChannelHandler(JwtAuthenticator jwtAuthenticator) {
    return new AuthChannelHandler(jwtAuthenticator);
  }

  @Bean
  public FileServerHandler fileServerHandler() {
    return new FileServerHandler(properties.getDataStorePath());
  }

  @Bean
  public LoggingHandler loggingHandler() {
    return new LoggingHandler(properties.getLogLevel());
  }

  @Bean
  public Bonecrusher bonecrusher(
      LoggingHandler loggingHandler,
      AuthChannelHandler authChannelHandler,
      FileServerHandler fileServerHandler) {
    return new Bonecrusher(properties, loggingHandler, authChannelHandler, fileServerHandler);
  }

  @Autowired
  public void setProperties(BonecrusherProperties properties) {
    this.properties = properties;
  }
}
