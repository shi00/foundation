package com.silong.foundation.duuid.server.configure;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.stream.Stream;

import static com.silong.foundation.constants.CommonErrorCode.AUTHENTICATION_FAILED;
import static com.silong.foundation.constants.CommonErrorCode.INSUFFICIENT_PERMISSIONS;

/**
 * 服务自动装配
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-03 10:48
 */
@Slf4j
@Configuration
@EnableWebFluxSecurity
public class SecurityAutoConfiguration {

  /**
   * For Spring Security webflux, a chain of filters will provide user authentication and
   * authorization, we add custom filters to enable JWT token approach.
   *
   * @param http An initial object to build common filter scenarios. Customized filters are added
   *     here.
   * @return SecurityWebFilterChain A filter chain for web exchanges that will provide security
   */
  @Bean
  SecurityWebFilterChain springSecurityFilterChain(
      ServerHttpSecurity http,
      @Value("${spring.application.name}") String appName,
      @Value("${duuid.server.context-path}") String servicesPath,
      @Value("${management.endpoints.web.base-path") String actuatorPath) {
    http.csrf()
        .disable()
        .cors()
        .configurationSource(corsConfiguration())
        .and()
        .formLogin()
        .disable()
        .httpBasic()
        .disable()
        .logout()
        .disable()
        .anonymous()
        .disable()
        .requestCache()
        .requestCache(NoOpServerRequestCache.getInstance())
        .and()
        .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
        // 定制权限不足异常处理
        .exceptionHandling()
        .accessDeniedHandler((exchange, e) -> handleAccessDeniedException(appName, exchange, e))
        // 定制鉴权失败异常处理
        .authenticationEntryPoint(
            (exchange, e) -> handleAuthenticationException(appName, exchange, e))
        .and()
        .authorizeExchange()
        .pathMatchers(Stream.of(actuatorPath, servicesPath).toArray(String[]::new))
        .authenticated()
        .anyExchange()
        .denyAll();
    return http.build();
  }

  private CorsConfigurationSource corsConfiguration() {
    CorsConfiguration corsConfig = new CorsConfiguration();
    corsConfig.applyPermitDefaultValues();
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", corsConfig);
    return source;
  }

  private Mono<Void> handleAuthenticationException(
      String appName, ServerWebExchange exchange, AuthenticationException e) {
    ServerHttpRequest request = exchange.getRequest();
    log.error("Authentication failed, {} {}", request.getMethod(), request.getPath().value(), e);
    return exchange
        .getResponse()
        .writeAndFlushWith(Mono.fromRunnable(() -> AUTHENTICATION_FAILED.format(appName)));
  }

  private Mono<Void> handleAccessDeniedException(
      String appName, ServerWebExchange exchange, AccessDeniedException e) {
    ServerHttpRequest request = exchange.getRequest();
    log.error("Not authorized to {} {}", request.getMethod(), request.getPath().value(), e);
    return exchange
        .getResponse()
        .writeAndFlushWith(Mono.fromRunnable(() -> INSUFFICIENT_PERMISSIONS.format(appName)));
  }
}
