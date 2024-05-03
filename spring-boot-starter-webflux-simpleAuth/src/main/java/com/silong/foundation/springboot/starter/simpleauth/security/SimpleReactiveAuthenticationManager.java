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

import static com.silong.foundation.crypto.digest.HmacToolkit.hmacSha256;
import static com.silong.foundation.springboot.starter.simpleauth.constants.AuthHeaders.*;

import com.silong.foundation.springboot.starter.simpleauth.configure.config.SimpleAuthProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
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

  /** 配置信息 */
  @SuppressFBWarnings(
      value = {"EI_EXPOSE_REP2"},
      justification = "Read-only initial configuration")
  private final SimpleAuthProperties properties;

  /**
   * 构造方法
   *
   * @param properties 配置
   */
  public SimpleReactiveAuthenticationManager(SimpleAuthProperties properties) {
    this.properties = properties;
  }

  @Override
  public Mono<Authentication> authenticate(Authentication authentication) {

    // 已经鉴权成功无需再鉴权
    if (authentication.isAuthenticated()) {
      return Mono.just(authentication);
    }

    SimpleAuthenticationToken token = (SimpleAuthenticationToken) authentication;
    String identifier = token.identifier();
    String signature = token.signature();
    String random = token.random();
    String timestamp = token.timestamp();

    if (log.isDebugEnabled()) {
      log.debug(
          "identifier:{}, random:{}, signature:{}, timestamp:{}",
          identifier,
          random,
          "******",
          timestamp);
    }

    if (isAnyEmpty(identifier, signature, random, timestamp)) {
      throw new BadCredentialsException(
          String.format(
              "The request header must contain the following contents [%s, %s, %s, %s]",
              SIGNATURE, TIMESTAMP, RANDOM, IDENTITY));
    }

    long now = System.currentTimeMillis();
    long occur = Long.parseLong(timestamp);
    int acceptableTimeDiffMills = properties.getAcceptableTimeDiffMills();
    if (Math.abs(now - occur) > acceptableTimeDiffMills) {
      throw new BadCredentialsException(
          String.format(
              "The time difference between the request client and the server exceeds %dms",
              acceptableTimeDiffMills));
    }

    if (Objects.equals(
        signature, hmacSha256(identifier + timestamp + random, properties.getWorkKey()))) {
      authentication.setAuthenticated(true);
      return Mono.just(authentication);
    }

    throw new BadCredentialsException("The client request signature was tampered.");
  }

  private boolean isAnyEmpty(String identifier, String signature, String random, String timestamp) {
    return isEmpty(identifier) || isEmpty(signature) || isEmpty(random) || isEmpty(timestamp);
  }

  private boolean isEmpty(String s) {
    return s == null || s.isEmpty();
  }
}
