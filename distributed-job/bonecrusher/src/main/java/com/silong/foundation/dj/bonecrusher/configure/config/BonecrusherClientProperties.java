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

import io.netty.handler.logging.LogLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Duration;
import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.convert.DurationUnit;
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

  /** netty相关调优配置 */
  @Data
  public static class NettyTuningProperties {
    /** SO_REUSEADDR，默认：true */
    private boolean SO_REUSEADDR = true;

    /** CONNECT_TIMEOUT_MILLIS，默认：5秒 */
    @NotNull
    @DurationUnit(SECONDS)
    private Duration CONNECT_TIMEOUT_MILLIS = Duration.of(5, SECONDS);
  }

  /** Netty client connector，默认：可用cpu核数 */
  @Positive private int connectorGroupThreads = Runtime.getRuntime().availableProcessors();

  /** 日志级别，默认：INFO */
  @NotNull private LogLevel LogLevel = INFO;

  /** netty调优配置 */
  @Valid @NestedConfigurationProperty
  private NettyTuningProperties netty = new NettyTuningProperties();
}
