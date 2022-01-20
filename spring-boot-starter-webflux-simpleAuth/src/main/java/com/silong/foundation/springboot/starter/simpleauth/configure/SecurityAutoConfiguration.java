package com.silong.foundation.springboot.starter.simpleauth.configure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.silong.foundation.springboot.starter.simpleauth.configure.properties.SimpleAuthProperties;
import com.silong.foundation.springboot.starter.simpleauth.security.authentication.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationEntryPointFailureHandler;
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
  @ConditionalOnBean({CorsConfigurationSource.class, ObjectMapper.class})
  SecurityWebFilterChain springSecurityFilterChain(
      ServerHttpSecurity http,
      ObjectMapper objectMapper,
      SimpleAuthProperties simpleAuthProperties,
      CorsConfigurationSource corsConfigurationSource) {
    // 提供cors配置确保前端页面可以跨域访问，因为管理端口和业务端口是分离的，不提供cors配置会导致页面无法访问
    // 例如：swagger-ui页面
    return configure(
            http,
            simpleAuthProperties,
            corsConfigurationSource,
            new SimpleServerAuthenticationEntryPoint(appName, objectMapper),
            new SimpleServerAccessDeniedHandler(appName, objectMapper))
        .build();
  }

  @Bean
  @ConditionalOnProperty(prefix = "simple-auth", value = "work-key")
  @ConditionalOnBean({ObjectMapper.class})
  @ConditionalOnMissingBean(CorsConfigurationSource.class)
  SecurityWebFilterChain springSecurityFilterChain(
      ServerHttpSecurity http,
      ObjectMapper objectMapper,
      SimpleAuthProperties simpleAuthProperties) {
    return configure(
            http,
            simpleAuthProperties,
            null,
            new SimpleServerAuthenticationEntryPoint(appName, objectMapper),
            new SimpleServerAccessDeniedHandler(appName, objectMapper))
        .build();
  }

  private ServerHttpSecurity configure(
      ServerHttpSecurity http,
      SimpleAuthProperties simpleAuthProperties,
      CorsConfigurationSource corsConfigurationSource,
      SimpleServerAuthenticationEntryPoint authenticationEntryPoint,
      SimpleServerAccessDeniedHandler accessDeniedHandler) {
    // 关闭csrf
    http = http.csrf().disable();

    // 是否开启CORS
    if (corsConfigurationSource != null) {
      http = http.cors().configurationSource(corsConfigurationSource).and();
    }

    // 关闭http基础鉴权
    http = http.httpBasic().disable();

    // 关闭表单登录
    http = http.formLogin().disable();

    // 关闭登出
    http = http.logout().disable();

    // 关闭匿名用户
    http = http.anonymous().disable();

    // 关闭请求缓存
    http = http.requestCache().requestCache(NoOpServerRequestCache.getInstance()).and();

    // 无安全上下文缓存
    http = http.securityContextRepository(NoOpServerSecurityContextRepository.getInstance());

    return http
        // 定制权限不足异常处理
        .exceptionHandling()
        .accessDeniedHandler(accessDeniedHandler)
        // 定制鉴权失败异常处理
        .authenticationEntryPoint(authenticationEntryPoint)
        .and()
        .authorizeExchange()
        .pathMatchers(simpleAuthProperties.getWhiteList().toArray(new String[0]))
        .permitAll()
        .pathMatchers(simpleAuthProperties.getAuthList().toArray(new String[0]))
        .authenticated()
        .anyExchange()
        .denyAll()
        .and()
        // 定制鉴权过滤器
        .addFilterAt(
            authenticationWebFilter(simpleAuthProperties, authenticationEntryPoint),
            AUTHENTICATION);
  }

  AuthenticationWebFilter authenticationWebFilter(
      SimpleAuthProperties simpleAuthProperties,
      SimpleServerAuthenticationEntryPoint simpleServerAuthenticationEntryPoint) {
    AuthenticationWebFilter authenticationWebFilter =
        new AuthenticationWebFilter(new SimpleReactiveAuthenticationManager(simpleAuthProperties));
    authenticationWebFilter.setServerAuthenticationConverter(
        new SimpleServerAuthenticationConverter(simpleAuthProperties));
    authenticationWebFilter.setAuthenticationSuccessHandler(
        SimpleServerAuthenticationSuccessHandler.getInstance());
    authenticationWebFilter.setAuthenticationFailureHandler(
        new ServerAuthenticationEntryPointFailureHandler(simpleServerAuthenticationEntryPoint));
    return authenticationWebFilter;
  }
}
