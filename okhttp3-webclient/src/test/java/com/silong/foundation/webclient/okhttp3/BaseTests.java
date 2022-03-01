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
package com.silong.foundation.webclient.okhttp3;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import lombok.*;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.security.KeyStore;

import static com.silong.foundation.springboot.starter.okhttp3.Constants.APPLICATION_JSON_VALUE;
import static com.silong.foundation.springboot.starter.okhttp3.Constants.CONTENT_TYPE;

/**
 * 测试基类
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-02-03 15:01
 */
public abstract class BaseTests {

  /**
   * 测试对象
   *
   * @author louis sin
   * @version 1.0.0
   * @since 2022-02-03 09:42
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Employee {
    private String name;
    private int age;
    private int salary;
    private int level;
    private String company;
    private String gender;
  }

  /**
   * 测试对象
   *
   * @author louis sin
   * @version 1.0.0
   * @since 2022-02-03 09:42
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Result {
    private String code;
    private String msg;
  }

  static final String TLSV_1_3 = "TLSv1.3";

  static final String TLSV_1_2 = "TLSv1.2";

  static final String PASSWORD = "NXUdkZEJDJ53K";

  static final String PKCS_12 = KeyStore.getDefaultType();

  static final ObjectMapper MAPPER = new ObjectMapper();

  static final Faker FAKER = new Faker();

  static MockWebServer mockWebServer;

  static OkHttpClient webClient;

  static String baseUrl;

  @AfterAll
  static void cleanUp() throws IOException {
    if (mockWebServer != null) {
      mockWebServer.shutdown();
    }
  }

  static void getTest() throws Exception {
    Result expected = Result.builder().code("0").msg("test-result").build();
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .setBody(MAPPER.writeValueAsString(expected)));

    try (Response response =
        webClient
            .newCall(
                new Request.Builder()
                    .url(baseUrl + "/test/a")
                    .get()
                    .addHeader("Accept", APPLICATION_JSON_VALUE)
                    .build())
            .execute()) {

      Result result =
          MAPPER.readValue(response.isSuccessful() ? response.body().string() : null, Result.class);
      Assertions.assertEquals(expected, result);
    }
  }

  static void deleteTest() throws IOException {
    mockWebServer.enqueue(
        new MockResponse().setResponseCode(200).setHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE));

    try (Response response =
        webClient
            .newCall(
                new Request.Builder()
                    .url(baseUrl + "/test/1")
                    .delete()
                    .addHeader("Accept", APPLICATION_JSON_VALUE)
                    .build())
            .execute()) {
      Assertions.assertEquals(200, response.code());
    }
  }

  @SneakyThrows
  static void postTest() {
    Result expected = Result.builder().code("0").msg("successful").build();
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(HttpStatus.OK.value())
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(MAPPER.writeValueAsString(expected)));

    Mono<Result> resultMono =
        webClient
            .post()
            .uri(baseUrl + "/test/{param}", "a")
            .body(BodyInserters.fromValue(randomEmployee()))
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus(
                httpStatus -> httpStatus.is4xxClientError() || httpStatus.is5xxServerError(),
                resp -> Mono.just(new Exception("error")))
            .bodyToMono(Result.class);
    StepVerifier.create(resultMono).expectNext(expected).verifyComplete();
  }

  @SneakyThrows
  static void patchTest() {
    Employee employee = randomEmployee();
    mockWebServer.enqueue(
        new MockResponse()
            .setBody(MAPPER.writeValueAsString(employee))
            .setResponseCode(HttpStatus.OK.value())
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    Mono<Employee> employeeMono =
        webClient
            .patch()
            .uri(baseUrl + "/test/{id}", "1")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus(
                httpStatus -> httpStatus.is4xxClientError() || httpStatus.is5xxServerError(),
                resp -> Mono.just(new Exception("error")))
            .bodyToMono(Employee.class);
    StepVerifier.create(employeeMono).expectNext(employee).verifyComplete();
  }

  static void headTest() {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(HttpStatus.OK.value())
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    Mono<Void> voidMono =
        webClient
            .head()
            .uri(baseUrl + "/test/{id}", "1")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus(
                httpStatus -> httpStatus.is4xxClientError() || httpStatus.is5xxServerError(),
                resp -> Mono.just(new Exception("error")))
            .bodyToMono(Void.class);
    StepVerifier.create(voidMono).expectNext().verifyComplete();
  }

  static void putTest() {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(HttpStatus.OK.value())
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    Mono<Void> voidMono =
        webClient
            .put()
            .uri(baseUrl + "/test/{param}", "a")
            .body(BodyInserters.fromValue(randomEmployee()))
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus(
                httpStatus -> httpStatus.is4xxClientError() || httpStatus.is5xxServerError(),
                resp -> Mono.just(new Exception("error")))
            .bodyToMono(Void.class);
    StepVerifier.create(voidMono).expectNext().verifyComplete();
  }

  static Employee randomEmployee() {
    return Employee.builder()
        .level(RandomUtils.nextInt(0, 100))
        .salary(RandomUtils.nextInt(0, 100))
        .age(RandomUtils.nextInt(0, 100))
        .gender(RandomUtils.nextInt(0, 1000) % 2 == 1 ? "male" : "female")
        .name(FAKER.name().username())
        .company(FAKER.company().name())
        .build();
  }
}
