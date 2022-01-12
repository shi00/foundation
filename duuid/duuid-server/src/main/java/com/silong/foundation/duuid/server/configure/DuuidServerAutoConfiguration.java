package com.silong.foundation.duuid.server.configure;

import com.google.common.collect.ImmutableMap;
import com.silong.foundation.duuid.generator.DuuidGenerator;
import com.silong.foundation.duuid.generator.impl.CircularQueueDuuidGenerator;
import com.silong.foundation.duuid.server.configure.properties.DuuidGeneratorProperties;
import com.silong.foundation.duuid.server.configure.properties.DuuidServerProperties;
import com.silong.foundation.duuid.server.configure.properties.EtcdProperties;
import com.silong.foundation.duuid.spi.WorkerIdAllocator;
import com.silong.foundation.duuid.spi.WorkerInfo;
import com.silong.foundation.duuid.server.handlers.IdGeneratorHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

import static com.silong.foundation.duuid.generator.impl.CircularQueueDuuidGenerator.Constants.SYSTEM_CLOCK_PROVIDER;
import static com.silong.foundation.duuid.spi.Etcdv3WorkerIdAllocator.*;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliteratorUnknownSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

/**
 * 服务自动装配
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-03 10:48
 */
@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties({
  DuuidGeneratorProperties.class,
  EtcdProperties.class,
  DuuidServerProperties.class
})
public class DuuidServerAutoConfiguration {

  /** 配置注入 */
  private DuuidGeneratorProperties generatorProperties;

  private EtcdProperties etcdProperties;

  private DuuidServerProperties serverProperties;

  @Bean
  WorkerIdAllocator registerWorkerIdAllocator() {
    ServiceLoader<WorkerIdAllocator> load = ServiceLoader.load(WorkerIdAllocator.class);
    return StreamSupport.stream(spliteratorUnknownSize(load.iterator(), ORDERED), false)
        .filter(
            allocator ->
                StringUtils.isEmpty(generatorProperties.getWorkerIdAllocatorFqdn())
                    || allocator
                        .getClass()
                        .getName()
                        .equals(generatorProperties.getWorkerIdAllocatorFqdn()))
        .findFirst()
        .orElseThrow(
            () -> new RuntimeException("No WorkerIdAllocator implementation could be found."));
  }

  @Bean
  @ConditionalOnProperty(name = "duuid.worker-id-provider.etcdv3.enabled", havingValue = "true")
  DuuidGenerator registerIdGenerator(WorkerIdAllocator allocator) {
    WorkerInfo workerInfo =
        WorkerInfo.builder()
            .name(SystemUtils.getHostName())
            .extraInfo(
                ImmutableMap.of(
                    ETCDV3_ENDPOINTS,
                    String.join(",", etcdProperties.getServerAddresses()),
                    ETCDV3_TRUST_CERT_COLLECTION_FILE,
                    etcdProperties.getTrustCertCollectionFile(),
                    ETCDV3_KEY_CERT_CHAIN_FILE,
                    etcdProperties.getKeyCertChainFile(),
                    ETCDV3_KEY_FILE,
                    etcdProperties.getKeyFile(),
                    ETCDV3_USER,
                    etcdProperties.getUserName(),
                    ETCDV3_PASSWORD,
                    etcdProperties.getPassword()))
            .build();
    return new CircularQueueDuuidGenerator(
        generatorProperties.getWorkerIdBits(),
        generatorProperties.getDeltaDaysBits(),
        generatorProperties.getSequenceBits(),
        () -> allocator.allocate(workerInfo),
        SYSTEM_CLOCK_PROVIDER,
        generatorProperties.getSequence(),
        generatorProperties.getQueueCapacity(),
        generatorProperties.isEnableSequenceRandom(),
        generatorProperties.getMaxRandomIncrement());
  }

  @Bean
  IdGeneratorHandler registerHandler(DuuidGenerator duuidGenerator) {
    return new IdGeneratorHandler(duuidGenerator);
  }

  @Bean
  RouterFunction<ServerResponse> routes(IdGeneratorHandler handler) {
    return RouterFunctions.route(
        POST(serverProperties.getPath()).and(accept(APPLICATION_JSON)), handler);
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
  public void setGeneratorProperties(DuuidGeneratorProperties generatorProperties) {
    this.generatorProperties = generatorProperties;
  }

  @Autowired
  public void setEtcdProperties(EtcdProperties etcdProperties) {
    this.etcdProperties = etcdProperties;
  }

  @Autowired
  public void setServerProperties(DuuidServerProperties serverProperties) {
    this.serverProperties = serverProperties;
  }
}
