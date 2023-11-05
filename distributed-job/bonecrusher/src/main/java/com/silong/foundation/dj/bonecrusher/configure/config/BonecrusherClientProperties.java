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

package com.silong.foundation.dj.bonecrusher.configure.config;

import static io.netty.handler.logging.LogLevel.INFO;
import static java.time.temporal.ChronoUnit.SECONDS;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Duration;
import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

/**
 * UDT 客户端配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-19 11:12
 */
@Data
@Validated
@ConfigurationProperties(prefix = "bonecrusher.client")
public class BonecrusherClientProperties {

  /** 日志级别，默认：INFO */
  @NotNull private io.netty.handler.logging.LogLevel LogLevel = INFO;

  /** Connector Group线程池线程命名前缀，默认：BC-Connector */
  @NotEmpty private String connectorGroupPoolName = "BC-Connector";

  /** 是否开启自动重联，默认：true */
  private boolean enabledAutoReconnection = true;

  /** Netty client connector，默认：2 */
  @Positive private int connectorGroupThreads = 2;

  /** 请求超时时间，默认：3秒 */
  @NotNull private Duration requestTimeout = Duration.of(3, SECONDS);

  /** 预期通常情况下的并发请求数，默认：250/s */
  @Positive private int expectedConcurrentRequests = 250;

  /** 最大并发请求数，默认：500/s */
  @Positive private int maximumConcurrentRequests = 500;

  /** 握手间隔时间，默认：30秒 */
  @NotNull private Duration handshakeInterval = Duration.ofSeconds(30);

  /** netty调优配置 */
  @Valid @NestedConfigurationProperty
  private NettyTuningProperties netty = new NettyTuningProperties();
}
