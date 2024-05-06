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

package com.silong.foundation.springboot.starter.tokenauth.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 鉴权成功处理器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-16 13:21
 */
@Slf4j
public class SimpleServerAuthenticationSuccessHandler
    implements ServerAuthenticationSuccessHandler {

  private static final ServerAuthenticationSuccessHandler SUCCESS_HANDLER =
      new SimpleServerAuthenticationSuccessHandler();

  /**
   * 单例方法
   *
   * @return 处理器
   */
  public static ServerAuthenticationSuccessHandler getInstance() {
    return SUCCESS_HANDLER;
  }

  private SimpleServerAuthenticationSuccessHandler() {}

  @Override
  public Mono<Void> onAuthenticationSuccess(
      WebFilterExchange webFilterExchange, Authentication authentication) {
    ServerWebExchange exchange = webFilterExchange.getExchange();
    if (log.isDebugEnabled()) {
      ServerHttpRequest request = exchange.getRequest();
      log.debug("{} {} Authentication succeeded.", request.getMethod(), request.getPath());
    }
    return webFilterExchange.getChain().filter(exchange);
  }
}
