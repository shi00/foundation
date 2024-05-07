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

import static java.time.temporal.ChronoUnit.SECONDS;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.silong.foundation.springboot.starter.tokenauth.configure.config.SimpleAuthProperties;
import com.silong.foundation.springboot.starter.tokenauth.exception.AccessTokenExpiredException;
import com.silong.foundation.springboot.starter.tokenauth.exception.IllegalAccessTokenException;
import java.time.Instant;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

/**
 * 鉴权管理器，对请求头签名进行鉴权
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-16 11:46
 */
@Slf4j
public class SimpleReactiveAuthenticationManager implements ReactiveAuthenticationManager {

  private final Map<String, String> tokenCache;

  private final SimpleAuthProperties authProperties;

  private final String appName;

  /**
   * 构造方法
   *
   * @param tokenCache token内存缓存
   * @param authProperties 配置
   * @param appName 应用名
   */
  public SimpleReactiveAuthenticationManager(
      Map<String, String> tokenCache, SimpleAuthProperties authProperties, String appName) {
    this.tokenCache = tokenCache;
    this.authProperties = authProperties;
    this.appName = appName;
  }

  @Override
  public Mono<Authentication> authenticate(Authentication authentication) {

    // 已经鉴权成功无需再鉴权
    if (authentication.isAuthenticated()) {
      return Mono.just(authentication);
    }

    SimpleAuthenticationToken token = (SimpleAuthenticationToken) authentication;
    DecodedJWT decodedJWT = token.decodedJWT();
    Instant issuedAtAsInstant = decodedJWT.getIssuedAtAsInstant();
    Instant expireTime =
        issuedAtAsInstant.minus(
            authProperties.getTokenTimeout() * (-1), SECONDS); // 根据token发放时间计算超时时间
    if (expireTime.compareTo(Instant.now()) < 0) {
      throw new AccessTokenExpiredException("Access token has expired.");
    }

    String tokenKey = String.format("%s-token-%s", appName, decodedJWT.getToken());

    String v = tokenCache.get(tokenKey);
    if (v == null) {
      throw new IllegalAccessTokenException("Invalid access token.");
    }
    authentication.setAuthenticated(true);
    return Mono.just(authentication);
  }
}
