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

package com.silong.foundation.springboot.starter.jwt;

import static com.silong.foundation.springboot.starter.jwt.TestUser.SAM;
import static com.silong.foundation.springboot.starter.jwt.TestUser.TOM;
import static com.silong.foundation.springboot.starter.jwt.common.Constants.ACCESS_TOKEN;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.util.StringUtils.hasLength;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.silong.foundation.springboot.starter.jwt.TestService.User;
import com.silong.foundation.springboot.starter.jwt.common.Credentials;
import com.silong.foundation.springboot.starter.jwt.common.TokenBody;
import com.silong.foundation.springboot.starter.jwt.provider.DefaultUserAuthenticationProvider;
import com.silong.foundation.springboot.starter.jwt.provider.UserAuthenticationProvider;
import com.silong.foundation.webclient.reactive.WebClients;
import com.silong.foundation.webclient.reactive.config.WebClientConfig;
import com.silong.foundation.webclient.reactive.config.WebClientSslConfig;
import java.time.Duration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.BodyInserters;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * 测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-19 21:36
 */
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Testcontainers
@SpringBootTest(classes = JwtAuthTestApp.class, webEnvironment = RANDOM_PORT)
@TestPropertySource(locations = "classpath:application.properties")
@ExtendWith(SpringExtension.class)
public class JwtAuthTests {

  // Instantiate a generator with a factory method.
  static UniformRandomProvider RNG = RandomSource.XO_RO_SHI_RO_128_PP.create();

  static final Faker FAKER = new Faker();

  @Container
  public static GenericContainer<?> REDIS_CONTAINER =
      new GenericContainer<>(DockerImageName.parse("redis:6.2.6"))
          .withExposedPorts(6379)
          .waitingFor(new HostPortWaitStrategy().forPorts(6379))
          .withStartupTimeout(Duration.ofMinutes(1));

  public final WebClientConfig WEB_CLIENT_CONFIG = new WebClientConfig();

  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Autowired private UserAuthenticationProvider userAuthenticationProvider;

  @LocalServerPort private int port;

  private static final WebClientSslConfig CLIENT_SSL_CONFIG_ONE_WAY =
      new WebClientSslConfig().trustAll(true).protocols(new String[] {"TLSv1.2"});

