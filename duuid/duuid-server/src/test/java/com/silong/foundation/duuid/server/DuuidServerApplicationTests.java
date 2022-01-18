package com.silong.foundation.duuid.server;

import com.silong.foundation.crypto.digest.HmacToolkit;
import com.silong.foundation.duuid.server.model.Duuid;
import com.silong.foundation.springboot.starter.simpleauth.configure.properties.SimpleAuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static com.silong.foundation.springboot.starter.simpleauth.constants.AuthHeaders.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_PLAIN;

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
@TestPropertySource(
    locations = "classpath:application-test.properties",
    properties = {"management.server.port=35671"})
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class DuuidServerApplicationTests {

  @LocalServerPort private int port;

  @Value("${management.server.port}")
  private int actuatorPort;

  @Autowired private TestRestTemplate restTemplate;

  @Autowired private SimpleAuthProperties properties;

  private String idGenEngdpoint;

  private String prometheusEngdpoint;

  private String openApiEngdpoint;

  private HttpHeaders headers;

  @BeforeEach
  void init() {
    idGenEngdpoint = String.format("http://localhost:%d/duuid", port);
    prometheusEngdpoint = String.format("http://localhost:%d/actuator/prometheus", actuatorPort);
    openApiEngdpoint =
        String.format("http://localhost:%d/actuator/openapi/duuid-server", actuatorPort);
    headers = new HttpHeaders();
    // set `content-type` header
    headers.setContentType(APPLICATION_JSON);
    // set `accept` header
    headers.setAccept(Arrays.asList(APPLICATION_JSON, TEXT_PLAIN));
  }

  private void buildHeaders(String identity) {
    long now = System.currentTimeMillis();
    headers.set(IDENTITY, identity);
    headers.set(TIMESTAMP, String.valueOf(now));
    String random = RandomStringUtils.randomAlphabetic(64);
    headers.set(RANDOM, random);
    headers.set(
        SIGNATURE, HmacToolkit.hmacSha256(identity + now + random, properties.getWorkKey()));
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
    headers = new HttpHeaders();
    buildHeaders("prometheus");
    HttpEntity<Void> entity = new HttpEntity<>(headers);
    ResponseEntity<Object> responseEntity =
        restTemplate.exchange(
            prometheusEngdpoint,
            //            "http://localhost:27891/actuator/prometheus",
            GET,
            entity,
            Object.class);
    System.out.println(responseEntity.getBody());
    //    assertFalse(responseEntity.getBody() == null || responseEntity.getBody().isEmpty());
  }
}
