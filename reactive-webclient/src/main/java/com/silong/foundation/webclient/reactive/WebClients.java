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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.silong.foundation.webclient.reactive.config.WebClientConfig;
import com.silong.foundation.webclient.reactive.config.WebClientProxyConfig;
import com.silong.foundation.webclient.reactive.config.WebClientSslConfig;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.ssl.*;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ClientCodecConfigurer.ClientDefaultCodecs;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider.Builder;

import javax.net.ssl.CertPathTrustManagerParameters;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.*;
import java.time.Duration;
import java.util.Collection;
import java.util.Locale;

import static com.silong.foundation.webclient.reactive.config.WebClientConfig.NETTY_CLIENT_CATEGORY;
import static com.silong.foundation.webclient.reactive.config.WebClientSslConfig.DEFAULT_APN;
import static io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS;
import static io.netty.channel.ChannelOption.TCP_FASTOPEN_CONNECT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.springframework.util.StringUtils.hasLength;
import static reactor.netty.transport.logging.AdvancedByteBufFormat.TEXTUAL;

/**
 * WebClient创建工具
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-02-01 12:33
 */
@Slf4j(topic = NETTY_CLIENT_CATEGORY)
@SuppressFBWarnings(
    value = {"PATH_TRAVERSAL_IN", "URLCONNECTION_SSRF_FD"},
    justification = "配置密钥需要指定密钥文件位置")
public final class WebClients {

  /**
   * 根据配置创建webclient
   *
   * @param webClientConfig 基础配置
   * @return webclient
   */
  public static WebClient create(WebClientConfig webClientConfig) {
    return create(webClientConfig, null, null, new ObjectMapper());
  }

  /**
   * 根据配置创建webclient
   *
   * @param webClientConfig 基础配置
   * @param objectMapper jackson
   * @return webclient
   */
  public static WebClient create(WebClientConfig webClientConfig, ObjectMapper objectMapper) {
    return create(webClientConfig, null, null, objectMapper);
  }

  /**
   * 根据配置创建webclient
   *
   * @param webClientConfig 基础配置
   * @param webClientSslConfig ssl配置
   * @param objectMapper jackson
   * @return webclient
   */
  public static WebClient create(
      WebClientConfig webClientConfig,
      WebClientSslConfig webClientSslConfig,
      ObjectMapper objectMapper) {
    return create(webClientConfig, webClientSslConfig, null, objectMapper);
  }

  /**
   * 根据配置创建webclient
   *
   * @param webClientConfig 基础配置
   * @param proxyConfig 代理配置
   * @param objectMapper jackson
   * @return webclient
   */
  public static WebClient create(
      WebClientConfig webClientConfig,
      WebClientProxyConfig proxyConfig,
      ObjectMapper objectMapper) {
    return create(webClientConfig, null, proxyConfig, objectMapper);
  }

  /**
   * 根据配置创建webclient
   *
   * @param webClientConfig 基础配置
   * @param proxyConfig 代理配置
   * @param webClientSslConfig ssl配置
   * @param objectMapper jackson
   * @return webclient
   */
  public static WebClient create(
      @NonNull WebClientConfig webClientConfig,
      @Nullable WebClientSslConfig webClientSslConfig,
      @Nullable WebClientProxyConfig proxyConfig,
      @NonNull ObjectMapper objectMapper) {
    return WebClient.builder()
        .clientConnector(
            new ReactorClientHttpConnector(
                buildHttpClient(webClientConfig, webClientSslConfig, proxyConfig)))
        .exchangeStrategies(
            buildExchangeStrategies(webClientConfig.codecMaxBufferSize(), objectMapper))
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }

