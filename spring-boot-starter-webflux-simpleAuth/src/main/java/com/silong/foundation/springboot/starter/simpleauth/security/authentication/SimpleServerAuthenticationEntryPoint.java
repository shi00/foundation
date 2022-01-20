package com.silong.foundation.springboot.starter.simpleauth.security.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.silong.foundation.model.ErrorDetail;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static com.silong.foundation.constants.CommonErrorCode.AUTHENTICATION_FAILED;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_JSON;

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

  private final ObjectMapper objectMapper;

  /**
   * 构造方法
   *
   * @param appName 服务名
   * @param objectMapper jackson
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

  @SneakyThrows
  private Mono<Void> writeErrorDetail(
      ServerWebExchange exchange, AuthenticationException ex, ServerHttpResponse response) {
    ServerHttpRequest request = exchange.getRequest();
    log.error("{} {} Authentication failed.", request.getMethod(), request.getPath(), ex);
    response.setStatusCode(UNAUTHORIZED);
    response.getHeaders().setContentType(APPLICATION_JSON);
    ErrorDetail errorDetail = AUTHENTICATION_FAILED.format(appName);
    String result = objectMapper.writeValueAsString(errorDetail);
    DataBuffer buffer = response.bufferFactory().wrap(result.getBytes(UTF_8));
    return response.writeWith(Mono.just(buffer));
  }
}
