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
package com.silong.foundation.webclient.reactive.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.validation.constraints.Positive;
import java.time.Duration;

import static reactor.netty.resources.ConnectionProvider.DEFAULT_POOL_MAX_CONNECTIONS;

/**
 * 连接池配置
 *
 * @author s00011749
 * @version 1.0.0
 * @since 2022/2/7 14:23
 */
@Data
@NoArgsConstructor
@Accessors(fluent = true)
public class WebClientConnectionPoolConfig {

  /** 默认配置 */
  public static final WebClientConnectionPoolConfig DEFAULT_CONFIG =
      new WebClientConnectionPoolConfig();

  /** 连接池最大连接数，默认值：16 */
  @Positive private int maxConnections = 16;

  /** 从连接池获取连接超时时间，单位：秒，默认：30 */
  @Positive private long pendingAcquireTimeout = 30;

  /** 连接最长寿命，单位：毫秒，默认：3600000 */
  @Positive private long maxLifeTimeMillis = Duration.ofHours(1).toMillis();

  /** 连接最长空闲时长，单位：毫秒，默认：1800000 */
  @Positive private long maxIdleTimeMillis = Duration.ofMinutes(30).toMillis();

  /** 定时检查连接池内连接是否应该释放时间间隔，单位：秒，默认：60 */
  @Positive private long evictionInterval = Duration.ofMinutes(1).getSeconds();

  /**
   * 排队获取连接请求上限，默认：32<br>
   * 配置-1表示无上限
   */
  private int pendingAcquireMaxCount = 32;
}
