package com.silong.foundation.duuid.server.configure;

import com.google.common.collect.ImmutableMap;
import com.silong.foundation.duuid.generator.DuuidGenerator;
import com.silong.foundation.duuid.generator.impl.CircularQueueDuuidGenerator;
import com.silong.foundation.duuid.server.configure.properties.DuuidGeneratorProperties;
import com.silong.foundation.duuid.server.configure.properties.DuuidServerProperties;
import com.silong.foundation.duuid.server.configure.properties.EtcdProperties;
import com.silong.foundation.duuid.server.handlers.IdGeneratorHandler;
import com.silong.foundation.duuid.server.model.Duuid;
import com.silong.foundation.duuid.spi.WorkerIdAllocator;
import com.silong.foundation.duuid.spi.WorkerInfo;
import com.silong.foundation.model.ErrorDetail;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

import static com.silong.foundation.constants.HttpStatusCode.*;
import static com.silong.foundation.duuid.generator.impl.CircularQueueDuuidGenerator.Constants.SYSTEM_CLOCK_PROVIDER;
import static com.silong.foundation.duuid.spi.Etcdv3WorkerIdAllocator.*;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliteratorUnknownSize;
import static org.springframework.http.MediaType.*;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

/**
 * 服务端点路由配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-13 21:23
 */
@Configuration
@EnableConfigurationProperties({
  DuuidGeneratorProperties.class,
  EtcdProperties.class,
  DuuidServerProperties.class
})
public class RoutesAutoConfiguration {

  /** 配置注入 */
  private DuuidGeneratorProperties generatorProperties;

  private EtcdProperties etcdProperties;

  private DuuidServerProperties serverProperties;

  @Bean
  WorkerIdAllocator registerWorkerIdAllocator() {
    ServiceLoader<WorkerIdAllocator> workerIdAllocators =
        ServiceLoader.load(WorkerIdAllocator.class);
    return StreamSupport.stream(
            spliteratorUnknownSize(workerIdAllocators.iterator(), ORDERED), false)
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
  WorkerInfo registerWorkerInfo() {
    return WorkerInfo.builder()
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
  }

  @Bean
  @ConditionalOnProperty(name = "duuid.worker-id-provider.etcdv3.enabled", havingValue = "true")
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
      @Value("spring.application.name") String applicationName, DuuidGenerator duuidGenerator) {
    return new IdGeneratorHandler(applicationName, duuidGenerator);
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
    return RouterFunctions.route(POST(serverProperties.getPath()).and(accept(ALL)), handler);
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
