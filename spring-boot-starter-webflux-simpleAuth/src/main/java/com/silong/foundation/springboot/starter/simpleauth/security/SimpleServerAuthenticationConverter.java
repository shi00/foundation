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
package com.silong.foundation.springboot.starter.simpleauth.security;

import static com.silong.foundation.springboot.starter.simpleauth.constants.AuthHeaders.*;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import com.silong.foundation.springboot.starter.simpleauth.configure.config.SimpleAuthProperties;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
public class SimpleServerAuthenticationConverter implements ServerAuthenticationConverter {

  /** 百名单授权 */
  private static final Authentication GEUST =
      new SimpleAuthenticationToken(singletonList(new SimpleGrantedAuthority("guest")), true);

  /** 禁止访问 */
  private static final Authentication DENIED = new SimpleAuthenticationToken(emptyList());

  private final Map<String, List<SimpleGrantedAuthority>> cache = new HashMap<>();

  private final ServerWebExchangeMatcher noAuthServerWebExchangeMatcher;

  private final ServerWebExchangeMatcher authServerWebExchangeMatcher;

  /**
   * 构造方法
   *
   * @param properties 服务配置
   */
  public SimpleServerAuthenticationConverter(SimpleAuthProperties properties) {
    properties
        .getUserRolesMappings()
        .forEach(
            (key, value) ->
                cache.put(
                    key,
                    value.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList())));
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
        .map(matchResult -> GEUST) // 匹配白名单，则以guest身份访问
        .switchIfEmpty(
            authServerWebExchangeMatcher
                .matches(exchange)
                .filter(ServerWebExchangeMatcher.MatchResult::isMatch)
                .map(matchResult -> getNeedAuthentication(exchange))
                .switchIfEmpty(Mono.just(DENIED)));
  }

  private Authentication getNeedAuthentication(ServerWebExchange exchange) {
    HttpHeaders httpHeaders = exchange.getRequest().getHeaders();
    String identity = httpHeaders.getFirst(IDENTITY);
    List<SimpleGrantedAuthority> grantedAuthorities = cache.get(identity);
    if (grantedAuthorities == null || grantedAuthorities.isEmpty()) {
      throw new BadCredentialsException("Could not find any roles for the request.");
    }
    return new SimpleAuthenticationToken(
        httpHeaders.getFirst(SIGNATURE),
        identity,
        httpHeaders.getFirst(TIMESTAMP),
        httpHeaders.getFirst(RANDOM),
        grantedAuthorities);
  }
}
