package com.silong.foundation.springboot.starter.simpleauth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.silong.foundation.model.ErrorDetail;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static com.silong.foundation.constants.CommonErrorCode.INSUFFICIENT_PERMISSIONS;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.MediaType.APPLICATION_JSON;

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

  private final ObjectMapper objectMapper;

  /**
   * 构造方法
   *
   * @param appName 服务名
   * @param objectMapper jackson
   */
  public SimpleServerAccessDeniedHandler(String appName, ObjectMapper objectMapper) {
    this.appName = appName;
    this.objectMapper = objectMapper;
  }

  @Override
  @SneakyThrows
  public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
    ServerHttpRequest request = exchange.getRequest();
    log.error("Not authorized to {} {}", request.getMethod(), request.getPath().value(), denied);
    ServerHttpResponse response = exchange.getResponse();
    response.setStatusCode(FORBIDDEN);
    response.getHeaders().setContentType(APPLICATION_JSON);
    ErrorDetail errorDetail = INSUFFICIENT_PERMISSIONS.format(appName);
    String result = objectMapper.writeValueAsString(errorDetail);
    DataBuffer buffer = response.bufferFactory().wrap(result.getBytes(UTF_8));
    return response.writeWith(Mono.just(buffer));
  }
}