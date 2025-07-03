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

package com.silong.llm.chatbot;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.util.StringUtils.hasLength;

import com.silong.foundation.springboot.starter.jwt.common.Credentials;
import com.silong.foundation.springboot.starter.jwt.common.TokenBody;
import com.silong.llm.chatbot.providers.LdapUserProvider;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("startTLS")
@AutoConfigureWebTestClient
public class ChatbotApplicationStartTLSTests {

  private static final String ADMIN_PASSWORD = "Secret@123";
  private static final String TEST_DB = "test_db";

  @Container
  private static final GenericContainer<?> REDIS_CONTAINER =
      new GenericContainer<>("redis:6.2.6")
          .withExposedPorts(6379)
          .waitingFor(Wait.forListeningPort())
          .withStartupTimeout(Duration.ofMinutes(1));

  @Container
  private static final GenericContainer<?> MYSQL_CONTAINER =
      new GenericContainer<>("mysql:8.0.33")
          .withEnv("MYSQL_DATABASE", TEST_DB)
          .withEnv("MYSQL_ROOT_PASSWORD", ADMIN_PASSWORD)
          .withExposedPorts(3306)
          .withCommand("--default-authentication-plugin=mysql_native_password")
          .waitingFor(Wait.forLogMessage(".*port: 3306  MySQL Community Server - GPL.*\\n", 1));

  @Container
  private static final GenericContainer<?> LDAP_CONTAINER =
      new GenericContainer<>("rroemhild/docker-test-openldap:2.1").withExposedPorts(10389);

  @DynamicPropertySource
  static void redisProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
    registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379));
    registry.add(
        "spring.datasource.url",
        () ->
            String.format(
                "jdbc:mysql://%s:%d/%s",
                MYSQL_CONTAINER.getHost(), MYSQL_CONTAINER.getMappedPort(3306), TEST_DB));
    registry.add("spring.datasource.username", () -> "root");
    registry.add("spring.datasource.password", () -> ADMIN_PASSWORD);

    registry.add("ldap.base-dn", () -> "dc=planetexpress,dc=com");
    registry.add("ldap.username", () -> "cn=admin,dc=planetexpress,dc=com");
    registry.add("ldap.password", () -> "GoodNewsEveryone");

    registry.add(
        "ldap.urls",
        () ->
            new String[] {
              String.format(
                  "ldap://%s:%d", LDAP_CONTAINER.getHost(), LDAP_CONTAINER.getMappedPort(10389))
            });
  }

  @Autowired private WebTestClient webTestClient;

  @Value("${jwt-auth.auth-path}")
  private String loginPath;

  @Autowired private LdapUserProvider ldapUserProvider;

  @AfterAll
  static void cleanup() {
    REDIS_CONTAINER.stop();
    MYSQL_CONTAINER.stop();
    LDAP_CONTAINER.stop();
  }

  @Test
  @Order(1)
  void testRedisConnected() {
    assertTrue(REDIS_CONTAINER.isRunning());
  }

  @Test
  void searchUser() {
    assertDoesNotThrow(() -> ldapUserProvider.checkUserExists("professor"));
  }

  @Test
  @Order(2)
  void testImportUsers() {
    Credentials credentials = new Credentials();
    credentials.setUserName("professor");
    credentials.setPassword("professor");
    assertDoesNotThrow(() -> ldapUserProvider.authenticate(credentials));

    credentials.setUserName("fry");
    credentials.setPassword("fry");
    assertDoesNotThrow(() -> ldapUserProvider.authenticate(credentials));

    credentials.setUserName("zoidberg");
    credentials.setPassword("zoidberg");
    assertDoesNotThrow(() -> ldapUserProvider.authenticate(credentials));

    credentials.setUserName("hermes");
    credentials.setPassword("hermes");
    assertDoesNotThrow(() -> ldapUserProvider.authenticate(credentials));

    credentials.setUserName("leela");
    credentials.setPassword("leela");
    assertDoesNotThrow(() -> ldapUserProvider.authenticate(credentials));

    credentials.setUserName("bender");
    credentials.setPassword("bender");
    assertDoesNotThrow(() -> ldapUserProvider.authenticate(credentials));

    credentials.setUserName("amy");
    credentials.setPassword("amy");
    assertDoesNotThrow(() -> ldapUserProvider.authenticate(credentials));
  }

  @Test
  void testLogin() {
    Credentials credentials = new Credentials();
    credentials.setPassword("amy");
    credentials.setUserName("amy");

    TokenBody responseBody =
        webTestClient
            .post()
            .uri(loginPath)
            .body(BodyInserters.fromValue(credentials))
            .accept(APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(TokenBody.class)
            .returnResult()
            .getResponseBody();

    assertTrue(responseBody != null && hasLength(responseBody.getToken()));
  }
}
