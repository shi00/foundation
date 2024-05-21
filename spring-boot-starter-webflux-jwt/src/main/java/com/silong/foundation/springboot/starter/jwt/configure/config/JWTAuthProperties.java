/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.silong.foundation.springboot.starter.jwt.configure.config;

import static java.util.concurrent.TimeUnit.HOURS;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.Set;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT鉴权配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-03 11:31
 */
@Data
@Validated
@ConfigurationProperties(prefix = "jwt-auth")
@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "Read-only initial configuration")
public class JWTAuthProperties {

  /** token超时时长，默认：3600秒，如果为0表示永不超时 */
  @PositiveOrZero private int tokenTimeout = (int) HOURS.toSeconds(1);

  /** 鉴权路径 */
  @NotEmpty private String authPath;

  /** token签名密钥 */
  @NotEmpty private String signKey;

  /** 无需鉴权请求路径名单 */
  @Valid @NotEmpty private Set<@NotEmpty String> whiteList;

  /** 需鉴权请求路径名单 */
  @Valid @NotEmpty private Set<@NotEmpty String> authList;
}
