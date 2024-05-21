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

package com.silong.foundation.springboot.starter.crypto;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * 测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-19 21:36
 */
@Slf4j
@SpringBootTest(classes = TestApp.class, webEnvironment = RANDOM_PORT)
@TestPropertySource(locations = "classpath:application.properties")
@ExtendWith(SpringExtension.class)
public class AppTests {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void testPass() {
    ResponseEntity<String> idEntity = restTemplate.getForEntity("/pass", String.class);
    Assertions.assertEquals(idEntity.getBody(), "password");
  }

  @Test
  void testName() {
    ResponseEntity<String> idEntity = restTemplate.getForEntity("/name", String.class);
    Assertions.assertEquals(idEntity.getBody(), "security:abc");
  }

  @Test
  void testId() {
    ResponseEntity<String> idEntity = restTemplate.postForEntity("/id", null, String.class);
    Assertions.assertEquals(idEntity.getBody(), "123456");
  }
}
