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
import lombok.ToString;
import lombok.experimental.Accessors;
import reactor.netty.transport.ProxyProvider;

import static reactor.netty.transport.ProxyProvider.Proxy.HTTP;

/**
 * webclient代理配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-02-01 14:40
 */
@Data
@NoArgsConstructor
@Accessors(fluent = true)
public class WebClientProxyConfig {
  /** 代理地址 */
  private String host;

  /** 代理端口 */
  private int port;

  /** 代理用户 */
  private String userName;

  /** 代理用户密码 */
  @ToString.Exclude private String password;

  /** 不使用代理的主机模式 */
  private String nonProxyHostsPattern;

  /** 代理类型，默认：http */
  private ProxyProvider.Proxy type = HTTP;

  /**
   * 构造方法
   *
   * @param proxyConfig 代理配置
   */
  public WebClientProxyConfig(@NonNull WebClientProxyConfig proxyConfig) {
    this.host = proxyConfig.host;
    this.port = proxyConfig.port;
    this.userName = proxyConfig.userName;
    this.password = proxyConfig.password;
    this.nonProxyHostsPattern = proxyConfig.nonProxyHostsPattern;
    this.type = proxyConfig.type;
  }
}
