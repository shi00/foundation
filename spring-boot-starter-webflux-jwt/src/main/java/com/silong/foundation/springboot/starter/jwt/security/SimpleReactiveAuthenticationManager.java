/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */
package com.silong.foundation.springboot.starter.jwt.security;

import static com.silong.foundation.springboot.starter.jwt.handler.AuthTokenHandler.generateTokenKey;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.springframework.util.StringUtils.hasLength;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.silong.foundation.springboot.starter.jwt.configure.config.JWTAuthProperties;
import com.silong.foundation.springboot.starter.jwt.exception.AccessTokenExpiredException;
import com.silong.foundation.springboot.starter.jwt.exception.IdentityNotFoundException;
import com.silong.foundation.springboot.starter.jwt.exception.IllegalAccessTokenException;
import com.silong.foundation.springboot.starter.jwt.exception.IllegalUserException;
import com.silong.foundation.springboot.starter.jwt.provider.JWTProvider;
import com.silong.foundation.springboot.starter.jwt.provider.UserDetailsProvider;
import java.time.Instant;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
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

  private final ConcurrentMap<String, String> tokenCache;

  private final JWTAuthProperties authProperties;

  private final JWTProvider jwtProvider;

  private final UserDetailsProvider userDetailsProvider;

  private final String appName;

  /**
   * 构造方法
   *
   * @param appName 服务名
   * @param tokenCache token内存缓存
   * @param jwtProvider jwt provider
   * @param userDetailsProvider user details provider
   * @param authProperties 配置
   */
  public SimpleReactiveAuthenticationManager(
      String appName,
      ConcurrentMap<String, String> tokenCache,
      JWTProvider jwtProvider,
      UserDetailsProvider userDetailsProvider,
      JWTAuthProperties authProperties) {
    this.appName = appName;
    this.tokenCache = tokenCache;
    this.jwtProvider = jwtProvider;
    this.userDetailsProvider = userDetailsProvider;
    this.authProperties = authProperties;
  }

  private String maskString(String strText, int start, int end) {
    int maskLength = end - start;
    if (maskLength == 0) {
      return strText;
    }
    return strText.substring(0, start)
        + "*".repeat(maskLength)
        + strText.substring(start + maskLength);
  }

  @Override
  public Mono<Authentication> authenticate(Authentication authentication) {
    // 已经鉴权成功无需再鉴权，例如：guest用户
    if (authentication.isAuthenticated()) {
      return Mono.just(authentication);
    }

    SimpleTokenAuthentication simpleTokenAuthentication =
        (SimpleTokenAuthentication) authentication;

    String token = simpleTokenAuthentication.getToken();
    DecodedJWT decodedJWT;
    try {
      decodedJWT = jwtProvider.verify(token);
    } catch (Exception e) {
      log.error("Failed to verify token.", e);
      throw new IllegalAccessTokenException(e.getMessage());
    }

    // 读取用户名
    String userName = decodedJWT.getAudience().get(0);
    if (!hasLength(userName)) {
      log.error("userName could not be found in the access token.");
      throw new IdentityNotFoundException("Illegal Access Token.");
    }

    // 查找用户
    UserDetails userDetails = userDetailsProvider.findByUserName(userName);
    if (userDetails == null) {
      log.error("{} could not be found.", userName);
      throw new IllegalUserException("Illegal Access Token.");
    }

    // 查询缓存，确认token是否由服务发放
    String tokenRecord = tokenCache.get(generateTokenKey(userName, appName, token));
    if (tokenRecord == null || tokenRecord.isEmpty()) {
      log.error("The record of token could not be found. token: {}", maskString(token, 5, 10));
      throw new IllegalAccessTokenException("Illegal Access Token.");
    }

    // token发布时间
    Instant issuedAtAsInstant = decodedJWT.getIssuedAtAsInstant();
    Instant expireTime =
        issuedAtAsInstant.plus(authProperties.getTokenTimeout(), SECONDS); // 根据token发放时间计算超时时间
    if (expireTime.compareTo(Instant.now()) < 0) {
      log.error(
          "The token is overdue. token: {}, issuedAt: {}, expiredAt: {}",
          maskString(token, 5, 10),
          issuedAtAsInstant.toEpochMilli(),
          expireTime.toEpochMilli());
      throw new AccessTokenExpiredException("Access token has expired.");
    }

    authentication.setAuthenticated(true);
    return Mono.just(authentication);
  }
}
