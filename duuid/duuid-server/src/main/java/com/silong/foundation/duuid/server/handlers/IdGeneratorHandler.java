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
package com.silong.foundation.duuid.server.handlers;

import com.silong.foundation.duuid.generator.DuuidGenerator;
import com.silong.foundation.duuid.server.model.Duuid;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static com.silong.foundation.constants.CommonErrorCode.SERVICE_INTERNAL_ERROR;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * id生成器处理器，配合webflux使用
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-03 12:28
 */
@Slf4j
public class IdGeneratorHandler implements HandlerFunction<ServerResponse> {

  /** 服务名 */
  private final String serviceName;

  /** id生成器 */
  private final DuuidGenerator duuidGenerator;

  /** 请求总量计数器 */
  private final Counter totalRequests;

  /** 请求成功总量计数器 */
  private final Counter totalSucceededRequests;

  /** 请求失败总量计数器 */
  private final Counter totalFailedRequests;

  /** 方法耗时 */
  private final Timer timer;

  /**
   * 构造方法
   *
   * @param serviceName 服务名
   * @param duuidGenerator id生成器
   * @param meterRegistry prometheus
   */
  public IdGeneratorHandler(
      @NonNull String serviceName,
      @NonNull DuuidGenerator duuidGenerator,
      @NonNull MeterRegistry meterRegistry) {
    this.serviceName = serviceName;
    this.duuidGenerator = duuidGenerator;
    this.totalRequests = meterRegistry.counter("requests_total", "interface", "nextId");
    this.totalSucceededRequests =
        meterRegistry.counter("requests_succeeded_total", "interface", "nextId");
    this.totalFailedRequests =
        meterRegistry.counter("requests_failed_total", "interface", "nextId");
    this.timer = meterRegistry.timer("requests_duration", "interface", "nextId");
  }

  /**
   * 根据请求生成uuid
   *
   * @param request 请求
   * @return 响应
   */
  @Override
  public Mono<ServerResponse> handle(ServerRequest request) {
    return timer.record(
        () ->
            ServerResponse.ok()
                .contentType(APPLICATION_JSON)
                .body(BodyInserters.fromValue(new Duuid(duuidGenerator.nextId())))
                .doOnNext(serverResponse -> totalRequests.increment())
                .doOnSuccess(serverResponse -> totalSucceededRequests.increment())
                .onErrorResume(
                    t -> {
                      log.error("Failed to generate id.", t);
                      totalFailedRequests.increment();
                      return ServerResponse.status(INTERNAL_SERVER_ERROR)
                          .contentType(APPLICATION_JSON)
                          .body(
                              BodyInserters.fromValue(SERVICE_INTERNAL_ERROR.format(serviceName)));
                    }));
  }
}
