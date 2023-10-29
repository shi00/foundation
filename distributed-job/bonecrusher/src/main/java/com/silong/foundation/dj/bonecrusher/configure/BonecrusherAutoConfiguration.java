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
import com.silong.foundation.dj.bonecrusher.configure.config.BonecrusherClientProperties;
import com.silong.foundation.dj.bonecrusher.configure.config.BonecrusherProperties;
import com.silong.foundation.dj.bonecrusher.handler.*;
import com.silong.foundation.dj.bonecrusher.message.Messages;
import com.silong.foundation.utilities.jwt.JwtAuthenticator;
import com.silong.foundation.utilities.jwt.SimpleJwtAuthenticator;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
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
@EnableConfigurationProperties({BonecrusherProperties.class, BonecrusherClientProperties.class})
public class BonecrusherAutoConfiguration {

  static {
    RootKey.initialize();
  }

  /** 服务器配置 */
  private BonecrusherProperties serverProperties;

  /** 客户端配置 */
  private BonecrusherClientProperties clientProperties;

  @Bean
  public ProtobufDecoder protobufDecoder() {
    return new ProtobufDecoder(Messages.Request.getDefaultInstance());
  }

  @Bean
  public ProtobufVarint32LengthFieldPrepender protobufVarint32LengthFieldPrepender() {
    return new ProtobufVarint32LengthFieldPrepender();
  }

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
                    serverProperties.getAuth().getSignKey(),
                    serverProperties.getAuth().getWorkKey())))
        // 设置超期时间
        .period(serverProperties.getAuth().getExpires())
        .build();
  }

  @Bean
  public ServerAuthChannelHandler serverAuthChannelHandler(JwtAuthenticator jwtAuthenticator) {
    return new ServerAuthChannelHandler(serverProperties, jwtAuthenticator);
  }

  @Bean
  public ProtobufEncoder protobufEncoder() {
    return new ProtobufEncoder();
  }

  @Bean
  public ResourcesTransferHandler fileServerHandler() {
    return new ResourcesTransferHandler(serverProperties.getDataStorePath());
  }

  @Bean
  public LoggingHandler serverLoggingHandler() {
    return new LoggingHandler(serverProperties.getLogLevel());
  }

  @Bean
  public LoggingHandler clientLoggingHandler() {
    return new LoggingHandler(clientProperties.getLogLevel());
  }

  @Autowired
  public void setServerProperties(BonecrusherProperties serverProperties) {
    this.serverProperties = serverProperties;
  }

  @Autowired
  public void setClientProperties(BonecrusherClientProperties clientProperties) {
    this.clientProperties = clientProperties;
  }
}
