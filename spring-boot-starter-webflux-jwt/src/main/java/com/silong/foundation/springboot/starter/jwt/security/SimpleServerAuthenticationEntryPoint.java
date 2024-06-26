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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.silong.foundation.springboot.starter.jwt.common.ErrorCode;
import com.silong.foundation.springboot.starter.jwt.common.ErrorDetail;
import com.silong.foundation.springboot.starter.jwt.exception.AccessForbiddenException;
import com.silong.foundation.springboot.starter.jwt.exception.AccessTokenNotFoundException;
import com.silong.foundation.springboot.starter.jwt.exception.IdentityNotFoundException;
import com.silong.foundation.springboot.starter.jwt.exception.IllegalUserException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 鉴权异常处理器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-17 14:16
 */
@Slf4j
@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP2"},
    justification = "Read-only initial configuration")
public class SimpleServerAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  private final String appName;

  /**
   * 构造方法
   *
   * @param appName 服务名
   * @param objectMapper jackson mapper
   */
  public SimpleServerAuthenticationEntryPoint(String appName, ObjectMapper objectMapper) {
    this.appName = appName;
    this.objectMapper = objectMapper;
  }

  @Override
  public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
    return Mono.defer(() -> Mono.just(exchange.getResponse()))
        .flatMap(response -> writeErrorDetail(exchange, ex, response));
  }

  @SneakyThrows(JsonProcessingException.class)
  private Mono<Void> writeErrorDetail(
      ServerWebExchange exchange, AuthenticationException ex, ServerHttpResponse response) {
    ServerHttpRequest request = exchange.getRequest();
    log.error("{} {} Authentication failed.", request.getMethod(), request.getPath(), ex);

    ErrorDetail errorDetail;
    if (ex instanceof AccessForbiddenException) {
      response.setStatusCode(FORBIDDEN);
      errorDetail = new ErrorDetail(ErrorCode.FORBIDDEN.format(appName), ex.getMessage());
    } else if (ex instanceof AccessTokenNotFoundException) {
      response.setStatusCode(BAD_REQUEST);
      errorDetail = new ErrorDetail(ErrorCode.TOKEN_NOT_FOUND.format(appName), ex.getMessage());
    } else if (ex instanceof IdentityNotFoundException) {
      response.setStatusCode(BAD_REQUEST);
      errorDetail = new ErrorDetail(ErrorCode.IDENTITY_NOT_FOUND.format(appName), ex.getMessage());
    } else if (ex instanceof IllegalUserException) {
      response.setStatusCode(BAD_REQUEST);
      errorDetail = new ErrorDetail(ErrorCode.ILLEGAL_USER.format(appName), ex.getMessage());
    } else {
      response.setStatusCode(UNAUTHORIZED);
      errorDetail = new ErrorDetail(ErrorCode.UNAUTHENTICATED.format(appName), ex.getMessage());
    }
    response.getHeaders().setContentType(APPLICATION_JSON);
    DataBuffer buffer =
        response.bufferFactory().wrap(objectMapper.writeValueAsString(errorDetail).getBytes(UTF_8));
    return response.writeWith(Mono.just(buffer));
  }
}
