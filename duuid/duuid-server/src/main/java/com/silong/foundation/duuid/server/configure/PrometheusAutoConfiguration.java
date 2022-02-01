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

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.apache.commons.lang3.SystemUtils.getHostName;

/**
 * prometheus 自动装配
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-15 14:52
 */
@Configuration
public class PrometheusAutoConfiguration {
  @Bean
  MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(
      @Value("${spring.application.name}") String applicationName,
      @Value("${duuid.server.region}") String region,
      @Value("${duuid.server.data-center}") String dataCenter) {
    return registry ->
        registry
            .config()
            .commonTags(
                "service",
                applicationName,
                "host",
                getHostName(),
                "region",
                region,
                "data-center",
                dataCenter)
            .meterFilter(
                new MeterFilter() {
                  @Override
                  public DistributionStatisticConfig configure(
                      Meter.Id id, DistributionStatisticConfig config) {
                    return id.getType() == Meter.Type.TIMER
                        ? DistributionStatisticConfig.builder()
                            .percentilesHistogram(true)
                            .percentiles(0.50, 0.75, 0.90, 0.95, 0.99)
                            .serviceLevelObjectives(
                                Duration.ofMillis(1).toNanos(),
                                Duration.ofMillis(2).toNanos(),
                                Duration.ofMillis(3).toNanos(),
                                Duration.ofMillis(4).toNanos(),
                                Duration.ofMillis(5).toNanos(),
                                Duration.ofMillis(6).toNanos(),
                                Duration.ofMillis(7).toNanos(),
                                Duration.ofMillis(8).toNanos(),
                                Duration.ofMillis(9).toNanos(),
                                Duration.ofMillis(10).toNanos(),
                                Duration.ofMillis(15).toNanos(),
                                Duration.ofMillis(20).toNanos())
                            .minimumExpectedValue(Duration.ofMillis(1).toNanos())
                            .maximumExpectedValue(Duration.ofMillis(20).toNanos())
                            .build()
                            .merge(config)
                        : config;
                  }
                });
  }
}
