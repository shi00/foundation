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

import static com.silong.foundation.springboot.starter.tokenauth.constants.AuthHeaders.*;
import static java.util.Collections.singletonList;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.util.StringUtils.hasLength;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.silong.foundation.crypto.aes.AesGcmToolkit;
import com.silong.foundation.springboot.starter.tokenauth.configure.config.SimpleAuthProperties;
import com.silong.foundation.springboot.starter.tokenauth.exception.AccessForbiddenException;
import com.silong.foundation.springboot.starter.tokenauth.exception.AccessTokenNotFoundException;
import com.silong.foundation.springboot.starter.tokenauth.exception.IllegalAccessTokenException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
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
@Slf4j
public class SimpleServerAuthenticationConverter implements ServerAuthenticationConverter {

  /** 百名单授权 */
  private static final Authentication GEUST =
      new SimpleAuthenticationToken(null, singletonList(new SimpleGrantedAuthority("guest")), true);

  private final JWTVerifier verifier;

  private final Map<String, List<SimpleGrantedAuthority>> cache;

  private final ServerWebExchangeMatcher noAuthServerWebExchangeMatcher;

  private final ServerWebExchangeMatcher authServerWebExchangeMatcher;

  /**
   * 构造方法
   *
   * @param properties 服务配置
   * @param appName 应用名
   */
  public SimpleServerAuthenticationConverter(
      @NonNull SimpleAuthProperties properties, @Value("spring.application.name") String appName) {
    Map<String, List<SimpleGrantedAuthority>> map = new HashMap<>();
    properties
        .getUserRolesMappings()
        .forEach(
            (key, value) ->
                map.put(
                    key,
                    value.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList())));
    this.cache = Collections.unmodifiableMap(map);
    this.authServerWebExchangeMatcher =
        ServerWebExchangeMatchers.pathMatchers(properties.getAuthList().toArray(new String[0]));
    this.noAuthServerWebExchangeMatcher =
        ServerWebExchangeMatchers.pathMatchers(properties.getWhiteList().toArray(new String[0]));
    this.verifier =
        JWT.require(
                Algorithm.HMAC256(
                    AesGcmToolkit.decrypt(properties.getSignKey(), properties.getWorkKey())))
            .acceptExpiresAt(properties.getTokenTimeout())
            // specify any specific claim validations
            .withIssuer(appName)
            // reusable verifier instance
            .build();
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
                .switchIfEmpty(
                    Mono.defer(
                        () ->
                            Mono.error(
                                () -> new AccessForbiddenException(FORBIDDEN.getReasonPhrase())))));
  }

  private Authentication getNeedAuthentication(ServerWebExchange exchange) {
    HttpHeaders httpHeaders = exchange.getRequest().getHeaders();
    String at = httpHeaders.getFirst(ACCESS_TOKEN);
    if (!hasLength(at)) {
      throw new AccessTokenNotFoundException("AccessToken is not exist in request headers.");
    }

    DecodedJWT decodedJWT;
    try {
      decodedJWT = verifier.verify(at);
    } catch (JWTVerificationException e) {
      log.error("Failed to verify token.", e);
      throw new IllegalAccessTokenException("Illegal access token.");
    }

    String identity = decodedJWT.getClaim(IDENTITY).asString();
    if (!hasLength(identity)) {
      log.error("{} is not exist in access token.", IDENTITY);
      throw new IllegalAccessTokenException("Illegal access token.");
    }

    List<SimpleGrantedAuthority> grantedAuthorities = cache.get(identity);
    if (grantedAuthorities == null || grantedAuthorities.isEmpty()) {
      log.error("Could not find any roles for identity:{}.", identity);
      throw new IllegalAccessTokenException("Illegal access token.");
    }
    return new SimpleAuthenticationToken(decodedJWT, grantedAuthorities, false);
  }
}
