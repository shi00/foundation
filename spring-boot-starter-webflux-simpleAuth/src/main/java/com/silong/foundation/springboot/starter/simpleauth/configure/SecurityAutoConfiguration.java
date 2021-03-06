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
 * ????????????
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-17 13:50
 */
@EnableWebFlux
@EnableConfigurationProperties(SimpleAuthProperties.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class SecurityAutoConfiguration {
  /** ????????? */
  private String appName;

  /** jackson */
  private ObjectMapper objectMapper;

  /** ?????? */
  private SimpleAuthProperties simpleAuthProperties;

  /** cors?????? */
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
    // ??????csrf
    http = http.csrf().disable();

    // ????????????CORS
    if (corsConfigurationSource != null) {
      http = http.cors().configurationSource(corsConfigurationSource).and();
    }

    // ??????http????????????
    http = http.httpBasic().disable();

    // ??????????????????
    http = http.formLogin().disable();

    // ????????????
    http = http.logout().disable();

    // ??????????????????
    http = http.anonymous().disable();

    // ??????????????????
    http = http.requestCache().requestCache(NoOpServerRequestCache.getInstance()).and();

    // ????????????????????????
    http = http.securityContextRepository(NoOpServerSecurityContextRepository.getInstance());

    return http
        // ??????????????????????????????
        .exceptionHandling()
        .accessDeniedHandler(accessDeniedHandler)
        // ??????????????????????????????
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
        // ?????????????????????
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
