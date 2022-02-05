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
public class WebClientHttpsTlsv12Tests extends BaseTests {

  static final String PASSWORD = "password";

  static final String CLIENT_KEYSTORE_PATH = "src/test/resources/tlsv12/client-keystore.jks";

  static final String CLIENT_TRUST_KEYSTORE_PATH =
      "src/test/resources/tlsv12/client-truststore.jks";

  static final WebClientSslConfig CLIENT_SSL_CONFIG_TREUST_ALL =
      new WebClientSslConfig()
          .trustAll(true)
          .keyStoreType("jks")
          .keyStorePassword(PASSWORD)
          .keyStorePath(CLIENT_KEYSTORE_PATH);

  static final WebClientSslConfig CLIENT_SSL_CONFIG =
      new WebClientSslConfig()
          .trustAll(false)
          .protocols(List.of("TLSv1.2").toArray(String[]::new))
          .ciphers(List.of("TLS_AES_128_GCM_SHA256"))
          .trustStoreType("jks")
          .trustStorePassword(PASSWORD)
          .trustStorePath(CLIENT_TRUST_KEYSTORE_PATH)
          .keyStoreType("jks")
          .keyStorePassword(PASSWORD)
          .keyStorePath(CLIENT_KEYSTORE_PATH);

  @BeforeAll
  static void setup() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.requestClientAuth();
    mockWebServer.setProtocolNegotiationEnabled(true);
    mockWebServer.setProtocols(List.of(Protocol.HTTP_1_1, Protocol.HTTP_2));
    mockWebServer.useHttps(buildTestSslContext().getSocketFactory(), false);
    mockWebServer.start(findAvailableTcpPort(PORT_RANGE_MIN, PORT_RANGE_MAX));
    baseUrl = String.format("https://localhost:%s", mockWebServer.getPort());
    webClient =
        WebClients.create(new WebClientConfig().baseUrl(baseUrl), CLIENT_SSL_CONFIG, MAPPER);
  }

  private static SSLContext buildTestSslContext() throws Exception {
    // Load self-signed certificate
    char[] serverKeyStorePassword = PASSWORD.toCharArray();
    KeyStore serverKeyStore = KeyStore.getInstance("jks");
    try (InputStream in =
        WebClientHttpsTlsv12Tests.class.getResourceAsStream("tlsv12/server-keystore.jks")) {
      serverKeyStore.load(in, serverKeyStorePassword);
    }

    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(serverKeyStore, serverKeyStorePassword);

    KeyStore trustStore = KeyStore.getInstance("jks");
    try (InputStream in =
        WebClientHttpsTlsv12Tests.class.getResourceAsStream("tlsv12/server-truststore.jks")) {
      trustStore.load(in, serverKeyStorePassword);
    }
    TrustManagerFactory trustManagerFactory =
        TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    trustManagerFactory.init(trustStore);
    SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
    sslContext.init(
        kmf.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());
    return sslContext;
  }

  @Test
  @DisplayName("https-TwoWay-GET")
  void test1() throws IOException {
    getTest();
  }
}
