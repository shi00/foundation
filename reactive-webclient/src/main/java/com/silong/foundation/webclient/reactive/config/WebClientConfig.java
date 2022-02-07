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
import lombok.NonNull;
import lombok.experimental.Accessors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * web client配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-02-01 12:44
 */
@Data
@NoArgsConstructor
@Accessors(fluent = true)
public class WebClientConfig {

  /** 默认配置 */
  public static final WebClientConfig DEFAULT_CONFIG = new WebClientConfig();

  /** 打印请求响应配置category */
  public static final String NETTY_CLIENT_CATEGORY = "reactor.netty.http.client.HttpClient";

  /** The number of bytes in 1MB. */
  public static final int DEFAULT_ONE_MB = 1024 * 1024;

  /** 连接超时时长，单位：毫秒，默认：10000毫秒 */
  @Positive private long connectTimeoutMillis = Duration.ofSeconds(10).toMillis();

  /** 读超时时长，单位：毫秒，默认：10000毫秒 */
  @Positive private long readTimeoutMillis = Duration.ofSeconds(10).toMillis();

  /** 写超时时长，单位：毫秒，默认：10000毫秒 */
  @Positive private long writeTimeoutMillis = Duration.ofSeconds(10).toMillis();

  /** 响应超时时长，单位：毫秒，默认：15000毫秒 */
  @Positive private long responseTimeoutMillis = Duration.ofSeconds(15).toMillis();

  /** codec内存缓存上限 */
  @Positive private int codecMaxBufferSize = DEFAULT_ONE_MB;

  /** 默认请求头，默认：["Content-Type":"application/json"] */
  @NotNull
  private Map<String, String> defaultRequestHeaders =
      Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

  /** 请求过滤器列表，默认：empty */
  @NotNull private List<ExchangeFilterFunction> exchangeFilterFunctions = List.of();

  /**
   * 客户端访问时的基础url，例如：https://localhost:8881/test<br>
   * 配置基础url后，后续使用Webclient时只需指定相对路径即可。
   */
  private String baseUrl;

  /** 是否开启GZip压缩，默认：true */
  private boolean compressionEnabled = true;

  /** 是否可开启keepAlive，默认：true */
  private boolean keepAliveEnabled = true;

  /**
   * Client-side TCP FastOpen. Sending data with the initial TCP handshake.<br>
   * 默认：true
   */
  private boolean tcpFastOpenConnectEnabled = true;

  /** 是否开启TCP_NODELAY，默认：true */
  private boolean tcpNoDelayEnabled = true;

  /** 是否关闭失败重试一次，默认：false */
  private boolean disableRetryOnce = false;

  /**
   * 构造方法
   *
   * @param config 配置信息
   */
  public WebClientConfig(@NonNull WebClientConfig config) {
    this.disableRetryOnce = config.disableRetryOnce;
    this.codecMaxBufferSize = config.codecMaxBufferSize;
    this.connectTimeoutMillis = config.connectTimeoutMillis;
    this.readTimeoutMillis = config.readTimeoutMillis;
    this.responseTimeoutMillis = config.responseTimeoutMillis;
    this.compressionEnabled = config.compressionEnabled;
    this.keepAliveEnabled = config.keepAliveEnabled;
    this.writeTimeoutMillis = config.writeTimeoutMillis;
    this.tcpFastOpenConnectEnabled = config.tcpFastOpenConnectEnabled;
    this.baseUrl = config.baseUrl;
    this.tcpNoDelayEnabled = config.tcpNoDelayEnabled;
    this.defaultRequestHeaders = config.defaultRequestHeaders;
    this.exchangeFilterFunctions = config.exchangeFilterFunctions;
  }
}
