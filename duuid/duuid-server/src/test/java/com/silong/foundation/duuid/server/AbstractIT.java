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
package com.silong.foundation.duuid.server;

import com.silong.foundation.crypto.RootKey;
import com.silong.foundation.crypto.digest.HmacToolkit;
import com.silong.foundation.duuid.server.model.Duuid;
import com.silong.foundation.springboot.starter.simpleauth.configure.config.SimpleAuthProperties;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static com.silong.foundation.springboot.starter.simpleauth.constants.AuthHeaders.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

/**
 * 抽象集成测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-30 18:59
 */
abstract class AbstractIT {

  /** 工作密钥 */
  static String workKey;

  @LocalServerPort
  private int port;

  @Value("${management.server.port}")
  private int actuatorPort;

  @Autowired private TestRestTemplate restTemplate;

  @Autowired private SimpleAuthProperties properties;

  private String idGenEngdpoint;

  private String prometheusEngdpoint;

  private String openApiEngdpoint;

  private String swaggerUiEndpoint;

  private HttpHeaders headers;

  @BeforeAll
  static void initRootKey() throws IOException {
    Path dir = new File("target/test-classes").toPath();
    RootKey.export(
        RootKey.DEFAULT_ROOT_KEY_PARTS.stream()
            .map(s -> dir.resolve(s).toFile())
            .toArray(File[]::new));
    RootKey rootKey = RootKey.initialize();
    workKey = rootKey.encryptWorkKey(RandomStringUtils.random(RandomUtils.nextInt(1, Short.SIZE)));
  }

  @BeforeEach
  void init() {
    idGenEngdpoint = String.format("http://localhost:%d/duuid", port);
    prometheusEngdpoint = String.format("http://localhost:%d/actuator/prometheus", actuatorPort);
    openApiEngdpoint =
        String.format("http://localhost:%d/actuator/openapi/duuid-server", actuatorPort);
    swaggerUiEndpoint = String.format("http://localhost:%d/actuator/swagger-ui", actuatorPort);
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
    String random = randomAlphabetic(64);
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
    ResponseEntity<String> response =
        restTemplate.exchange(openApiEngdpoint, GET, null, String.class);
    assertEquals(OK, response.getStatusCode());
  }

  @Test
  void test4() {
    ResponseEntity<String> response =
        restTemplate.exchange(swaggerUiEndpoint, GET, null, String.class);
    assertEquals(OK, response.getStatusCode());
  }

  @Test
  void test5() {
    buildHeaders("prometheus");
    HttpEntity<Void> entity = new HttpEntity<>(headers);
    ResponseEntity<String> responseEntity =
        restTemplate.exchange(prometheusEngdpoint, GET, entity, String.class);
    System.out.println(responseEntity.getBody());
    assertFalse(responseEntity.getBody() == null || responseEntity.getBody().isEmpty());
  }
}
