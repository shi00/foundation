package com.silong.foundation.duuid.server;

import com.silong.foundation.crypto.digest.HmacToolkit;
import com.silong.foundation.duuid.server.model.Duuid;
import com.silong.foundation.springboot.starter.simpleauth.configure.properties.SimpleAuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * 服务集成测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-11 20:49
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = DuuidServerApplication.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class DuuidServerApplicationTests {

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate restTemplate;

  @Autowired private SimpleAuthProperties properties;

  private String idGenEngdpoint;

  private String prometheusEngdpoint;

  private String openApiEngdpoint;

  private HttpHeaders headers;

  @BeforeEach
  void init() {
    idGenEngdpoint = String.format("http://localhost:%d/duuid", port);
    prometheusEngdpoint = String.format("http://localhost:%d/actuator/prometheus", port + 1);
    openApiEngdpoint = String.format("http://localhost:%d/actuator/openapi/duuid-server", port + 1);
    headers = new HttpHeaders();
    // set `content-type` header
    headers.setContentType(APPLICATION_JSON);
    // set `accept` header
    headers.setAccept(Collections.singletonList(APPLICATION_JSON));
  }

  private void buildHeaders(String identifier) {
    long now = System.currentTimeMillis();
    headers.set(properties.getHttpHeaderIdentifier(), identifier);
    headers.set(properties.getHttpHeaderTimestamp(), String.valueOf(now));
    String random = RandomStringUtils.randomAlphabetic(64);
    headers.set(properties.getHttpHeaderRandom(), random);
    headers.set(
        properties.getHttpHeaderSignature(),
        HmacToolkit.hmacSha256(identifier + now + random, properties.getWorkKey()));
  }

  @Test
  void test1() {
    buildHeaders("client");
    HttpEntity<Void> entity = new HttpEntity<>(headers);
    long id1 = restTemplate.postForObject(idGenEngdpoint, entity, Duuid.class).id();
    buildHeaders("client");
    entity = new HttpEntity<>(headers);
    long id2 = restTemplate.postForObject(idGenEngdpoint, entity, Duuid.class).id();
    assertTrue(id2 > id1);
  }

  @Test
  void test2() {
    LinkedList<Long> list = new LinkedList<>();
    for (int i = 0; i < 10000; i++) {
      buildHeaders("client");
      HttpEntity<Void> entity = new HttpEntity<>(headers);
      list.add(restTemplate.postForObject(idGenEngdpoint, entity, Duuid.class).id());
    }
    List<Long> back = new ArrayList<>(list);
    back.sort(Long::compare);
    assertEquals(list, back);
  }

  @Test
  void test3() {
    String openApi = restTemplate.getForObject(openApiEngdpoint, String.class);
    assertTrue(openApi.contains("Distributed UUID Generation Service"));
  }

  @Test
  void test4() {
    buildHeaders("prometheus");
    HttpEntity<Void> entity = new HttpEntity<>(headers);
    ResponseEntity<String> exchange = restTemplate.exchange(prometheusEngdpoint, GET, entity, String.class);
    assertFalse(exchange.getBody() == null || exchange.getBody().isEmpty());
  }
}
