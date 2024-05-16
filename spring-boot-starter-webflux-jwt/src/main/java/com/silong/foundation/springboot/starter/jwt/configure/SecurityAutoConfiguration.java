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

import static com.silong.foundation.springboot.starter.jwt.common.Constants.TOKEN_CACHE;
import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.REACTIVE;
import static org.springframework.security.config.web.server.SecurityWebFiltersOrder.HTTP_BASIC;
import static org.springframework.security.crypto.password.Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256;

import com.auth0.jwt.algorithms.Algorithm;
import com.silong.foundation.crypto.aes.AesGcmToolkit;
import com.silong.foundation.springboot.starter.jwt.configure.config.JWTAuthProperties;
import com.silong.foundation.springboot.starter.jwt.provider.DefaultJwtProvider;
import com.silong.foundation.springboot.starter.jwt.provider.DefaultUserDetailsProvider;
import com.silong.foundation.springboot.starter.jwt.provider.JWTProvider;
import com.silong.foundation.springboot.starter.jwt.provider.UserDetailsProvider;
import com.silong.foundation.springboot.starter.jwt.security.SimpleReactiveAuthenticationManager;
import com.silong.foundation.springboot.starter.jwt.security.SimpleServerAccessDeniedHandler;
import com.silong.foundation.springboot.starter.jwt.security.SimpleServerAuthenticationConverter;
import com.silong.foundation.springboot.starter.jwt.security.SimpleServerAuthenticationEntryPoint;
import com.silong.foundation.springboot.starter.jwt.security.SimpleServerAuthenticationSuccessHandler;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
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
    value = {"sign-key", "work-key"})
public class SecurityAutoConfiguration {
  /** 服务名 */
  private String appName;

  /** token缓存 */
  private Map<String, String> tokenCache;

  /** 用户信息提供者 */
  private UserDetailsProvider userDetailsProvider;

  /** 配置 */
  private JWTAuthProperties JWTAuthProperties;

  @Bean
  @ConditionalOnMissingBean
  JWTProvider jwtProvider(Algorithm algorithm, String appName) {
    return new DefaultJwtProvider(algorithm, appName);
  }

  @Bean
  @ConditionalOnMissingBean
  PasswordEncoder passwordEncoder() {
    return new Pbkdf2PasswordEncoder(
        AesGcmToolkit.decrypt(JWTAuthProperties.getSignKey(), JWTAuthProperties.getWorkKey()),
        16,
        310000,
        PBKDF2WithHmacSHA256);
  }

  @Bean
  @ConditionalOnMissingBean
  UserDetailsProvider defaultUserDetailsProvider() {
    return new DefaultUserDetailsProvider();
  }

  @Bean
  @ConditionalOnMissingBean
  Algorithm registerTokenSignAlg() {
    return Algorithm.HMAC256(
        AesGcmToolkit.decrypt(JWTAuthProperties.getSignKey(), JWTAuthProperties.getWorkKey()));
  }

  @Bean
  SecurityWebFilterChain springSecurityFilterChain(
      ServerHttpSecurity http, JWTProvider jwtProvider) {
    return configure(
            http,
            jwtProvider,
            JWTAuthProperties,
            new SimpleServerAuthenticationEntryPoint(appName),
            new SimpleServerAccessDeniedHandler(appName))
        .build();
  }

  private ServerHttpSecurity configure(
      ServerHttpSecurity http,
      JWTProvider jwtProvider,
      JWTAuthProperties JWTAuthProperties,
      SimpleServerAuthenticationEntryPoint authenticationEntryPoint,
      SimpleServerAccessDeniedHandler accessDeniedHandler) {
    // 关闭csrf，rest接口不提供浏览器使用
    http = http.csrf(CsrfSpec::disable);

    // 关闭跨越资源共享CORS
    http = http.cors(CorsSpec::disable);

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

    // 登录接口加入白名单
    String[] whiteList = JWTAuthProperties.getWhiteList().toArray(new String[0]);

    log.info("whiteList: {}", String.join(", ", whiteList));

    String[] authList = JWTAuthProperties.getAuthList().toArray(new String[0]);

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
                    .pathMatchers(whiteList) // 其他无需鉴权接口，如：查询版本
                    .permitAll()
                    .pathMatchers(authList) // 须鉴权接口
                    .authenticated()
                    .anyExchange()
                    .denyAll())
        // 定制鉴权过滤器
        .addFilterAt(
            authenticationWebFilter(JWTAuthProperties, jwtProvider, authenticationEntryPoint),
            HTTP_BASIC);
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
  @Qualifier(TOKEN_CACHE)
  public void setTokenCache(Map<String, String> tokenCache) {
    this.tokenCache = tokenCache;
  }

  @Autowired
  public void setAppName(@Value("${spring.application.name}") String appName) {
    this.appName = appName;
  }
}
