package com.silong.foundation.duuid.server.security.authencation;

import com.silong.foundation.duuid.server.configure.properties.DuuidServerProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 请求鉴权转换器，提取请求头内鉴权信息供后续模块使用
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-16 11:16
 */
public class SimpleServerAuthenticationConverter implements ServerAuthenticationConverter {

  /** 服务配置 */
  private final DuuidServerProperties properties;

  /**
   * 构造方法
   *
   * @param properties 服务配置
   */
  public SimpleServerAuthenticationConverter(DuuidServerProperties properties) {
    this.properties = properties;
  }

  @Override
  public Mono<Authentication> convert(ServerWebExchange exchange) {
    return Mono.just(exchange.getRequest().getHeaders())
        .map(
            httpHeaders ->
                new SimpleAuthenticationToken(
                    httpHeaders.getFirst(properties.getHttpHeaderSignature()),
                    httpHeaders.getFirst(properties.getHttpHeaderIdentifier()),
                    httpHeaders.getFirst(properties.getHttpHeaderTimestamp()),
                    httpHeaders.getFirst(properties.getHttpHeaderRandom()),
                    new SimpleGrantedAuthority(
                        httpHeaders.getFirst(properties.getHttpHeaderIdentifier()))));
  }
}
