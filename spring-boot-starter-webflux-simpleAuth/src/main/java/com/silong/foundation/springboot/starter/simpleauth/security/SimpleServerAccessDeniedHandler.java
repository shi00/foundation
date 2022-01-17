package com.silong.foundation.springboot.starter.simpleauth.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static com.silong.foundation.constants.CommonErrorCode.INSUFFICIENT_PERMISSIONS;

/**
 * 请求拒绝处理器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-17 14:11
 */
@Slf4j
public class SimpleServerAccessDeniedHandler implements ServerAccessDeniedHandler {

  private final String appName;

  /**
   * 构造方法
   *
   * @param appName 服务名
   */
  public SimpleServerAccessDeniedHandler(String appName) {
    this.appName = appName;
  }

  @Override
  public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
    ServerHttpRequest request = exchange.getRequest();
    log.error("Not authorized to {} {}", request.getMethod(), request.getPath().value(), denied);
    return exchange
        .getResponse()
        .writeAndFlushWith(Mono.fromRunnable(() -> INSUFFICIENT_PERMISSIONS.format(appName)));
  }
}
