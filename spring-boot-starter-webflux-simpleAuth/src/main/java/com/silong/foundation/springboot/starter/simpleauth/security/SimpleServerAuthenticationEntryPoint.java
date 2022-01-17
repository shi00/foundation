package com.silong.foundation.springboot.starter.simpleauth.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static com.silong.foundation.constants.CommonErrorCode.AUTHENTICATION_FAILED;

/**
 * 鉴权异常处理器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-17 14:16
 */
@Slf4j
public class SimpleServerAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

  private final String appName;

  /**
   * 构造方法
   *
   * @param appName 服务名
   */
  public SimpleServerAuthenticationEntryPoint(String appName) {
    this.appName = appName;
  }

  @Override
  public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
    ServerHttpRequest request = exchange.getRequest();
    log.error("{} {} Authentication failed.", request.getMethod(), request.getPath(), ex);
    return exchange
        .getResponse()
        .writeAndFlushWith(Mono.fromRunnable(() -> AUTHENTICATION_FAILED.format(appName)));
  }
}
