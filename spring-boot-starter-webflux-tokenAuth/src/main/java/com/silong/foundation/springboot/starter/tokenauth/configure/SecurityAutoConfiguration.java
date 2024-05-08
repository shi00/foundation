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

package com.silong.foundation.springboot.starter.tokenauth.configure;

import static com.silong.foundation.springboot.starter.tokenauth.configure.TokenCacheAutoConfiguration.TOKEN_CACHE;
import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.REACTIVE;
import static org.springframework.security.config.web.server.SecurityWebFiltersOrder.AUTHENTICATION;

import com.auth0.jwt.algorithms.Algorithm;
import com.silong.foundation.crypto.aes.AesGcmToolkit;
import com.silong.foundation.springboot.starter.tokenauth.configure.config.SimpleAuthProperties;
import com.silong.foundation.springboot.starter.tokenauth.security.*;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity.*;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationEntryPointFailureHandler;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.reactive.config.EnableWebFlux;

/**
 * 安全配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-17 13:50
 */
@Slf4j
@EnableWebFlux
@Configuration
@EnableConfigurationProperties(SimpleAuthProperties.class)
@ConditionalOnWebApplication(type = REACTIVE)
public class SecurityAutoConfiguration {
  /** 服务名 */
  private String appName;

  /** token缓存 */
  private Map<String, String> tokenCache;

  /** 配置 */
  private SimpleAuthProperties simpleAuthProperties;

  /** cors配置 */
  private CorsConfigurationSource corsConfigurationSource;

  @Bean
  @ConditionalOnProperty(
      prefix = "token-auth",
      value = {"sign-key", "work-key"})
  Algorithm registerSignAlg() {
    return Algorithm.HMAC256(
        AesGcmToolkit.decrypt(
            simpleAuthProperties.getSignKey(), simpleAuthProperties.getWorkKey()));
  }

  @Bean
  @ConditionalOnProperty(
      prefix = "token-auth",
      value = {"sign-key", "work-key"})
  SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http, Algorithm signAlg) {
    return configure(
            http,
            signAlg,
            simpleAuthProperties,
            corsConfigurationSource,
            new SimpleServerAuthenticationEntryPoint(appName),
            new SimpleServerAccessDeniedHandler(appName))
        .build();
  }

  private ServerHttpSecurity configure(
      ServerHttpSecurity http,
      Algorithm signAlg,
      SimpleAuthProperties simpleAuthProperties,
      CorsConfigurationSource corsConfigurationSource,
      SimpleServerAuthenticationEntryPoint authenticationEntryPoint,
      SimpleServerAccessDeniedHandler accessDeniedHandler) {
    // 关闭csrf，rest接口不提供浏览器使用
    http = http.csrf(CsrfSpec::disable);

    // 是否开启CORS
    if (corsConfigurationSource != null) {
      http = http.cors(cors -> cors.configurationSource(corsConfigurationSource));
    }

    // 关闭http基础鉴权
    http = http.httpBasic(HttpBasicSpec::disable);

    // 关闭表单登录
    http = http.formLogin(FormLoginSpec::disable);

    // 关闭登出
    http = http.logout(LogoutSpec::disable);

    // 关闭匿名用户
    http = http.anonymous(AnonymousSpec::disable);

    // 关闭请求缓存
    http =
        http.requestCache(
            requestCacheSpec ->
                requestCacheSpec.requestCache(NoOpServerRequestCache.getInstance()));

    // 无安全上下文缓存
    http = http.securityContextRepository(NoOpServerSecurityContextRepository.getInstance());

    String[] whiteList = simpleAuthProperties.getWhiteList().toArray(new String[0]);

    log.info("whiteList: {}", String.join(", ", whiteList));

    String[] authList = simpleAuthProperties.getAuthList().toArray(new String[0]);

    log.info("authList: {}", String.join(", ", authList));

    return http
        // 定制权限不足异常处理
        // 定制鉴权失败异常处理
        .exceptionHandling(
            exceptionHandlingSpec ->
                exceptionHandlingSpec
                    .accessDeniedHandler(accessDeniedHandler)
                    .authenticationEntryPoint(authenticationEntryPoint))
        .authorizeExchange(
            authorizeExchangeSpec ->
                authorizeExchangeSpec
                    .pathMatchers(whiteList)
                    .permitAll()
                    .pathMatchers(authList)
                    .authenticated()
                    .anyExchange()
                    .denyAll())
        // 定制鉴权过滤器
        .addFilterAt(
            authenticationWebFilter(simpleAuthProperties, signAlg, authenticationEntryPoint),
            AUTHENTICATION);
  }

  AuthenticationWebFilter authenticationWebFilter(
      SimpleAuthProperties simpleAuthProperties,
      Algorithm signAlg,
      SimpleServerAuthenticationEntryPoint simpleServerAuthenticationEntryPoint) {
    AuthenticationWebFilter authenticationWebFilter =
        new AuthenticationWebFilter(
            new SimpleReactiveAuthenticationManager(tokenCache, simpleAuthProperties, appName));
    authenticationWebFilter.setServerAuthenticationConverter(
        new SimpleServerAuthenticationConverter(simpleAuthProperties, signAlg, appName));
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
  @Qualifier(TOKEN_CACHE)
  public void setTokenCache(Map<String, String> tokenCache) {
    this.tokenCache = tokenCache;
  }

  @Autowired
  public void setAppName(@Value("${spring.application.name}") String appName) {
    this.appName = appName;
  }
}
