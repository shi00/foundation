/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package com.silong.foundation.springboot.starter.jwt.configure;

import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.REACTIVE;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.security.config.web.server.SecurityWebFiltersOrder.HTTP_BASIC;

import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.silong.foundation.springboot.starter.jwt.configure.config.JWTAuthProperties;
import com.silong.foundation.springboot.starter.jwt.provider.DefaultJwtProvider;
import com.silong.foundation.springboot.starter.jwt.provider.JWTProvider;
import com.silong.foundation.springboot.starter.jwt.provider.UserDetailsProvider;
import com.silong.foundation.springboot.starter.jwt.security.SimpleReactiveAuthenticationManager;
import com.silong.foundation.springboot.starter.jwt.security.SimpleServerAccessDeniedHandler;
import com.silong.foundation.springboot.starter.jwt.security.SimpleServerAuthenticationConverter;
import com.silong.foundation.springboot.starter.jwt.security.SimpleServerAuthenticationEntryPoint;
import com.silong.foundation.springboot.starter.jwt.security.SimpleServerAuthenticationSuccessHandler;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity.*;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationEntryPointFailureHandler;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;

/**
 * 安全配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-17 13:50
 */
@Slf4j
@EnableWebFluxSecurity
@Configuration
@EnableConfigurationProperties(JWTAuthProperties.class)
@ConditionalOnWebApplication(type = REACTIVE)
@ConditionalOnProperty(
    prefix = "jwt-auth",
    value = {"sign-key", "auth-path"})
public class SecurityAutoConfiguration {
  /** 服务名 */
  private String appName;

  /** token缓存 */
  private ConcurrentMap<String, String> tokenCache;

  /** 用户信息提供者 */
  private UserDetailsProvider userDetailsProvider;

  /** 配置 */
  private JWTAuthProperties JWTAuthProperties;

  private ObjectMapper objectMapper;

  @Bean
  @ConditionalOnMissingBean
  JWTProvider jwtProvider(Algorithm algorithm) {
    return new DefaultJwtProvider(algorithm, appName);
  }

  @Bean
  @ConditionalOnMissingBean
  Algorithm registerTokenSignAlg() {
    return Algorithm.HMAC256(JWTAuthProperties.getSignKey());
  }

  @Bean
  SecurityWebFilterChain springSecurityFilterChain(
      ServerHttpSecurity http, JWTProvider jwtProvider) {
    return configure(
            http,
            jwtProvider,
            JWTAuthProperties,
            new SimpleServerAuthenticationEntryPoint(appName, objectMapper),
            new SimpleServerAccessDeniedHandler(appName, objectMapper))
        .build();
  }

  private ServerHttpSecurity configure(
      ServerHttpSecurity http,
      JWTProvider jwtProvider,
      JWTAuthProperties JWTAuthProperties,
      SimpleServerAuthenticationEntryPoint authenticationEntryPoint,
      SimpleServerAccessDeniedHandler accessDeniedHandler) {

    return
    // 关闭csrf，rest接口不提供浏览器使用
    http.csrf(CsrfSpec::disable)

        // 关闭跨越资源共享CORS
        .cors(CorsSpec::disable)

        // 关闭http基础鉴权
        .httpBasic(HttpBasicSpec::disable)

        // 关闭表单登录
        .formLogin(FormLoginSpec::disable)

        // 关闭登出
        .logout(LogoutSpec::disable)

        // 关闭匿名用户
        .anonymous(AnonymousSpec::disable)

        // 关闭请求缓存
        .requestCache(
            requestCacheSpec -> requestCacheSpec.requestCache(NoOpServerRequestCache.getInstance()))

        // 无安全上下文缓存
        .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
        // 定制权限不足异常处理
        // 定制鉴权失败异常处理
        .exceptionHandling(
            exceptionHandlingSpec ->
                exceptionHandlingSpec
                    .accessDeniedHandler(accessDeniedHandler)
                    .authenticationEntryPoint(authenticationEntryPoint))
        .authorizeExchange(
            authorizeExchangeSpec -> {
              log.info("whiteList: {}", JWTAuthProperties.getWhiteList());

              log.info("authList: {}", JWTAuthProperties.getAuthList());

              // 配置登录接口无需鉴权
              authorizeExchangeSpec.pathMatchers(POST, JWTAuthProperties.getAuthPath()).permitAll();

              // 按配置配置无需鉴权的接口
              Map<HttpMethod, List<String>> whiteList = JWTAuthProperties.getWhiteList();
              if (whiteList != null && !whiteList.isEmpty()) {
                whiteList.forEach(
                    (k, v) ->
                        authorizeExchangeSpec
                            .pathMatchers(k, v.toArray(String[]::new))
                            .permitAll());
              }

              // 按配置配置需要鉴权的接口
              Map<HttpMethod, List<String>> authList = JWTAuthProperties.getAuthList();
              if (authList != null && !authList.isEmpty()) {
                authList.forEach(
                    (k, v) ->
                        authorizeExchangeSpec
                            .pathMatchers(k, v.toArray(String[]::new))
                            .authenticated());
              }

              // 其他访问路径一律拒绝访问
              authorizeExchangeSpec.anyExchange().denyAll();
            })
        .addFilterAt(
            authenticationWebFilter(JWTAuthProperties, jwtProvider, authenticationEntryPoint),
            HTTP_BASIC); // 定制鉴权过滤器
  }

  AuthenticationWebFilter authenticationWebFilter(
      JWTAuthProperties JWTAuthProperties,
      JWTProvider jwtProvider,
      SimpleServerAuthenticationEntryPoint simpleServerAuthenticationEntryPoint) {
    AuthenticationWebFilter authenticationWebFilter =
        new AuthenticationWebFilter(
            new SimpleReactiveAuthenticationManager(
                appName, tokenCache, jwtProvider, userDetailsProvider, JWTAuthProperties));
    authenticationWebFilter.setServerAuthenticationConverter(
        new SimpleServerAuthenticationConverter(JWTAuthProperties));
    authenticationWebFilter.setAuthenticationSuccessHandler(
        SimpleServerAuthenticationSuccessHandler.getInstance());
    authenticationWebFilter.setAuthenticationFailureHandler(
        new ServerAuthenticationEntryPointFailureHandler(simpleServerAuthenticationEntryPoint));
    return authenticationWebFilter;
  }

  @Autowired
  public void setJWTAuthProperties(JWTAuthProperties JWTAuthProperties) {
    this.JWTAuthProperties = JWTAuthProperties;
  }

  @Autowired
  public void setUserDetailsProvider(UserDetailsProvider userDetailsProvider) {
    this.userDetailsProvider = userDetailsProvider;
  }

  @Autowired
  public void setObjectMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Autowired
  public void setTokenCache(ConcurrentMap<String, String> tokenCache) {
    this.tokenCache = tokenCache;
  }

  @Autowired
  public void setAppName(@Value("${spring.application.name}") String appName) {
    this.appName = appName;
  }
}
