package com.silong.foundation.springboot.starter.simpleauth.security;

import com.silong.foundation.springboot.starter.simpleauth.configure.properties.SimpleAuthProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.security.core.authority.AuthorityUtils.NO_AUTHORITIES;

/**
 * 请求鉴权转换器，提取请求头内鉴权信息供后续模块使用
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-16 11:16
 */
public class SimpleServerAuthenticationConverter implements ServerAuthenticationConverter {

  /** 服务配置 */
  private final SimpleAuthProperties properties;

  private final List<GrantedAuthority> authorities;

  private final ServerWebExchangeMatcher noAuthServerWebExchangeMatcher;

  private final ServerWebExchangeMatcher authServerWebExchangeMatcher;

  /**
   * 构造方法
   *
   * @param properties 服务配置
   */
  public SimpleServerAuthenticationConverter(SimpleAuthProperties properties) {
    this.properties = properties;
    this.authorities =
        properties.getRolePathsMappings().keySet().stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());
    authServerWebExchangeMatcher =
        ServerWebExchangeMatchers.pathMatchers(properties.getAuthList().toArray(new String[0]));
    noAuthServerWebExchangeMatcher =
        ServerWebExchangeMatchers.pathMatchers(properties.getWhiteList().toArray(new String[0]));
  }

  @Override
  public Mono<Authentication> convert(ServerWebExchange exchange) {
    return noAuthServerWebExchangeMatcher
        .matches(exchange)
        .filter(ServerWebExchangeMatcher.MatchResult::isMatch)
        .map(matchResult -> getNoNeedAuthentication())
        .switchIfEmpty(
            authServerWebExchangeMatcher
                .matches(exchange)
                .filter(ServerWebExchangeMatcher.MatchResult::isMatch)
                .map(matchResult -> getNeedAuthentication(exchange))
                .switchIfEmpty(Mono.just(new SimpleAuthenticationToken(NO_AUTHORITIES))));
  }

  private SimpleAuthenticationToken getNeedAuthentication(ServerWebExchange exchange) {
    HttpHeaders httpHeaders = exchange.getRequest().getHeaders();
    return new SimpleAuthenticationToken(
        httpHeaders.getFirst(properties.getHttpHeaderSignature()),
        httpHeaders.getFirst(properties.getHttpHeaderIdentifier()),
        httpHeaders.getFirst(properties.getHttpHeaderTimestamp()),
        httpHeaders.getFirst(properties.getHttpHeaderRandom()),
        authorities);
  }

  private Authentication getNoNeedAuthentication() {
    Authentication simpleAuthenticationToken = new SimpleAuthenticationToken(authorities);
    simpleAuthenticationToken.setAuthenticated(true);
    return simpleAuthenticationToken;
  }
}
