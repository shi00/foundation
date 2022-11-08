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

import com.silong.foundation.webclient.reactive.config.WebClientConfig;
import okhttp3.Protocol;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

/**
 * 测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-02-02 13:41
 */
public class WebClientHttpTests extends BaseTests {

  @BeforeAll
  static void setup() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.setProtocolNegotiationEnabled(true);
    mockWebServer.setProtocols(List.of(Protocol.HTTP_1_1, Protocol.HTTP_2));
    mockWebServer.start(0);
    baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());
    webClient = WebClients.create(new WebClientConfig(), MAPPER);
  }

  @Test
  @DisplayName("http-GET")
  void test1() throws IOException {
    getTest();
  }

  @Test
  @DisplayName("http-DELETE")
  void test2() {
    deleteTest();
  }

  @Test
  @DisplayName("http-POST")
  void test3() {
    postTest();
  }

  @Test
  @DisplayName("http-PUT")
  void test4() {
    putTest();
  }

  @Test
  @DisplayName("http-HEAD")
  void test5() {
    headTest();
  }

  @Test
  @DisplayName("http-PATCH")
  void test6() {
    patchTest();
  }
}
