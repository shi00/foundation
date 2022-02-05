package com.silong.foundation.webclient.reactive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import com.silong.foundation.webclient.reactive.config.WebClientConfig;
import lombok.*;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.AfterAll;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;

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

  static final ObjectMapper MAPPER = new ObjectMapper();

  static final Faker FAKER = new Faker();

  static MockWebServer mockWebServer;

  static WebClient webClient;

  static String baseUrl;

  @AfterAll
  static void cleanUp() throws IOException {
    if (mockWebServer != null) {
      mockWebServer.shutdown();
    }
  }

  static void getTest() throws JsonProcessingException {
    Result expected = Result.builder().code("0").msg("test-result").build();
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(HttpStatus.OK.value())
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(MAPPER.writeValueAsString(expected)));

    WebClientConfig webClientConfig = new WebClientConfig().baseUrl(baseUrl);

    Mono<Result> resultMono =
        WebClients.create(webClientConfig, MAPPER)
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

  static void deleteTest() {
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

  @SneakyThrows
  static void postTest() {
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

  @SneakyThrows
  static void patchTest() {
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

  static void headTest() {
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

  static void putTest() {
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
