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
package com.silong.foundation.duuid.server.configure;

import com.silong.foundation.crypto.aes.AesGcmToolkit;
import com.silong.foundation.duuid.generator.DuuidGenerator;
import com.silong.foundation.duuid.generator.impl.CircularQueueDuuidGenerator;
import com.silong.foundation.duuid.server.configure.properties.DuuidGeneratorProperties;
import com.silong.foundation.duuid.server.configure.properties.DuuidServerProperties;
import com.silong.foundation.duuid.server.configure.properties.EtcdProperties;
import com.silong.foundation.duuid.server.configure.properties.MysqlProperties;
import com.silong.foundation.duuid.server.handlers.IdGeneratorHandler;
import com.silong.foundation.duuid.server.model.Duuid;
import com.silong.foundation.duuid.spi.Etcdv3WorkerIdAllocator;
import com.silong.foundation.duuid.spi.MysqlWorkerIdAllocator;
import com.silong.foundation.duuid.spi.WorkerIdAllocator;
import com.silong.foundation.duuid.spi.WorkerInfo;
import com.silong.foundation.model.ErrorDetail;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

import static com.silong.foundation.constants.HttpStatusCode.*;
import static com.silong.foundation.duuid.generator.impl.CircularQueueDuuidGenerator.Constants.SYSTEM_CLOCK_PROVIDER;
import static com.silong.foundation.duuid.spi.Etcdv3WorkerIdAllocator.*;
import static com.silong.foundation.duuid.spi.MysqlWorkerIdAllocator.*;
import static com.silong.foundation.springboot.starter.simpleauth.constants.AuthHeaders.*;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliteratorUnknownSize;
import static org.springframework.http.MediaType.*;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

/**
 * ????????????????????????
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-13 21:23
 */
@Configuration
@EnableWebFlux
@AutoConfigureBefore(WebFluxAutoConfiguration.class)
@EnableConfigurationProperties({
  DuuidGeneratorProperties.class,
  EtcdProperties.class,
  MysqlProperties.class,
  DuuidServerProperties.class
})
@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP2"},
    justification = "??????????????????")
public class RoutesAutoConfiguration {

  /** ???????????? */
  private DuuidGeneratorProperties generatorProperties;

  private EtcdProperties etcdProperties;

  private MysqlProperties mysqlProperties;

  private DuuidServerProperties serverProperties;

  @Bean
  @ConditionalOnProperty(
      prefix = "duuid.worker-id-provider.etcdv3",
      value = "enabled",
      havingValue = "true")
  WorkerIdAllocator registerEtcdV3WorkerIdAllocator() {
    return load(Etcdv3WorkerIdAllocator.class.getName());
  }

  @Bean
  @ConditionalOnProperty(
      prefix = "duuid.worker-id-provider.mysql",
      value = "enabled",
      havingValue = "true")
  WorkerIdAllocator registerMysqlWorkerIdAllocator() {
    return load(MysqlWorkerIdAllocator.class.getName());
  }

  @Bean
  @ConditionalOnProperty(
      prefix = "duuid.worker-id-provider.etcdv3",
      value = "enabled",
      havingValue = "true")
  WorkerInfo registerEtcdV3WorkerInfo() {
    return WorkerInfo.builder()
        .name(SystemUtils.getHostName())
        .extraInfo(
            Map.of(
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
                StringUtils.isEmpty(etcdProperties.getPassword())
                    ? etcdProperties.getPassword()
                    : AesGcmToolkit.decrypt(
                        etcdProperties.getPassword(), serverProperties.getWorkKey())))
        .build();
  }

  @Bean
  @ConditionalOnProperty(
      prefix = "duuid.worker-id-provider.mysql",
      value = "enabled",
      havingValue = "true")
  WorkerInfo registerMysqlWorkerInfo() {
    return WorkerInfo.builder()
        .name(SystemUtils.getHostName())
        .extraInfo(
            Map.of(
                JDBC_DRIVER,
                mysqlProperties.getJdbcDriver(),
                HOST_NAME,
                SystemUtils.getHostName(),
                JDBC_URL,
                mysqlProperties.getJdbcUrl(),
                USER,
                mysqlProperties.getUserName(),
                PASSWORD,
                StringUtils.isEmpty(mysqlProperties.getPassword())
                    ? mysqlProperties.getPassword()
                    : AesGcmToolkit.decrypt(
                        mysqlProperties.getPassword(), serverProperties.getWorkKey())))
        .build();
  }

  @Bean(destroyMethod = "close")
  DuuidGenerator registerIdGenerator(WorkerIdAllocator allocator, WorkerInfo workerInfo) {
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
  IdGeneratorHandler registerHandler(
      @Value("${spring.application.name}") String applicationName,
      DuuidGenerator duuidGenerator,
      MeterRegistry meterRegistry) {
    return new IdGeneratorHandler(applicationName, duuidGenerator, meterRegistry);
  }

  @Bean
  @RouterOperations(
      @RouterOperation(
          produces = {APPLICATION_JSON_VALUE},
          consumes = {ALL_VALUE},
          method = POST,
          beanClass = IdGeneratorHandler.class,
          beanMethod = "handle",
          operation =
              @Operation(
                  operationId = "nextId",
                  summary = "Generate a globally unique id",
                  tags = {"nextId"},
                  security = {
                    @SecurityRequirement(name = IDENTITY),
                    @SecurityRequirement(name = SIGNATURE),
                    @SecurityRequirement(name = RANDOM),
                    @SecurityRequirement(name = TIMESTAMP)
                  },
                  responses = {
                    @ApiResponse(
                        responseCode = OK,
                        description = "OK",
                        content = @Content(schema = @Schema(implementation = Duuid.class))),
                    @ApiResponse(
                        responseCode = UNAUTHORIZED,
                        description = "UNAUTHORIZED",
                        content = @Content(schema = @Schema(implementation = ErrorDetail.class))),
                    @ApiResponse(
                        responseCode = FORBIDDEN,
                        description = "FORBIDDEN",
                        content = @Content(schema = @Schema(implementation = ErrorDetail.class)))
                  })))
  RouterFunction<ServerResponse> routes(IdGeneratorHandler handler) {
    return RouterFunctions.route(POST(serverProperties.getServicePath()).and(accept(ALL)), handler);
  }

  private WorkerIdAllocator load(String fqdn) {
    return StreamSupport.stream(
            spliteratorUnknownSize(ServiceLoader.load(WorkerIdAllocator.class).iterator(), ORDERED),
            false)
        .filter(allocator -> allocator.getClass().getName().equals(fqdn))
        .findAny()
        .orElseThrow(
            () -> new RuntimeException("No WorkerIdAllocator implementation could be found."));
  }

  @Autowired
  public void setGeneratorProperties(DuuidGeneratorProperties generatorProperties) {
    this.generatorProperties = generatorProperties;
  }

  @Autowired
  public void setMysqlProperties(MysqlProperties mysqlProperties) {
    this.mysqlProperties = mysqlProperties;
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
