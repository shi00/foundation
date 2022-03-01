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
package com.silong.foundation.springboot.starter.okhttp3;

import com.silong.foundation.springboot.starter.okhttp3.configure.config.WebClientConfig;
import com.silong.foundation.springboot.starter.okhttp3.configure.config.WebClientConnectionPoolConfig;
import com.silong.foundation.springboot.starter.okhttp3.configure.config.WebClientProxyConfig;
import com.silong.foundation.springboot.starter.okhttp3.configure.config.WebClientSslConfig;
import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Authenticator;
import okhttp3.*;
import okhttp3.internal.Util;

import javax.net.SocketFactory;
import javax.net.ssl.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static com.silong.foundation.springboot.starter.okhttp3.Constants.PROXY_AUTHORIZATION;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static okhttp3.ConnectionSpec.MODERN_TLS;
import static okhttp3.Protocol.*;

/**
 * WebClient创建工具
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-02-01 12:33
 */
@Slf4j
public final class WebClients {

  private static final AtomicInteger CONTER = new AtomicInteger(0);

  private static final X509Certificate[] X_509_CERTIFICATES_EMPTY = {};

  private static final X509TrustManager TRUST_ALL_CERTS =
      new X509TrustManager() {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {}

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {}

        @Override
        public X509Certificate[] getAcceptedIssuers() {
          return X_509_CERTIFICATES_EMPTY;
        }
      };

  /**
   * 根据配置创建OkHttp3客户端
   *
   * @param webClientConfig 基础配置
   * @param webClientConnectionPoolConfig 连接池配置
   * @param webClientSslConfig ssl配置
   * @param webClientProxyConfig 代理配置
   * @param dns dns实现
   * @return 客户端
   */
  public static OkHttpClient create(
      @NonNull WebClientConfig webClientConfig,
      @Nullable WebClientConnectionPoolConfig webClientConnectionPoolConfig,
      @Nullable WebClientSslConfig webClientSslConfig,
      @Nullable WebClientProxyConfig webClientProxyConfig,
      @Nullable HostnameVerifier hostnameVerifier,
      @Nullable Authenticator authenticator,
      @Nullable List<Interceptor> normalInterceptors,
      @Nullable List<Interceptor> networkInterceptors,
      @Nullable Dns dns) {
    webClientConnectionPoolConfig =
        webClientConnectionPoolConfig == null
            ? WebClientConnectionPoolConfig.DEFAULT_CONFIG
            : webClientConnectionPoolConfig;

    OkHttpClient.Builder builder =
        new OkHttpClient.Builder()
            .connectTimeout(webClientConfig.connectTimeoutMillis(), MILLISECONDS)
            .readTimeout(webClientConfig.readTimeoutMillis(), MILLISECONDS)
            .writeTimeout(webClientConfig.writeTimeoutMillis(), MILLISECONDS)
            .callTimeout(webClientConfig.callTimeoutMillis(), MILLISECONDS)
            .dispatcher(getDispatcher(webClientConfig))
            .dns(dns == null ? Dns.SYSTEM : dns)
            .addInterceptor(
                chain -> {
                  Request.Builder requestBuilder = chain.request().newBuilder();
                  webClientConfig.defaultRequestHeaders().forEach(requestBuilder::header);
                  return chain.proceed(requestBuilder.build());
                })
            .retryOnConnectionFailure(webClientConfig.retryOnConnectionFailure())
            .pingInterval(webClientConfig.pingIntervalMillis(), MILLISECONDS)
            .connectionPool(
                new ConnectionPool(
                    webClientConnectionPoolConfig.maxIdleConnections(),
                    webClientConnectionPoolConfig.keepAliveMillis(),
                    MILLISECONDS));

    // 设置认证实现
    if (authenticator != null) {
      builder.authenticator(authenticator);
    }

    // 添加拦截器
    if (normalInterceptors != null && !normalInterceptors.isEmpty()) {
      normalInterceptors.forEach(builder::addInterceptor);
    }
    if (networkInterceptors != null && !networkInterceptors.isEmpty()) {
      networkInterceptors.forEach(builder::addNetworkInterceptor);
    }

    if (webClientSslConfig != null) {
      builder
          .protocols(List.of(HTTP_2, HTTP_1_1))
          .connectionSpecs(List.of(MODERN_TLS))
          .sslSocketFactory(buildSslContext(webClientSslConfig).getSocketFactory())
          .hostnameVerifier(
              hostnameVerifier == null ? (hostname, session) -> true : hostnameVerifier);
    } else {
      builder
          .protocols(List.of(H2_PRIOR_KNOWLEDGE, HTTP_1_1))
          .socketFactory(SocketFactory.getDefault());
    }

    // 设置代理
    if (webClientProxyConfig != null) {
      settingProxy(webClientProxyConfig, builder);
    }
    return builder.build();
  }