  private static ExchangeStrategies buildExchangeStrategies(
      int bufferSize, ObjectMapper objectMapper) {
    return ExchangeStrategies.builder()
        .codecs(
            configurer -> {
              ClientDefaultCodecs clientDefaultCodecs = configurer.defaultCodecs();
              clientDefaultCodecs.maxInMemorySize(bufferSize);
              clientDefaultCodecs.jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper));
              clientDefaultCodecs.jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper));
            })
        .build();
  }

  private static HttpClient buildHttpClient(
      WebClientConfig webClientConfig,
      WebClientSslConfig webClientSslConfig,
      WebClientProxyConfig proxyConfig) {
    boolean httpsEnabled = httpsEnabled(webClientConfig.baseUrl());
    HttpClient httpClient =
        HttpClient.create()
            .protocol(httpsEnabled ? HttpProtocol.H2 : HttpProtocol.H2C, HttpProtocol.HTTP11)
            .baseUrl(webClientConfig.baseUrl())
            .keepAlive(webClientConfig.keepAliveEnabled())
            .compress(webClientConfig.compressionEnabled())
            .option(CONNECT_TIMEOUT_MILLIS, (int) webClientConfig.connectTimeoutMillis())
            .option(TCP_FASTOPEN_CONNECT, webClientConfig.fastOpenConnectEnabled())
            .responseTimeout(Duration.ofMillis(webClientConfig.responseTimeoutMillis()))
            .doOnError(
                (request, throwable) ->
                    log.error(
                        "WebClient request: {} {} {} {}",
                        request.requestId(),
                        request.method(),
                        request.uri(),
                        request.version(),
                        throwable),
                (response, throwable) ->
                    log.error(
                        "WebClient response: {} {} {} {} {}",
                        response.requestId(),
                        response.method(),
                        response.uri(),
                        response.version(),
                        response.status(),
                        throwable))
            .doOnConnected(
                conn ->
                    conn.addHandlerLast(
                            new ReadTimeoutHandler(
                                webClientConfig.readTimeoutMillis(), MILLISECONDS))
                        .addHandlerLast(
                            new WriteTimeoutHandler(
                                webClientConfig.writeTimeoutMillis(), MILLISECONDS)))
            .disableRetry(webClientConfig.disableRetryOnce())
            // 开启日志打印
            .wiretap(NETTY_CLIENT_CATEGORY, LogLevel.DEBUG, TEXTUAL, UTF_8);

    // 如果开启代理则配置代理
    if (proxyConfig != null && proxyConfig.enabled()) {
      httpClient =
          httpClient.proxy(
              typeSpec -> {
                Builder builder =
                    typeSpec
                        .type(proxyConfig.type())
                        .host(proxyConfig.host())
                        .port(proxyConfig.port())
                        .connectTimeoutMillis(webClientConfig.connectTimeoutMillis());
                String password = proxyConfig.password();
                String userName = proxyConfig.userName();
                if (hasLength(password) && hasLength(userName)) {
                  builder.username(userName).password(userNameParam -> password);
                }
                String nonProxyHostsPattern = proxyConfig.nonProxyHostsPattern();
                if (hasLength(nonProxyHostsPattern)) {
                  builder.nonProxyHosts(nonProxyHostsPattern);
                }
              });
      log.info("WebClient enable proxy with {}", proxyConfig);
    }

    // 如果开启ssl则配置
    if (webClientSslConfig != null && httpsEnabled) {
      httpClient =
          httpClient.secure(
              sslContextSpec ->
                  sslContextSpec
                      .sslContext(buildSslContext(webClientSslConfig))
                      .handshakeTimeoutMillis(webClientSslConfig.handshakeTimeoutMillis()));
      log.info("WebClient enable https with {}", webClientSslConfig);
    }
    return httpClient;
  }

  @SneakyThrows
  @reactor.util.annotation.NonNull
  private static SslContext buildSslContext(WebClientSslConfig webClientSslConfig) {
    SslProvider sslProvider = OpenSsl.isAvailable() ? SslProvider.OPENSSL : SslProvider.JDK;
    return SslContextBuilder.forClient()
        .sslProvider(sslProvider)
        .protocols(webClientSslConfig.protocols())
        .startTls(webClientSslConfig.startTls())
        .applicationProtocolConfig(
            SslProvider.isAlpnSupported(sslProvider)
                ? DEFAULT_APN
                : ApplicationProtocolConfig.DISABLED)
        .ciphers(webClientSslConfig.ciphers(), SupportedCipherSuiteFilter.INSTANCE)
        .keyManager(buildKeyManagerFactory(webClientSslConfig))
        .trustManager(buildTrustManagerFactory(webClientSslConfig))
        .build();
  }

  private static KeyManagerFactory buildKeyManagerFactory(WebClientSslConfig webClientSslConfig)
      throws Exception {
    String keyStorePath = webClientSslConfig.keyStorePath();
    String keyStoreType = webClientSslConfig.keyStoreType();
    if (!hasLength(keyStorePath) || !hasLength(keyStoreType)) {
      log.info(
          "KeyManagerFactory returns null because keyStorePath[{}] or keyStoreType[{}] is empty.",
          keyStorePath,
          keyStoreType);
      return null;
    }
    String keyStoreProvider = webClientSslConfig.keyStoreProvider();
    String keyStorePassword = webClientSslConfig.keyStorePassword();
    KeyStore keyStore =
        loadKeyStore(keyStoreProvider, keyStoreType, keyStorePath, keyStorePassword);
    KeyManagerFactory keyManagerFactory =
        KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    keyManagerFactory.init(
        keyStore, hasLength(keyStorePassword) ? keyStorePassword.toCharArray() : null);
    return keyManagerFactory;
  }

  private static TrustManagerFactory buildTrustManagerFactory(WebClientSslConfig webClientSslConfig)
      throws Exception {
    if (webClientSslConfig.trustAll()) {
      return InsecureTrustManagerFactory.INSTANCE;
    } else {
      String trustStorePath = webClientSslConfig.trustStorePath();
      String trustStoreType = webClientSslConfig.trustStoreType();

      if (!hasLength(trustStorePath) || !hasLength(trustStoreType)) {
        log.info(
            "TrustManagerFactory returns null because trustStorePath[{}] or trustStoreType[{}] is empty.",
            trustStorePath,
            trustStoreType);
        return null;
      }

      TrustManagerFactory trustMgrFactory =
          TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      String trustStorePassword = webClientSslConfig.trustStorePassword();
      String trustStoreProvider = webClientSslConfig.trustStoreProvider();
      KeyStore trustStore =
          loadKeyStore(trustStoreProvider, trustStoreType, trustStorePath, trustStorePassword);
      boolean ocsp = webClientSslConfig.ocsp();
      String crlPath = webClientSslConfig.crlPath();
      boolean initialized = false;
      if (enableCrl(ocsp, crlPath)) {
        PKIXBuilderParameters pkixParams =
            new PKIXBuilderParameters(trustStore, new X509CertSelector());
        if (crlPath != null) {
          pkixParams.setRevocationEnabled(true);
          Collection<? extends CRL> crlList = loadCrl(crlPath);
          if (crlList != null) {
            pkixParams.addCertStore(
                CertStore.getInstance("Collection", new CollectionCertStoreParameters(crlList)));
          }
        }
        trustMgrFactory.init(new CertPathTrustManagerParameters(pkixParams));
        initialized = true;
      }

      if (!initialized) {
        trustMgrFactory.init(trustStore);
      }
      return trustMgrFactory;
    }
  }

  private static KeyStore loadKeyStore(
      final String keystoreProvider,
      final String keystoreType,
      final String keystorePath,
      final String keystorePassword)
      throws Exception {
    KeyStore ks =
        hasLength(keystoreProvider)
            ? KeyStore.getInstance(keystoreType, keystoreProvider)
            : KeyStore.getInstance(keystoreType);
    try (InputStream in = toUrl(keystorePath).openStream()) {
      ks.load(in, hasLength(keystorePassword) ? keystorePassword.toCharArray() : null);
    }
    return ks;
  }

  private static URL toUrl(final String storePath) throws Exception {
    try {
      return new URL(storePath);
    } catch (MalformedURLException e) {
      File file = new File(storePath);
      if (file.exists() && file.isFile()) {
        return file.toURI().toURL();
      } else {
        URL url = WebClients.class.getResource(storePath);
        if (url != null) {
          return url;
        }
      }
    }
    throw new Exception("Failed to find a store at " + storePath);
  }

  private static boolean enableCrl(boolean ocsp, String crlPath) {
    return (ocsp || hasLength(crlPath))
        && "PKIX".equalsIgnoreCase(TrustManagerFactory.getDefaultAlgorithm());
  }

  private static Collection<? extends CRL> loadCrl(String crlPath) throws Exception {
    try (InputStream is = toUrl(crlPath).openStream()) {
      return CertificateFactory.getInstance("X.509").generateCRLs(is);
    }
  }

  private static boolean httpsEnabled(String url) {
    return url.toLowerCase(Locale.ROOT).startsWith("https");
  }
}
