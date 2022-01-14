package com.silong.foundation.duuid.server.configure;

import com.silong.foundation.duuid.server.configure.properties.DuuidServerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * 服务自动装配
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-03 10:48
 */
@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties({DuuidServerProperties.class})
public class SecurityAutoConfiguration {

  /** 配置注入 */
  private DuuidServerProperties serverProperties;

  private CorsConfigurationSource corsConfiguration() {
    CorsConfiguration corsConfig = new CorsConfiguration();
    corsConfig.applyPermitDefaultValues();
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", corsConfig);
    return source;
  }

  /**
   * For Spring Security webflux, a chain of filters will provide user authentication and
   * authorization, we add custom filters to enable JWT token approach.
   *
   * @param http An initial object to build common filter scenarios. Customized filters are added
   *     here.
   * @return SecurityWebFilterChain A filter chain for web exchanges that will provide security
   */
  @Bean
  SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
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
        .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
        //        // 定制权限不足异常处理
        //        .exceptionHandling()
        //        .accessDeniedHandler(
        //            (exchange, e) -> {
        //              ServerHttpResponse response = exchange.getResponse();
        //              return response.writeAndFlushWith(
        //                  Mono.fromRunnable(
        //                      () -> {
        //                        ErrorDetail.builder()
        //                            .errorCode(INSUFFICIENT_PERMISSIONS)
        //                            .errorMessage(messages.get(INSUFFICIENT_PERMISSIONS,
        // e.getMessage()))
        //                            .build();
        //                      }));
        //            })
        //
        //        // 定制鉴权失败异常处理
        //        .authenticationEntryPoint(
        //            (exchange, e) -> {
        //              ServerHttpResponse response = exchange.getResponse();
        //              return response.writeAndFlushWith(
        //                  Mono.fromRunnable(
        //                      () -> {
        //                        ErrorDetail.builder()
        //                            .code(AUTHENTICATION_FAILED)
        //                            .message(messages.get(AUTHENTICATION_FAILED, e.getMessage()))
        //                            .build();
        //                      }));
        //            })
        //
        //        // 添加鉴权过滤器
        //        // 定制需要鉴权的请求路径
        //        .and()
        //        .addFilterAt(getAuthWebFilter(), FIRST)
        .authorizeExchange()
        //        .pathMatchers(
        //            Stream.concat(props.getAuthWhiteList().stream(),
        // Stream.of(props.getAuthUrl()))
        //                .toArray(String[]::new))
        //        .permitAll()
        .anyExchange()
        .permitAll();
    return http.build();
  }

  @Autowired
  public void setServerProperties(DuuidServerProperties serverProperties) {
    this.serverProperties = serverProperties;
  }
}
