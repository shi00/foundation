package com.silong.foundation.springboot.starter.simpleauth.security;

import com.silong.foundation.springboot.starter.simpleauth.configure.properties.SimpleAuthProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.silong.foundation.springboot.starter.simpleauth.constants.AuthHeaders.*;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * 请求鉴权转换器，提取请求头内鉴权信息供后续模块使用
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-16 11:16
 */
public class SimpleServerAuthenticationConverter implements ServerAuthenticationConverter {

  private static final Authentication GEUST;

  private static final Authentication DENIED = new SimpleAuthenticationToken(emptyList());

  private final Map<String, List<SimpleGrantedAuthority>> cache = new HashMap<>();

  private final ServerWebExchangeMatcher noAuthServerWebExchangeMatcher;

  private final ServerWebExchangeMatcher authServerWebExchangeMatcher;

  static {
    GEUST = new SimpleAuthenticationToken(singletonList(new SimpleGrantedAuthority("guest")));
    GEUST.setAuthenticated(true);
  }

  /**
   * 构造方法
   *
   * @param properties 服务配置
   */
  public SimpleServerAuthenticationConverter(SimpleAuthProperties properties) {
    properties
        .getUserRolesMappings()
        .forEach(
            (key, value) ->
                cache.put(
                    key,
                    value.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList())));
    this.authServerWebExchangeMatcher =
        ServerWebExchangeMatchers.pathMatchers(properties.getAuthList().toArray(new String[0]));
    this.noAuthServerWebExchangeMatcher =
        ServerWebExchangeMatchers.pathMatchers(properties.getWhiteList().toArray(new String[0]));
  }

  @Override
  public Mono<Authentication> convert(ServerWebExchange exchange) {
    return noAuthServerWebExchangeMatcher
        .matches(exchange)
        .filter(ServerWebExchangeMatcher.MatchResult::isMatch)
        .map(matchResult -> GEUST)
        .switchIfEmpty(
            authServerWebExchangeMatcher
                .matches(exchange)
                .filter(ServerWebExchangeMatcher.MatchResult::isMatch)
                .map(matchResult -> getNeedAuthentication(exchange))
                .switchIfEmpty(Mono.just(DENIED)));
  }

  private Authentication getNeedAuthentication(ServerWebExchange exchange) {
    HttpHeaders httpHeaders = exchange.getRequest().getHeaders();
    String identity = httpHeaders.getFirst(IDENTITY);
    List<SimpleGrantedAuthority> grantedAuthorities = cache.get(identity);
    if (grantedAuthorities == null || grantedAuthorities.isEmpty()) {
      throw new BadCredentialsException("Could not find any roles for the request.");
    }
    return new SimpleAuthenticationToken(
        httpHeaders.getFirst(SIGNATURE),
        identity,
        httpHeaders.getFirst(TIMESTAMP),
        httpHeaders.getFirst(RANDOM),
        grantedAuthorities);
  }
}
