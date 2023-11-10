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

import static com.silong.foundation.dj.bonecrusher.enu.EventExecutorType.UNORDERED;

import com.auth0.jwt.algorithms.Algorithm;
import com.silong.foundation.crypto.RootKey;
import com.silong.foundation.crypto.aes.AesGcmToolkit;
import com.silong.foundation.dj.bonecrusher.configure.config.BonecrusherClientProperties;
import com.silong.foundation.dj.bonecrusher.configure.config.BonecrusherProperties;
import com.silong.foundation.dj.bonecrusher.configure.config.BonecrusherServerProperties;
import com.silong.foundation.dj.bonecrusher.handler.*;
import com.silong.foundation.dj.bonecrusher.message.Messages;
import com.silong.foundation.dj.hook.auth.JwtAuthenticator;
import com.silong.foundation.dj.hook.auth.SimpleJwtAuthenticator;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.*;
import java.util.concurrent.ThreadFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
@EnableConfigurationProperties({
  BonecrusherProperties.class,
  BonecrusherServerProperties.class,
  BonecrusherClientProperties.class
})
public class BonecrusherAutoConfiguration {

  static {
    RootKey.initialize();
  }

  /** 服务器配置 */
  private BonecrusherProperties properties;

  /** 客户端配置 */
  private BonecrusherClientProperties clientProperties;

  /** 服务端配置 */
  private BonecrusherServerProperties serverProperties;

  @Bean
  public ProtobufEncoder protobufEncoder() {
    return new ProtobufEncoder();
  }

  @Bean
  public ProtobufDecoder requestProtobufDecoder() {
    return new ProtobufDecoder(Messages.Request.getDefaultInstance());
  }

  @Bean
  public BonecrusherResponseDecoder bonecrusherResponseDecoder() {
    return new BonecrusherResponseDecoder();
  }

  @Bean
  public ProtobufVarint32LengthFieldPrepender protobufVarint32LengthFieldPrepender() {
    return new ProtobufVarint32LengthFieldPrepender();
  }

  @Bean
  public BonecrusherResponseEncoder bonecrusherResponseEncoder() {
    return new BonecrusherResponseEncoder();
  }

  /**
   * jwt 鉴权处理器
   *
   * @return 鉴权处理器
   */
  @Bean
  @ConditionalOnMissingBean
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
  EventExecutor eventExecutor() {
    ThreadFactory factory =
        new DefaultThreadFactory(properties.getEventExecutor().getEventExecutorPoolName(), true);
    return properties.getEventExecutor().getEventExecutorType() == UNORDERED
        ? new UnorderedThreadPoolEventExecutor(
            properties.getEventExecutor().getEventExecutorThreads(), factory)
        : new DefaultEventExecutor(factory);
  }

  @Bean
  public ServerChannelHandler serverChannelHandler(JwtAuthenticator jwtAuthenticator) {
    return new ServerChannelHandler(serverProperties, jwtAuthenticator);
  }

  @Bean
  ClientChannelHandler clientChannelHandler(
      JwtAuthenticator jwtAuthenticator, EventExecutor executor) {
    return new ClientChannelHandler(clientProperties, executor, jwtAuthenticator);
  }

  @Bean
  public ResourcesTransferHandler resourcesTransferHandler() {
    return new ResourcesTransferHandler(serverProperties);
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
  public void setProperties(BonecrusherProperties properties) {
    this.properties = properties;
  }

  @Autowired
  public void setClientProperties(BonecrusherClientProperties clientProperties) {
    this.clientProperties = clientProperties;
  }

  @Autowired
  public void setServerProperties(BonecrusherServerProperties serverProperties) {
    this.serverProperties = serverProperties;
  }
}
