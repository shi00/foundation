package com.silong.foundation.duuid.server.security.authencation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import reactor.core.publisher.Mono;

/**
 * 鉴权成功处理器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-16 13:21
 */
@Slf4j
public class SimpleServerAuthenticationSuccessHandler
    implements ServerAuthenticationSuccessHandler {

  private static final ServerAuthenticationSuccessHandler SUCCESS_HANDLER =
      new SimpleServerAuthenticationSuccessHandler();

  /**
   * 单例方法
   *
   * @return 处理器
   */
  public static ServerAuthenticationSuccessHandler getInstance() {
    return SUCCESS_HANDLER;
  }

  private SimpleServerAuthenticationSuccessHandler() {}

  @Override
  public Mono<Void> onAuthenticationSuccess(
      WebFilterExchange webFilterExchange, Authentication authentication) {
    if (log.isDebugEnabled()) {
      ServerHttpRequest request = webFilterExchange.getExchange().getRequest();
      log.debug("{} {} Authentication succeeded.", request.getMethod(), request.getPath());
    }
    return Mono.empty();
  }
}
