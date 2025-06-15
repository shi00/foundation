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

import static com.silong.foundation.springboot.starter.jwt.common.Constants.*;
import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.REACTIVE;
import static org.springframework.http.MediaType.*;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

import com.silong.foundation.springboot.starter.jwt.common.ErrorDetail;
import com.silong.foundation.springboot.starter.jwt.common.TokenBody;
import com.silong.foundation.springboot.starter.jwt.configure.config.JWTAuthProperties;
import com.silong.foundation.springboot.starter.jwt.handler.AuthTokenHandler;
import com.silong.foundation.springboot.starter.jwt.provider.JWTProvider;
import com.silong.foundation.springboot.starter.jwt.provider.UserAuthenticationProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.Map;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * 路由自动装配
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-05-08 10:51
 */
@Configuration
@EnableWebFlux
@AutoConfigureBefore(WebFluxAutoConfiguration.class)
@EnableConfigurationProperties({JWTAuthProperties.class})
@ConditionalOnProperty(prefix = "jwt-auth", value = "auth-path")
@ConditionalOnWebApplication(type = REACTIVE)
public class RoutesAutoConfiguration {

  private JWTAuthProperties jwtAuthProperties;

  @Bean
  AuthTokenHandler registerAuthTokenHandler(
      @Value("${spring.application.name}") String appName,
      UserAuthenticationProvider userAuthenticationProvider,
      JWTProvider jwtProvider,
      Map<String, String> tokenCache) {
    return new AuthTokenHandler(appName, userAuthenticationProvider, jwtProvider, tokenCache);
  }

  @Bean
  @RouterOperations(
      @RouterOperation(
          produces = {APPLICATION_JSON_VALUE},
          consumes = {APPLICATION_JSON_VALUE},
          method = POST,
          beanClass = AuthTokenHandler.class,
          beanMethod = "handle",
          operation =
              @Operation(
                  operationId = "authenticate",
                  summary = "User authentication generates an access token",
                  tags = {"authenticate"},
                  security = {@SecurityRequirement(name = ACCESS_TOKEN)},
                  responses = {
                    @ApiResponse(
                        responseCode = "200",
                        description = "OK",
                        content = @Content(schema = @Schema(implementation = TokenBody.class))),
                    @ApiResponse(
                        responseCode = "401",
                        description = "UNAUTHORIZED",
                        content = @Content(schema = @Schema(implementation = ErrorDetail.class))),
                    @ApiResponse(
                        responseCode = "403",
                        description = "FORBIDDEN",
                        content = @Content(schema = @Schema(implementation = ErrorDetail.class)))
                  })))
  RouterFunction<ServerResponse> routes(AuthTokenHandler handler) {
    return RouterFunctions.route(
        RequestPredicates.POST(jwtAuthProperties.getAuthPath()).and(accept(APPLICATION_JSON)),
        handler);
  }

  @Autowired
  public void setJwtAuthProperties(JWTAuthProperties jwtAuthProperties) {
    this.jwtAuthProperties = jwtAuthProperties;
  }
}