  private static void settingProxy(
      WebClientProxyConfig webClientProxyConfig, OkHttpClient.Builder builder) {
    builder.proxySelector(
        new ProxySelector() {
          private final List<Proxy> proxy =
              List.of(
                  new Proxy(
                      webClientProxyConfig.type(),
                      new InetSocketAddress(
                          webClientProxyConfig.host(), webClientProxyConfig.port())));

          private final List<Proxy> noProxy = List.of(Proxy.NO_PROXY);

          private final Pattern noProxyHostPattern =
              createNonProxyPattern(webClientProxyConfig.nonProxyHostsPattern());

          @Override
          public List<Proxy> select(URI uri) {
            return noProxyHostPattern != null && noProxyHostPattern.matcher(uri.getHost()).matches()
                ? noProxy
                : proxy;
          }

          @Override
          public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
            log.error("Failed to connect to proxy[{}]", sa, ioe);
          }
        });

    String password = webClientProxyConfig.password();
    String userName = webClientProxyConfig.userName();
    if (userName != null && password != null && !password.isEmpty() && !userName.isEmpty()) {
      builder.proxyAuthenticator(
          (route, response) ->
              response
                  .request()
                  .newBuilder()
                  .header(PROXY_AUTHORIZATION, Credentials.basic(userName, password))
                  .build());
    }
  }

  private static SSLContext buildSslContext(WebClientSslConfig config) {
    try {
      SSLContext sslContext = SSLContext.getInstance(config.protocol());
      if (config.trustAll()) {
        sslContext.init(null, new TrustManager[] {TRUST_ALL_CERTS}, null);
        return sslContext;
      }

      char[] password = config.keyStorePassword().toCharArray();
      KeyStore keyStore = KeyStore.getInstance(config.keyStoreType());
      try (InputStream in = toUrl(config.keyStorePath()).openStream()) {
        keyStore.load(in, password);
      }

      KeyManagerFactory kmf =
          KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      kmf.init(keyStore, password);

      password = config.trustStorePassword().toCharArray();
      KeyStore trustKeyStore = KeyStore.getInstance(config.trustStoreType());
      try (InputStream in = toUrl(config.trustStorePath()).openStream()) {
        trustKeyStore.load(in, password);
      }
      TrustManagerFactory trustManagerFactory =
          TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      trustManagerFactory.init(trustKeyStore);
      sslContext.init(
          kmf.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());
      return sslContext;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static Dispatcher getDispatcher(WebClientConfig webClientConfig) {
    Dispatcher dispatcher =
        new Dispatcher(
            new ThreadPoolExecutor(
                0,
                webClientConfig.dispatcherMaxThreadCount(),
                1,
                MINUTES,
                new SynchronousQueue<>(),
                Util.threadFactory("OkHttp-Dispatcher-" + CONTER.incrementAndGet(), true)));
    dispatcher.setMaxRequests(webClientConfig.dispatcherMaxConcurrentRequests());
    dispatcher.setMaxRequestsPerHost(webClientConfig.dispatcherMaxConcurrentRequestsPerHost());
    return dispatcher;
  }

  private static Pattern createNonProxyPattern(String nonProxyHosts) {
    if (nonProxyHosts == null || nonProxyHosts.isEmpty()) {
      return null;
    }

    // "*.fedora-commons.org" -> ".*?\.fedora-commons\.org"
    nonProxyHosts = nonProxyHosts.replaceAll("\\.", "\\\\.").replaceAll("\\*", ".*?");

    // a|b|*.c -> (a)|(b)|(.*?\.c)
    nonProxyHosts = "(" + nonProxyHosts.replaceAll("\\|", ")|(") + ")";

    try {
      return Pattern.compile(nonProxyHosts);

      // we don't want to bring down the whole server by misusing the nonProxy pattern
      // therefore the error is logged and the web client moves on.
    } catch (Exception e) {
      log.error("Creating the nonProxyHosts pattern failed for {}", nonProxyHosts, e);
      return null;
    }
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
}
