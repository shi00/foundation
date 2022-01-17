package com.silong.foundation.springboot.starter.simpleauth.configure;

import com.silong.foundation.springboot.starter.simpleauth.configure.properties.SimpleAuthProperties;
import com.silong.foundation.springboot.starter.simpleauth.security.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.reactive.config.EnableWebFlux;

import static org.springframework.security.config.web.server.SecurityWebFiltersOrder.AUTHENTICATION;

/**
 * 安全配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-17 13:50
 */
@EnableWebFlux
@EnableConfigurationProperties(SimpleAuthProperties.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class SecurityAutoConfiguration {
  /** 服务名 */
  @Value("${spring.application.name:webflux-server}")
  private String appName;

  @Bean
  @ConditionalOnProperty(prefix = "simple-auth", value = "work-key")
  @ConditionalOnBean(CorsConfigurationSource.class)
  SecurityWebFilterChain springSecurityFilterChain(
      ServerHttpSecurity http,
      SimpleAuthProperties simpleAuthProperties,
      CorsConfigurationSource corsConfigurationSource) {
    return http.csrf()
        .disable()
        .cors()
        .configurationSource(corsConfigurationSource)
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
        .accessDeniedHandler(new SimpleServerAccessDeniedHandler(appName))
        // 定制鉴权失败异常处理
        .authenticationEntryPoint(new SimpleServerAuthenticationEntryPoint(appName))
        .and()
        .authorizeExchange()
        .pathMatchers(simpleAuthProperties.getWhiteList().toArray(new String[0]))
        .permitAll()
        .pathMatchers(simpleAuthProperties.getAuthList().toArray(new String[0]))
        .authenticated()
        .anyExchange()
        .denyAll()
        .and()
        .addFilterAt(authenticationWebFilter(simpleAuthProperties), AUTHENTICATION)
        .build();
  }

  AuthenticationWebFilter authenticationWebFilter(SimpleAuthProperties simpleAuthProperties) {
    AuthenticationWebFilter authenticationWebFilter =
        new AuthenticationWebFilter(new SimpleReactiveAuthenticationManager(simpleAuthProperties));
    authenticationWebFilter.setServerAuthenticationConverter(
        new SimpleServerAuthenticationConverter(simpleAuthProperties));
    authenticationWebFilter.setAuthenticationSuccessHandler(
        SimpleServerAuthenticationSuccessHandler.getInstance());
    return authenticationWebFilter;
  }
}
