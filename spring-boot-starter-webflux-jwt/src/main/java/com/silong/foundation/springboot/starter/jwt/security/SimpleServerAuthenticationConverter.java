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
package com.silong.foundation.springboot.starter.jwt.security;

import static com.silong.foundation.springboot.starter.jwt.common.Constants.*;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;
import static org.springframework.util.StringUtils.hasLength;

import com.silong.foundation.springboot.starter.jwt.configure.config.JWTAuthProperties;
import com.silong.foundation.springboot.starter.jwt.exception.AccessForbiddenException;
import com.silong.foundation.springboot.starter.jwt.exception.AccessTokenNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 请求鉴权转换器，提取请求头内鉴权信息供后续模块使用
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-16 11:16
 */
@Slf4j
public class SimpleServerAuthenticationConverter implements ServerAuthenticationConverter {

  /** 用户访问白名单列表内的url时使用GUEST鉴权进行访问，无需鉴权 */
  private static final Authentication GUEST =
      new SimpleTokenAuthentication(createAuthorityList("guest"), true, null);

  private final ServerWebExchangeMatcher noAuthServerWebExchangeMatcher;

  private final ServerWebExchangeMatcher authServerWebExchangeMatcher;

  /**
   * 构造方法
   *
   * @param properties 服务配置
   */
  public SimpleServerAuthenticationConverter(JWTAuthProperties properties) {
    this.authServerWebExchangeMatcher =
        ServerWebExchangeMatchers.pathMatchers(properties.getAuthList().toArray(new String[0]));
    this.noAuthServerWebExchangeMatcher =
        ServerWebExchangeMatchers.pathMatchers(properties.getWhiteList().toArray(new String[0]));
  }

  @Override
  public Mono<Authentication> convert(ServerWebExchange exchange) {
    return noAuthServerWebExchangeMatcher
        .matches(exchange)
        .filter(ServerWebExchangeMatcher.MatchResult::isMatch)
        .map(matchResult -> GUEST) // 匹配白名单，则以guest身份访问
        .switchIfEmpty(
            authServerWebExchangeMatcher
                .matches(exchange)
                .filter(ServerWebExchangeMatcher.MatchResult::isMatch)
                .map(matchResult -> extract(exchange))
                .switchIfEmpty(
                    Mono.defer(
                        () ->
                            Mono.error(
                                () ->
                                    new AccessForbiddenException(
                                        FORBIDDEN.getReasonPhrase()))))); // 白名单和鉴权名单外的访问全部禁止
  }

  /**
   * 从请求中抽取鉴权信息
   *
   * @param exchange 请求
   * @return 鉴权信息
   */
  private Authentication extract(ServerWebExchange exchange) {
    HttpHeaders httpHeaders = exchange.getRequest().getHeaders();
    String at = httpHeaders.getFirst(ACCESS_TOKEN);
    if (!hasLength(at)) {
      throw new AccessTokenNotFoundException("AccessToken is not exist in request headers.");
    }
    return new SimpleTokenAuthentication(null, false, at);
  }
}
