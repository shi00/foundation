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
import com.silong.foundation.webclient.reactive.config.WebClientSslConfig;
import okhttp3.Protocol;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.List;

/**
 * 测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-02-02 13:41
 */
public class WebClientHttpsTwoWayTlsv12Tests extends WebClientHttpsTwoWayTlsv13Tests {

  private static final WebClientSslConfig CLIENT_SSL_CONFIG_TWO_WAY =
      new WebClientSslConfig()
          .trustAll(false)
          .protocols(new String[] {TLSV_1_2})
          .keyStoreType(PKCS_12)
          .keyStorePath(CLIENT_KEYSTORE_PATH)
          .keyStorePassword(PASSWORD)
          .trustStoreType(PKCS_12)
          .trustStorePassword(PASSWORD)
          .trustStorePath(CLIENT_TRUST_KEYSTORE_PATH);

  @BeforeAll
  static void setup() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.requestClientAuth();
    mockWebServer.setProtocolNegotiationEnabled(true);
    mockWebServer.setProtocols(List.of(Protocol.HTTP_2, Protocol.HTTP_1_1));
    mockWebServer.useHttps(buildTestSslContext().getSocketFactory(), false);
    mockWebServer.start(0);
    baseUrl = String.format("https://localhost:%s", mockWebServer.getPort());
    webClient =
        WebClients.create(
            new WebClientConfig(), CLIENT_SSL_CONFIG_TWO_WAY, MAPPER);
  }

  private static SSLContext buildTestSslContext() throws Exception {
    char[] passwordArray = PASSWORD.toCharArray();
    KeyStore serverKeyStore = KeyStore.getInstance(PKCS_12);
    try (InputStream in = new FileInputStream(SERVER_KEYSTORE_PATH)) {
      serverKeyStore.load(in, passwordArray);
    }

    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(serverKeyStore, passwordArray);

    KeyStore trustServerKeyStore = KeyStore.getInstance(PKCS_12);
    try (InputStream in = new FileInputStream(SERVER_TRUST_KEYSTORE_PATH)) {
      trustServerKeyStore.load(in, passwordArray);
    }
    TrustManagerFactory trustManagerFactory =
        TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    trustManagerFactory.init(trustServerKeyStore);
    SSLContext sslContext = SSLContext.getInstance(TLSV_1_2);
    sslContext.init(
        kmf.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());
    return sslContext;
  }


  @Test
  @DisplayName("https-TLSv1.2-TwoWay-GET")
  void test1() throws IOException {
    getTest();
  }

  @Test
  @DisplayName("https-TLSv1.2-TwoWay-DELETE")
  void test2() {
    deleteTest();
  }

  @Test
  @DisplayName("https-TLSv1.2-TwoWay-POST")
  void test3() {
    postTest();
  }

  @Test
  @DisplayName("https-TLSv1.2-TwoWay-PUT")
  void test4() {
    putTest();
  }

  @Test
  @DisplayName("https-TLSv1.2-TwoWay-HEAD")
  void test5() {
    headTest();
  }

  @Test
  @DisplayName("https-TLSv1.2-TwoWay-PATCH")
  void test6() {
    patchTest();
  }
}
