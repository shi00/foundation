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

import com.silong.foundation.duuid.server.DuuidServerApplication;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * OpenApi自动装配
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-14 11:13
 */
@Configuration
public class OpenApiAutoConfiguration {

  private static final String ANY_IPV4_ADDRESS = "0.0.0.0";

  @Bean
  CorsConfigurationSource corsConfiguration() {
    CorsConfiguration corsConfig = new CorsConfiguration();
    corsConfig.applyPermitDefaultValues();
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", corsConfig);
    return source;
  }

  @Bean
  GroupedOpenApi groupedOpenApi(
      @Value("${server.ssl.enabled}") boolean sslEnabled,
      @Value("${server.address}") String listenAddr,
      @Value("${server.port}") int port,
      @Value("${duuid.server.service-path}") String path,
      @Value("${spring.application.name}") String name) {
    return GroupedOpenApi.builder()
        .packagesToScan(DuuidServerApplication.class.getPackage().getName())
        .addOpenApiCustomiser(openApi -> customServers(sslEnabled, listenAddr, port, name, openApi))
        .group(name)
        .pathsToMatch(path)
        .build();
  }

  private void customServers(
      boolean sslEnabled, String listenAddr, int port, String name, OpenAPI openApi) {
    List<Server> servers = openApi.getServers();
    if (servers != null) {
      servers.clear();
    }
    openApi.addServersItem(
        new Server()
            .description(name)
            .url(
                String.format(
                    "%s://%s:%d",
                    sslEnabled ? "https" : "http",
                    ANY_IPV4_ADDRESS.equals(listenAddr) ? "localhost" : listenAddr,
                    port)));
  }
}
