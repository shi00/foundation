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

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Positive;
import java.time.Duration;

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
  /** 打印请求响应配置category */
  public static final String NETTY_CLIENT_CATEGORY = "reactor.netty.http.client.HttpClient";

  /** The number of bytes in 1MB. */
  public static final int DEFAULT_ONE_MB = 1024 * 1024;

  /** 基础url */
  @NotEmpty private String baseUrl;

  /** 连接超时时长，单位：毫秒，默认：10000毫秒 */
  @Positive private long connectTimeout = Duration.ofSeconds(10).toMillis();

  /** 读超时时长，单位：毫秒，默认：6000毫秒 */
  @Positive private long readTimeout = Duration.ofSeconds(6).toMillis();

  /** 写超时时长，单位：毫秒，默认：3000毫秒 */
  @Positive private long writeTimeout = Duration.ofSeconds(3).toMillis();

  /** 响应超时时长，单位：毫秒，默认：3000毫秒 */
  @Positive private long responseTimeout = Duration.ofSeconds(3).toMillis();

  /** codec内存缓存上限 */
  @Positive private int codecMaxBufferSize = DEFAULT_ONE_MB;

  /** 是否开启GZip压缩，默认：false */
  private boolean compressionEnabled = false;

  /** 是否可开启keepAlive，默认：true */
  private boolean keepAliveEnabled = true;

  /**
   * Client-side TCP FastOpen. Sending data with the initial TCP handshake.<br>
   * 默认：true
   */
  private boolean fastOpenConnectEnabled = true;

  /**
   * 构造方法
   *
   * @param config 配置信息
   */
  public WebClientConfig(WebClientConfig config) {
    this.baseUrl = config.baseUrl;
    this.codecMaxBufferSize = config.codecMaxBufferSize;
    this.connectTimeout = config.connectTimeout;
    this.readTimeout = config.readTimeout;
    this.responseTimeout = config.responseTimeout;
    this.compressionEnabled = config.compressionEnabled;
    this.keepAliveEnabled = config.keepAliveEnabled;
    this.writeTimeout = config.writeTimeout;
    this.fastOpenConnectEnabled = config.fastOpenConnectEnabled;
  }
}
