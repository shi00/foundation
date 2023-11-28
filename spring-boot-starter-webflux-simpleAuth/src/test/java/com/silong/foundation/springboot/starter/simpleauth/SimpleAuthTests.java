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

package com.silong.foundation.springboot.starter.simpleauth;

import static com.silong.foundation.springboot.starter.simpleauth.constants.AuthHeaders.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.silong.foundation.common.constants.HttpStatusCode;
import com.silong.foundation.crypto.digest.HmacToolkit;
import com.silong.foundation.springboot.starter.simpleauth.TestService.User;
import com.silong.foundation.webclient.reactive.WebClients;
import com.silong.foundation.webclient.reactive.config.WebClientConfig;
import java.time.Duration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.BodyInserters;
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
@SpringBootTest(classes = SimpleAuthTestApp.class, webEnvironment = RANDOM_PORT)
@TestPropertySource(locations = "classpath:application.properties")
@ExtendWith(SpringExtension.class)
public class SimpleAuthTests {

  public static final String BASEURL_FORMAT = "http://127.0.0.1:%d/guest";

  public final WebClientConfig WEB_CLIENT_CONFIG = new WebClientConfig();

  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Getter
  private static class AuthenticatedException extends Exception {

    private final int httpStatusCode;

    public AuthenticatedException(String message, int httpStatusCode) {
      super(message);
      this.httpStatusCode = httpStatusCode;
    }
  }

  static final Faker FAKER = new Faker();

  @LocalServerPort private int port;

  @Value("${simple-auth.work-key}")
  private String workKey;

  private final String id = "guest";

  @Test
  void test1() {
    String timestamp = Long.toString(System.currentTimeMillis());
    String random = Integer.toString(RandomUtils.nextInt());
    String signature = HmacToolkit.hmacSha256(id + timestamp + random, workKey);
    String userId = "119988";
    Mono<Long> resultMono =
        WebClients.create(WEB_CLIENT_CONFIG, OBJECT_MAPPER)
            .delete()
            .uri(getBaseurl() + "/b/{id}", userId)
            .accept(APPLICATION_JSON)
            .header(IDENTITY, id)
            .header(TIMESTAMP, timestamp)
            .header(RANDOM, random)
            .header(SIGNATURE, signature)
            .retrieve()
            .onStatus(
                httpStatus -> !httpStatus.is2xxSuccessful(),
                resp -> Mono.just(new Exception("error")))
            .bodyToMono(Long.class);
    StepVerifier.create(resultMono).expectNext(Long.parseLong(userId)).verifyComplete();
  }

  @Test
  void test2() {
    String userId = "119988";
    Mono<User> resultMono =
        WebClients.create(WEB_CLIENT_CONFIG, OBJECT_MAPPER)
            .get()
            .uri(getBaseurl() + "/a/{id}", userId)
            .accept(APPLICATION_JSON)
            .retrieve()
            .onStatus(
                httpStatus -> !httpStatus.is2xxSuccessful(),
                resp -> Mono.just(new Exception("error")))
            .bodyToMono(User.class);
    StepVerifier.create(resultMono)
        .expectNext(new User(Long.parseLong(userId), "random"))
        .verifyComplete();
  }

  @Test
  void test3() {
    String timestamp = Long.toString(System.currentTimeMillis());
    String random = Integer.toString(RandomUtils.nextInt());
    String signature = HmacToolkit.hmacSha256(id + timestamp + random, workKey);
    long userId = 119988;
    String name = FAKER.funnyName().name();
    Mono<Long> resultMono =
        WebClients.create(WEB_CLIENT_CONFIG, OBJECT_MAPPER)
            .post()
            .uri(getBaseurl() + "/c")
            .body(BodyInserters.fromValue(new User(userId, name)))
            .accept(APPLICATION_JSON)
            .header(IDENTITY, id)
            .header(TIMESTAMP, timestamp)
            .header(RANDOM, random)
            .header(SIGNATURE, signature)
            .retrieve()
            .onStatus(
                httpStatus -> !httpStatus.is2xxSuccessful(),
                resp -> Mono.just(new Exception("error")))
            .bodyToMono(Long.class);
    StepVerifier.create(resultMono).expectNext(userId).verifyComplete();
  }

  @Test
  void test4() {
    String timestamp = Long.toString(System.currentTimeMillis());
    String random = Integer.toString(RandomUtils.nextInt());
    String signature = HmacToolkit.hmacSha256(id + timestamp + random, workKey);
    long userId = 119988;
    String name = FAKER.funnyName().name();
    Mono<Long> resultMono =
        WebClients.create(WEB_CLIENT_CONFIG, OBJECT_MAPPER)
            .put()
            .uri(getBaseurl() + "/d")
            .body(BodyInserters.fromValue(new User(userId, name)))
            .accept(APPLICATION_JSON)
            .header(IDENTITY, id)
            .header(TIMESTAMP, timestamp)
            .header(RANDOM, random)
            .header(SIGNATURE, signature)
            .retrieve()
            .onStatus(
                httpStatus -> !httpStatus.is2xxSuccessful(),
                resp -> Mono.just(new Exception("error")))
            .bodyToMono(Long.class);
    StepVerifier.create(resultMono).expectNext(userId).verifyComplete();
  }

  @Test
  void test5() {
    String userId = "119988";
    Mono<User> resultMono =
        WebClients.create(WEB_CLIENT_CONFIG, OBJECT_MAPPER)
            .get()
            .uri(getBaseurl() + "/e/{id}", userId)
            .accept(APPLICATION_JSON)
            .retrieve()
            .onStatus(
                httpStatusCode -> !httpStatusCode.is2xxSuccessful(),
                clientResponse ->
                    Mono.just(
                        new AuthenticatedException("error", clientResponse.statusCode().value())))
            .bodyToMono(User.class);
    StepVerifier.create(resultMono)
        .expectErrorMatches(
            e ->
                e instanceof AuthenticatedException ee
                    && ee.httpStatusCode == Integer.parseInt(HttpStatusCode.FORBIDDEN))
        .verify(Duration.ofSeconds(10));
  }

  @Test
  void test6() {
    String timestamp = Long.toString(System.currentTimeMillis());
    String random = Integer.toString(RandomUtils.nextInt());
    String signature = "abc";
    long userId = 119988;
    String name = FAKER.funnyName().name();
    Mono<Long> resultMono =
        WebClients.create(WEB_CLIENT_CONFIG, OBJECT_MAPPER)
            .put()
            .uri(getBaseurl() + "/d")
            .body(BodyInserters.fromValue(new User(userId, name)))
            .accept(APPLICATION_JSON)
            .header(IDENTITY, id)
            .header(TIMESTAMP, timestamp)
            .header(RANDOM, random)
            .header(SIGNATURE, signature)
            .retrieve()
            .onStatus(
                httpStatusCode -> !httpStatusCode.is2xxSuccessful(),
                clientResponse ->
                    Mono.just(
                        new AuthenticatedException("error", clientResponse.statusCode().value())))
            .bodyToMono(Long.class);
    StepVerifier.create(resultMono)
        .expectErrorMatches(
            e ->
                e instanceof AuthenticatedException ee
                    && ee.httpStatusCode == Integer.parseInt(HttpStatusCode.UNAUTHORIZED))
        .verify(Duration.ofSeconds(10));
  }

  private String getBaseurl() {
    return String.format(BASEURL_FORMAT, port);
  }
}
