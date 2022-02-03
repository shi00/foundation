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
package com.silong.foundation.webclient.reactive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import com.silong.foundation.webclient.reactive.config.WebClientConfig;
import com.silong.foundation.webclient.reactive.config.WebClientSslConfig;
import okhttp3.Protocol;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.List;

import static org.springframework.util.SocketUtils.*;

/**
 * 测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-02-02 13:41
 */
public class WebClientHttpsTests {

  static final ObjectMapper MAPPER = new ObjectMapper();

  static final Faker FAKER = new Faker();

  private static final String PASSWORD = "password";

  private static final String CLIENT_KEYSTORE_PATH = "src/test/resources/clientkeystore.p12";

  private static final String CLIENT_TRUST_KEYSTORE_PATH =
      "src/test/resources/clienttruststore.jks";

  private static final WebClientSslConfig CLIENT_SSL_CONFIG_TREUST_ALL =
      new WebClientSslConfig()
          .trustAll(true)
          .keyStoreType("PKCS12")
          .keyStorePassword(PASSWORD)
          .keyStorePath(CLIENT_KEYSTORE_PATH);

  private static final WebClientSslConfig CLIENT_SSL_CONFIG =
      new WebClientSslConfig()
          .trustAll(false)
          .trustStoreType("JKS")
          .trustStorePassword(PASSWORD)
          .trustStorePath(CLIENT_TRUST_KEYSTORE_PATH)
          .keyStoreType("PKCS12")
          .keyStorePassword(PASSWORD)
          .keyStorePath(CLIENT_KEYSTORE_PATH);

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private MockWebServer mockWebServer;

  @BeforeEach
  void setup() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.requestClientAuth();
    mockWebServer.setProtocolNegotiationEnabled(true);
    mockWebServer.setProtocols(List.of(Protocol.HTTP_1_1, Protocol.HTTP_2));
    mockWebServer.useHttps(buildTestSslContext().getSocketFactory(), false);
    mockWebServer.start(findAvailableTcpPort(PORT_RANGE_MIN, PORT_RANGE_MAX));
  }

  private SSLContext buildTestSslContext() throws Exception {
    // Load self-signed certificate
    char[] serverKeyStorePassword = PASSWORD.toCharArray();
    KeyStore serverKeyStore = KeyStore.getInstance("PKCS12");
    try (InputStream in = WebClientHttpsTests.class.getResourceAsStream("serverkeystore.p12")) {
      serverKeyStore.load(in, serverKeyStorePassword);
    }

    String kmfAlgorithm = KeyManagerFactory.getDefaultAlgorithm();
    KeyManagerFactory kmf = KeyManagerFactory.getInstance(kmfAlgorithm);
    kmf.init(serverKeyStore, serverKeyStorePassword);

    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(kmfAlgorithm);
    trustManagerFactory.init(serverKeyStore);
    SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
    sslContext.init(kmf.getKeyManagers(), trustManagerFactory.getTrustManagers(), SECURE_RANDOM);
    return sslContext;
  }

  @AfterEach
  void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  @Test
  @DisplayName("https-GET")
  void test1() throws IOException {
    String baseUrl = String.format("https://localhost:%s", mockWebServer.getPort());
    Result expected = Result.builder().code("0").msg("test-result").build();
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(HttpStatus.OK.value())
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(MAPPER.writeValueAsString(expected)));

    WebClientConfig webClientConfig = new WebClientConfig().baseUrl(baseUrl);

    Mono<Result> resultMono =
        WebClients.create(webClientConfig, CLIENT_SSL_CONFIG, MAPPER)
            .get()
            .uri("/test/{param}", "a")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus(
                httpStatus -> httpStatus.is4xxClientError() || httpStatus.is5xxServerError(),
                resp -> Mono.just(new Exception("error")))
            .bodyToMono(Result.class);
    StepVerifier.create(resultMono).expectNext(expected).verifyComplete();
  }

  @Test
  @DisplayName("http-DELETE")
  void test2() {
    String baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(HttpStatus.OK.value())
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    WebClientConfig webClientConfig = new WebClientConfig().baseUrl(baseUrl);

    Mono<Void> voidMono =
        WebClients.create(webClientConfig, MAPPER)
            .delete()
            .uri("/test/{id}", "1")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus(
                httpStatus -> httpStatus.is4xxClientError() || httpStatus.is5xxServerError(),
                resp -> Mono.just(new Exception("error")))
            .bodyToMono(Void.class);
    StepVerifier.create(voidMono).expectNext().verifyComplete();
  }

  @Test
  @DisplayName("http-POST")
  void test3() throws JsonProcessingException {
    String baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());
    Result expected = Result.builder().code("0").msg("successful").build();
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(HttpStatus.OK.value())
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(MAPPER.writeValueAsString(expected)));

    WebClientConfig webClientConfig = new WebClientConfig().baseUrl(baseUrl);

    Mono<Result> resultMono =
        WebClients.create(webClientConfig, MAPPER)
            .post()
            .uri("/test/{param}", "a")
            .body(BodyInserters.fromValue(randomEmployee()))
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus(
                httpStatus -> httpStatus.is4xxClientError() || httpStatus.is5xxServerError(),
                resp -> Mono.just(new Exception("error")))
            .bodyToMono(Result.class);
    StepVerifier.create(resultMono).expectNext(expected).verifyComplete();
  }

  @Test
  @DisplayName("http-PUT")
  void test4() {
    String baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(HttpStatus.OK.value())
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    WebClientConfig webClientConfig = new WebClientConfig().baseUrl(baseUrl);

    Mono<Void> voidMono =
        WebClients.create(webClientConfig, MAPPER)
            .put()
            .uri("/test/{param}", "a")
            .body(BodyInserters.fromValue(randomEmployee()))
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus(
                httpStatus -> httpStatus.is4xxClientError() || httpStatus.is5xxServerError(),
                resp -> Mono.just(new Exception("error")))
            .bodyToMono(Void.class);
    StepVerifier.create(voidMono).expectNext().verifyComplete();
  }

  @Test
  @DisplayName("http-HEAD")
  void test5() {
    String baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(HttpStatus.OK.value())
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    WebClientConfig webClientConfig = new WebClientConfig().baseUrl(baseUrl);

    Mono<Void> voidMono =
        WebClients.create(webClientConfig, MAPPER)
            .head()
            .uri("/test/{id}", "1")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus(
                httpStatus -> httpStatus.is4xxClientError() || httpStatus.is5xxServerError(),
                resp -> Mono.just(new Exception("error")))
            .bodyToMono(Void.class);
    StepVerifier.create(voidMono).expectNext().verifyComplete();
  }

  @Test
  @DisplayName("http-PATCH")
  void test6() throws JsonProcessingException {
    String baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());
    Employee employee = randomEmployee();
    mockWebServer.enqueue(
        new MockResponse()
            .setBody(MAPPER.writeValueAsString(employee))
            .setResponseCode(HttpStatus.OK.value())
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    WebClientConfig webClientConfig = new WebClientConfig().baseUrl(baseUrl);

    Mono<Employee> employeeMono =
        WebClients.create(webClientConfig, MAPPER)
            .patch()
            .uri("/test/{id}", "1")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus(
                httpStatus -> httpStatus.is4xxClientError() || httpStatus.is5xxServerError(),
                resp -> Mono.just(new Exception("error")))
            .bodyToMono(Employee.class);
    StepVerifier.create(employeeMono).expectNext(employee).verifyComplete();
  }

  private Employee randomEmployee() {
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
