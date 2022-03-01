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
package com.silong.foundation.springboot.starter.okhttp3.configure.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Positive;
import java.time.Duration;

/**
 * 连接池配置
 *
 * @author s00011749
 * @version 1.0.0
 * @since 2022/2/7 14:23
 */
@Data
@Validated
@NoArgsConstructor
@Accessors(fluent = true)
@ConfigurationProperties(prefix = "okhttp3.webclient.connection-pool")
public class WebClientConnectionPoolConfig {

  /** 默认配置 */
  public static final WebClientConnectionPoolConfig DEFAULT_CONFIG =
      new WebClientConnectionPoolConfig();

  /** 最大空闲连接数，默认：10 */
  @Positive private int maxIdleConnections = 10;

  /** 空闲连接最大存货时间，单位：毫秒，默认：300000 */
  @Positive private long keepAliveMillis = Duration.ofMinutes(5).toMillis();
}