  @Value("${jwt-auth.auth-path}")
  private String authPath;

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
    registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379).toString());
  }

  @AfterAll
  static void afterAll() {
    REDIS_CONTAINER.close();
  }

  @BeforeEach
  void init() {
    DefaultUserAuthenticationProvider provider =
        (DefaultUserAuthenticationProvider) userAuthenticationProvider;
    provider.addUser(SAM);
    provider.addUser(TOM);
    WEB_CLIENT_CONFIG.baseUrl("https://127.0.0.1:" + port);
  }

  @Test
  @Order(1)
  void testRedisConnected() {
    assertTrue(REDIS_CONTAINER.isRunning());
  }

  @Test
  @Order(2)
  void testLogin() {
    Credentials credentials = new Credentials();
    credentials.setPassword("mypassword");
    credentials.setUserName("sam");
    Mono<TokenBody> resultMono =
        WebClients.create(WEB_CLIENT_CONFIG, CLIENT_SSL_CONFIG_ONE_WAY, OBJECT_MAPPER)
            .post()
            .uri(authPath)
            .body(BodyInserters.fromValue(credentials))
            .accept(APPLICATION_JSON)
            .retrieve()
            .onStatus(
                httpStatus -> !httpStatus.is2xxSuccessful(),
                resp -> Mono.just(new AuthenticatedException("error", resp.statusCode().value())))
            .bodyToMono(TokenBody.class);

    StepVerifier.create(resultMono)
        .expectNextMatches(tokenBody -> hasLength(tokenBody.getToken()))
        .verifyComplete();
  }

  @Test
  @Order(3)
  void testGet() {
    long userId = RNG.nextLong();
    Mono<User> userMono =
        WebClients.create(WEB_CLIENT_CONFIG, CLIENT_SSL_CONFIG_ONE_WAY, OBJECT_MAPPER)
            .get()
            .uri("/guest/a/" + userId)
            .accept(APPLICATION_JSON)
            .retrieve()
            .onStatus(
                httpStatus -> !httpStatus.is2xxSuccessful(),
                resp -> Mono.just(new AuthenticatedException("error", resp.statusCode().value())))
            .bodyToMono(User.class);
    StepVerifier.create(userMono).expectNextMatches(user -> user.id() == userId).verifyComplete();
  }

  @Test
  @Order(4)
  void testPost() {
    Credentials credentials = new Credentials();
    credentials.setPassword("password123");
    credentials.setUserName("tom");
    long userId = RNG.nextLong();
    Mono<Long> idMono =
        WebClients.create(WEB_CLIENT_CONFIG, CLIENT_SSL_CONFIG_ONE_WAY, OBJECT_MAPPER)
            .post()
            .uri(authPath)
            .body(BodyInserters.fromValue(credentials))
            .accept(APPLICATION_JSON)
            .retrieve()
            .onStatus(
                httpStatus1 -> !httpStatus1.is2xxSuccessful(),
                resp1 -> Mono.just(new AuthenticatedException("error", resp1.statusCode().value())))
            .bodyToMono(TokenBody.class)
            .flatMap(
                tokenBody ->
                    WebClients.create(WEB_CLIENT_CONFIG, CLIENT_SSL_CONFIG_ONE_WAY, OBJECT_MAPPER)
                        .post()
                        .uri("/guest/c")
                        .body(BodyInserters.fromValue(new User(userId, FAKER.animal().name())))
                        .header(ACCESS_TOKEN, tokenBody.getToken())
                        .accept(APPLICATION_JSON)
                        .retrieve()
                        .onStatus(
                            httpStatus -> !httpStatus.is2xxSuccessful(),
                            resp ->
                                Mono.just(
                                    new AuthenticatedException("error", resp.statusCode().value())))
                        .bodyToMono(Long.class));

    StepVerifier.create(idMono).expectNextMatches(id -> id == userId).verifyComplete();
  }

  @Test
  @Order(5)
  void testPut() {
    Credentials credentials = new Credentials();
    credentials.setPassword("password123");
    credentials.setUserName("tom");
    long userId = RNG.nextLong();
    Mono<Long> idMono =
        WebClients.create(WEB_CLIENT_CONFIG, CLIENT_SSL_CONFIG_ONE_WAY, OBJECT_MAPPER)
            .post()
            .uri(authPath)
            .body(BodyInserters.fromValue(credentials))
            .accept(APPLICATION_JSON)
            .retrieve()
            .onStatus(
                httpStatus1 -> !httpStatus1.is2xxSuccessful(),
                resp1 -> Mono.just(new AuthenticatedException("error", resp1.statusCode().value())))
            .bodyToMono(TokenBody.class)
            .flatMap(
                tokenBody ->
                    WebClients.create(WEB_CLIENT_CONFIG, CLIENT_SSL_CONFIG_ONE_WAY, OBJECT_MAPPER)
                        .put()
                        .uri("/guest/d")
                        .body(BodyInserters.fromValue(new User(userId, FAKER.animal().name())))
                        .header(ACCESS_TOKEN, tokenBody.getToken())
                        .accept(APPLICATION_JSON)
                        .retrieve()
                        .onStatus(
                            httpStatus -> !httpStatus.is2xxSuccessful(),
                            resp ->
                                Mono.just(
                                    new AuthenticatedException("error", resp.statusCode().value())))
                        .bodyToMono(Long.class));

    StepVerifier.create(idMono).expectNextMatches(id -> id == userId).verifyComplete();
  }

  @Test
  @Order(6)
  void testDelete() {
    Credentials credentials = new Credentials();
    credentials.setPassword("password123");
    credentials.setUserName("tom");
    long userId = RNG.nextLong();
    Mono<Long> idMono =
        WebClients.create(WEB_CLIENT_CONFIG, CLIENT_SSL_CONFIG_ONE_WAY, OBJECT_MAPPER)
            .post()
            .uri(authPath)
            .body(BodyInserters.fromValue(credentials))
            .accept(APPLICATION_JSON)
            .retrieve()
            .onStatus(
                httpStatus1 -> !httpStatus1.is2xxSuccessful(),
                resp1 -> Mono.just(new AuthenticatedException("error", resp1.statusCode().value())))
            .bodyToMono(TokenBody.class)
            .flatMap(
                tokenBody ->
                    WebClients.create(WEB_CLIENT_CONFIG, CLIENT_SSL_CONFIG_ONE_WAY, OBJECT_MAPPER)
                        .delete()
                        .uri("/guest/b/" + userId)
                        .header(ACCESS_TOKEN, tokenBody.getToken())
                        .accept(APPLICATION_JSON)
                        .retrieve()
                        .onStatus(
                            httpStatus -> !httpStatus.is2xxSuccessful(),
                            resp ->
                                Mono.just(
                                    new AuthenticatedException("error", resp.statusCode().value())))
                        .bodyToMono(Long.class));

    StepVerifier.create(idMono).expectNextMatches(id -> id == userId).verifyComplete();
  }

  @Getter
  private static class AuthenticatedException extends Exception {

    private final int httpStatusCode;

    public AuthenticatedException(String message, int httpStatusCode) {
      super(message);
      this.httpStatusCode = httpStatusCode;
    }
  }
}
