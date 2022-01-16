package com.silong.foundation.duuid.server.configure;

import com.silong.foundation.duuid.server.configure.properties.DuuidServerProperties;
import com.silong.foundation.duuid.server.security.authencation.SimpleAuthenticationWebFilter;
import com.silong.foundation.duuid.server.security.authorization.SimpleReactiveAuthorizationManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authorization.AuthorizationWebFilter;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.stream.Stream;

import static com.silong.foundation.constants.CommonErrorCode.AUTHENTICATION_FAILED;
import static com.silong.foundation.constants.CommonErrorCode.INSUFFICIENT_PERMISSIONS;
import static org.springframework.security.config.web.server.SecurityWebFiltersOrder.AUTHENTICATION;
import static org.springframework.security.config.web.server.SecurityWebFiltersOrder.AUTHORIZATION;

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
@EnableConfigurationProperties(ManagementServerProperties.class)
public class SecurityAutoConfiguration {

  /** 配置 */
  private DuuidServerProperties duuidServerProperties;

  private WebEndpointProperties webEndpointProperties;

  @Bean
  SecurityWebFilterChain springSecurityFilterChain(
      ServerHttpSecurity http,
      @Value("${spring.application.name}") String appName,
      @Value("${management.endpoints.web.base-path}") String actuatorBasePath) {
    return http.csrf()
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
        .authenticationEntryPoint((exchange, e) -> handleAuthenticationException(appName, exchange))
        .and()
        .authorizeExchange()
        .pathMatchers(duuidServerProperties.getAuthWhiteList().toArray(new String[0]))
        .permitAll()
        .pathMatchers(
            authPaths(
                duuidServerProperties.getServicePath(),
                actuatorBasePath,
                webEndpointProperties.getExposure().getInclude()))
        .authenticated()
        .anyExchange()
        .denyAll()
        .and()
        // 配置鉴权过滤器
        .addFilterAt(new SimpleAuthenticationWebFilter(duuidServerProperties), AUTHENTICATION)
        // 配置授权过滤器
        .addFilterAt(
            new AuthorizationWebFilter(
                new SimpleReactiveAuthorizationManager(
                    duuidServerProperties.getUserRolesMappings(),
                    duuidServerProperties.getRolePathsMappings())),
            AUTHORIZATION)
        .build();
  }

  private String[] authPaths(
      String servicePath, String actuatorBasePath, Collection<String> exportEndpoints) {
    return Stream.concat(
            Stream.of(servicePath),
            exportEndpoints.stream().map(s -> String.format("%s/%s", actuatorBasePath, s)))
        .toArray(String[]::new);
  }

  private CorsConfigurationSource corsConfiguration() {
    CorsConfiguration corsConfig = new CorsConfiguration();
    corsConfig.applyPermitDefaultValues();
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", corsConfig);
    return source;
  }

  private Mono<Void> handleAuthenticationException(String appName, ServerWebExchange exchange) {
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

  @Autowired
  public void setWebEndpointProperties(WebEndpointProperties webEndpointProperties) {
    this.webEndpointProperties = webEndpointProperties;
  }

  @Autowired
  public void setDuuidServerProperties(DuuidServerProperties duuidServerProperties) {
    this.duuidServerProperties = duuidServerProperties;
  }
}
