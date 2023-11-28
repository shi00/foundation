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
import com.silong.foundation.crypto.digest.HmacToolkit;
import com.silong.foundation.webclient.reactive.WebClients;
import com.silong.foundation.webclient.reactive.config.WebClientConfig;
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
    Mono<TestService.User> resultMono =
        WebClients.create(WEB_CLIENT_CONFIG, OBJECT_MAPPER)
            .get()
            .uri(getBaseurl() + "/a/{id}", userId)
            .accept(APPLICATION_JSON)
            .retrieve()
            .onStatus(
                httpStatus -> !httpStatus.is2xxSuccessful(),
                resp -> Mono.just(new Exception("error")))
            .bodyToMono(TestService.User.class);
    StepVerifier.create(resultMono)
        .expectNext(new TestService.User(Long.parseLong(userId), "random"))
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
            .body(BodyInserters.fromValue(new TestService.User(userId, name)))
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
            .body(BodyInserters.fromValue(new TestService.User(userId, name)))
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

  private String getBaseurl() {
    return String.format(BASEURL_FORMAT, port);
  }
}
