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
import okhttp3.MediaType;
import okhttp3.internal.http.HttpHeaders;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.time.Duration;
import java.util.Map;

import static com.silong.foundation.springboot.starter.okhttp3.Constants.APPLICATION_JSON_VALUE;
import static com.silong.foundation.springboot.starter.okhttp3.Constants.CONTENT_TYPE;

/**
 * web client配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-02-01 12:44
 */
@Data
@Validated
@NoArgsConstructor
@Accessors(fluent = true)
@ConfigurationProperties(prefix = "okhttp3.webclient")
public class WebClientConfig {
  /** 默认配置 */
  public static final WebClientConfig DEFAULT_CONFIG = new WebClientConfig();

  /** 是否启用Okhttp3客户端，默认：false */
  private boolean enabled;

  /** 连接失败是否重试，默认：true */
  private boolean retryOnConnectionFailure = true;

  /** 连接超时时长，单位：毫秒，默认：10000毫秒 */
  @Positive private long connectTimeoutMillis = Duration.ofSeconds(10).toMillis();

  /** 读超时时长，单位：毫秒，默认：10000毫秒 */
  @Positive private long readTimeoutMillis = Duration.ofSeconds(10).toMillis();

  /** 写超时时长，单位：毫秒，默认：10000毫秒 */
  @Positive private long writeTimeoutMillis = Duration.ofSeconds(10).toMillis();

  /** 调用超时时长，单位：毫秒，默认：15000毫秒 */
  @Positive private long callTimeoutMillis = Duration.ofSeconds(15).toMillis();

  /** ping命令发送间隔，针对http2和websocket生效，单位：毫秒，默认：60000毫秒 */
  @Positive private long pingIntervalMillis = Duration.ofMinutes(1).toMillis();

  /** 异步调用线程池最大线程数量，默认：Runtime.getRuntime().availableProcessors() * 2 */
  @Positive private int dispatcherMaxThreadCount = Runtime.getRuntime().availableProcessors() * 2;

  /** 异步执行时可并行的最大请求数，默认：64 */
  @Positive private int dispatcherMaxConcurrentRequests = 64;

  /** 异步执行时针对单个host可并行执行的最大请求数：默认：32 */
  @Positive private int dispatcherMaxConcurrentRequestsPerHost = 32;

  /** 默认请求头，默认：["Content-Type":"application/json"] */
  @NotNull
  private Map<String, String> defaultRequestHeaders = Map.of(CONTENT_TYPE, APPLICATION_JSON_VALUE);
}
