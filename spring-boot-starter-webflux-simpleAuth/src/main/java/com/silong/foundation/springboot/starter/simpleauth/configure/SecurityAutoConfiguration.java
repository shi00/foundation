/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.silong.foundation.springboot.starter.simpleauth.configure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.silong.foundation.springboot.starter.simpleauth.configure.config.SimpleAuthProperties;
import com.silong.foundation.springboot.starter.simpleauth.security.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
  private String appName;

  /** jackson */
  private ObjectMapper objectMapper;

  /** 配置 */
  private SimpleAuthProperties simpleAuthProperties;

  /** cors配置 */
  private CorsConfigurationSource corsConfigurationSource;

  @Bean
  @ConditionalOnProperty(prefix = "simple-auth", value = "work-key")
  SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
    return configure(
            http,
            simpleAuthProperties,
            corsConfigurationSource,
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

  @Autowired(required = false)
  public void setCorsConfigurationSource(CorsConfigurationSource corsConfigurationSource) {
    this.corsConfigurationSource = corsConfigurationSource;
  }

  @Autowired
  public void setSimpleAuthProperties(SimpleAuthProperties simpleAuthProperties) {
    this.simpleAuthProperties = simpleAuthProperties;
  }

  @Autowired
  public void setObjectMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Autowired
  public void setAppName(@Value("${spring.application.name:webflux-server}") String appName) {
    this.appName = appName;
  }
}
