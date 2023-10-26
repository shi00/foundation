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
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.apache.commons.lang3.SystemUtils.getHostName;

import io.netty.handler.logging.LogLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.nio.file.Path;
import java.time.Duration;
import lombok.*;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

/**
 * UDT Server配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-19 11:12
 */
@Data
@Validated
@ConfigurationProperties(prefix = "bonecrusher")
public class BonecrusherProperties {

  /** netty相关调优配置 */
  @Data
  public static class NettyTuningProperties {

    // SO_BACKLOG，默认：128
    @Positive private int SO_BACKLOG = 128;

    /** SO_REUSEADDR，默认：true */
    private boolean SO_REUSEADDR = true;

    /** CONNECT_TIMEOUT_MILLIS，默认：5秒 */
    @NotNull
    @DurationUnit(SECONDS)
    private Duration CONNECT_TIMEOUT_MILLIS = Duration.of(5, SECONDS);
  }

  /**
   * 鉴权配置
   *
   * @author louis sin
   * @version 1.0.0
   * @since 2023-10-23 22:30
   */
  @Data
  public static class AuthProperties {
    /** hmac sha256密钥长度必须为32字节 */
    @NotEmpty @ToString.Exclude private String signKey;

    /** 工作密钥 */
    @NotEmpty @ToString.Exclude private String workKey;

    /** token有效期，默认：1天 */
    @NotNull
    @DurationUnit(DAYS)
    private Duration expires = Duration.of(1, DAYS);
  }

  /**
   * UDT Server监听地址，目前支持ipv4<br>
   * 默认：0.0.0.0
   */
  @NotEmpty private String address = "0.0.0.0";

  // 服务监听端口，默认：6118
  @Max(65535)
  @Min(1025)
  private int port = 6118;

  /** 数据存储目录，默认：user.dir */
  @NotNull private Path dataStorePath = SystemUtils.getUserDir().toPath();

  /** 节点名，默认：主机名 */
  @NotEmpty private String nodeName = getHostName();

  /** Netty bossGroup，默认：1 */
  @Positive private int bossGroupThreads = 1;

  // Netty workerGroup，默认：可用cpu核数
  @Positive private int workerGroupThreads = Runtime.getRuntime().availableProcessors();

  // 日志级别，默认：INFO
  @NotNull private LogLevel LogLevel = INFO;

  /** 鉴权配置 */
  @Valid @NestedConfigurationProperty private AuthProperties auth = new AuthProperties();

  /** netty调优配置 */
  @Valid @NestedConfigurationProperty
  private NettyTuningProperties netty = new NettyTuningProperties();
}
