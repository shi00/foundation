package com.silong.foundation.duuid.server.security.authencation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import reactor.core.publisher.Mono;

/**
 * 鉴权失败处理器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-16 13:26
 */
@Slf4j
public class SimpleServerAuthenticationFailureHandler
    implements ServerAuthenticationFailureHandler {

  private static final ServerAuthenticationFailureHandler FAILURE_HANDLER =
      new SimpleServerAuthenticationFailureHandler();

  /**
   * 单例方法
   *
   * @return 实例
   */
  public static ServerAuthenticationFailureHandler getInstance() {
    return FAILURE_HANDLER;
  }

  /** 构造方法 */
  private SimpleServerAuthenticationFailureHandler() {}

  @Override
  public Mono<Void> onAuthenticationFailure(
      WebFilterExchange webFilterExchange, AuthenticationException exception) {
    ServerHttpRequest request = webFilterExchange.getExchange().getRequest();
    log.error("{} {} Authentication failed.", request.getMethod(), request.getPath(), exception);
    return Mono.empty();
  }
}
