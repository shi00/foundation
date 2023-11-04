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

import static java.time.temporal.ChronoUnit.DAYS;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Duration;
import lombok.*;
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

  /** 事件处理器配置 */
  @Valid @NestedConfigurationProperty
  private EventExecutorProperties eventExecutor = new EventExecutorProperties();

  /** 鉴权配置 */
  @Valid @NestedConfigurationProperty private AuthProperties auth = new AuthProperties();
}
