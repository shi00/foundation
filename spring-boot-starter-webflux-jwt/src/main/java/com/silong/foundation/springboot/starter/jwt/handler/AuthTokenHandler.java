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

package com.silong.foundation.springboot.starter.jwt.handler;

import static com.silong.foundation.springboot.starter.jwt.common.ErrorCode.UNAUTHENTICATED;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import com.silong.foundation.springboot.starter.jwt.common.Credentials;
import com.silong.foundation.springboot.starter.jwt.common.ErrorDetail;
import com.silong.foundation.springboot.starter.jwt.common.TokenBody;
import com.silong.foundation.springboot.starter.jwt.exception.IllegalUserException;
import com.silong.foundation.springboot.starter.jwt.provider.JWTProvider;
import com.silong.foundation.springboot.starter.jwt.provider.UserDetailsProvider;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * 鉴权处理器，生成token
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-05-07 22:02
 */
@Slf4j
public class AuthTokenHandler implements HandlerFunction<ServerResponse> {

  private final Map<String, String> tokenCache;

  private final String appName;

  private final JWTProvider jwtProvider;

  private final UserDetailsProvider userDetailsProvider;

  private final PasswordEncoder passwordEncoder;

  /**
   * 构造方法
   *
   * @param appName 应用名
   * @param userDetailsProvider 用户详情供应者
   * @param passwordEncoder 密码校验
   * @param jwtProvider jwt供应者
   * @param tokenCache token缓存
   */
  public AuthTokenHandler(
      @NonNull String appName,
      @NonNull UserDetailsProvider userDetailsProvider,
      @NonNull PasswordEncoder passwordEncoder,
      @NonNull JWTProvider jwtProvider,
      @NonNull Map<String, String> tokenCache) {
    this.appName = appName;
    this.userDetailsProvider = userDetailsProvider;
    this.passwordEncoder = passwordEncoder;
    this.tokenCache = tokenCache;
    this.jwtProvider = jwtProvider;
  }

  /**
   * 根据用户凭证生成token
   *
   * @param credentials 用户凭证
   * @return token
   */
  private Mono<ServerResponse> generateToken(Credentials credentials) {
    String userName = credentials.getUserName();
    String token = jwtProvider.generate(userName, Map.of());
    tokenCache.put(generateTokenKey(userName, appName, token), "ok"); // 缓存起来
    return ServerResponse.ok()
        .contentType(APPLICATION_JSON)
        .body(new TokenBody(token).toJson(), String.class);
  }

  /**
   * 生成应用token key，用于缓存
   *
   * @param userName 用户名
   * @param appName 应用名
   * @param token token字符串
   * @return token key
   */
  public static String generateTokenKey(String userName, String appName, String token) {
    return String.format("%s-%s-token-%s", userName, appName, token);
  }

  /**
   * 用户凭证鉴权
   *
   * @param credentials 用户凭证
   */
  private void authenticate(Credentials credentials) {
    String userName = credentials.getUserName();
    UserDetails userDetails = userDetailsProvider.findByUserName(userName);
    if (userDetails == null) {
      log.warn("userDetails can't be found by userName: {}", userName);
      throw new IllegalUserException("Illegal user: " + userName);
    }

    if (!userDetails.isEnabled()) {
      log.warn("user account is not available. {}", userDetails);
      throw new IllegalUserException("Illegal user: " + userName);
    }

    if (!userDetails.isAccountNonExpired()) {
      log.warn("user's account has expired. {}", userDetails);
      throw new AccountExpiredException("user's account has expired. userName: " + userName);
    }

    // 密码过期校验
    if (!userDetails.isCredentialsNonExpired()) {
      log.warn("user's password has expired. {}", userDetails);
      throw new CredentialsExpiredException("user's password has expired. userName: " + userName);
    }

    if (!userDetails.isAccountNonLocked()) {
      log.warn("user's account has locked. {}", userDetails);
      throw new LockedException("user's account has locked. userName: " + userName);
    }

    // 密码校验
    if (!passwordEncoder.matches(credentials.getPassword(), userDetails.getPassword())) {
      log.warn("user has an incorrect password. userName: {}", userName);
      throw new BadCredentialsException("Incorrect password.");
    }
  }

  @Override
  @NonNull
  public Mono<ServerResponse> handle(@NonNull ServerRequest request) {
    return request
        .bodyToMono(Credentials.class)
        .doOnNext(this::authenticate)
        .doOnSuccess(
            credentials -> log.info("{} authentication is successful.", credentials.getUserName()))
        .flatMap(this::generateToken)
        .onErrorResume(
            t ->
                ServerResponse.status(HttpStatus.UNAUTHORIZED.value())
                    .contentType(APPLICATION_JSON)
                    .body(
                        BodyInserters.fromValue(
                            ErrorDetail.builder()
                                .errorCode(UNAUTHENTICATED.format(appName))
                                .errorMessage(t.getMessage())
                                .build())));
  }
}
