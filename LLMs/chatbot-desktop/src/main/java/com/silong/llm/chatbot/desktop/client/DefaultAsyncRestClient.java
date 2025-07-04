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

package com.silong.llm.chatbot.desktop.client;

import static com.silong.llm.chatbot.desktop.ChatbotDesktopApplication.CONFIGURATION;
import static org.apache.hc.core5.http.ContentType.TEXT_EVENT_STREAM;
import static org.apache.hc.core5.http.ContentType.TEXT_PLAIN;
import static org.apache.hc.core5.http.HttpHeaders.*;
import static org.apache.hc.core5.http.HttpStatus.SC_OK;
import static org.apache.hc.core5.http2.HttpVersionPolicy.NEGOTIATE;
import static org.apache.hc.core5.pool.PoolConcurrencyPolicy.LAX;
import static org.apache.hc.core5.pool.PoolReusePolicy.LIFO;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.silong.llm.chatbot.desktop.config.HttpClientConfig;
import com.silong.llm.chatbot.desktop.config.HttpClientConnectionPoolConfig;
import com.silong.llm.chatbot.desktop.config.HttpClientProxy;
import com.silong.llm.chatbot.desktop.config.HttpClientRequestConfig;
import com.silong.llm.chatbot.desktop.utils.HostInfoConverter.HostInfo;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import javax.net.ssl.SSLContext;
import javax.security.auth.login.LoginException;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.SystemDefaultDnsResolver;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.config.TlsConfig;
import org.apache.hc.client5.http.impl.DefaultConnectionKeepAliveStrategy;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http.nio.ssl.BasicClientTlsStrategy;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

/**
 * 异步客户端
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-19 20:19
 */
@Slf4j
class DefaultAsyncRestClient implements AsyncRestClient, Closeable {

  private static final String LOGIN_REQUEST_TEMPLATE =
"""
{"user_name":"%s","password":"%s"}
""";

  private static final String LOGIN_RESPONSE_TEMPLATE =
"""
{"token":"%s"}
""";

  private static final String CONVERSATION_ID = "Conversation-ID";

  /** 访问token */
  private static final String ACCESS_TOKEN = "Access-Token";

  private final ObjectMapper mapper = new ObjectMapper();

  private final CloseableHttpAsyncClient httpAsyncClient;

  private final URI chatStreamRequestUri;

  private final URI chatHistoryRequestUri;

  private final URI loginRequestUri;

  private final List<ResponseCallback> responseCallbacks = new CopyOnWriteArrayList<>();

  /**
   * 用户登录
   *
   * @param userName 用户名
   * @param password 密码
   * @return 用户凭证
   * @throws LoginException 登录异常
   */
  private String login(String userName, String password) throws LoginException {
    try (var client = HttpClients.createDefault()) {
      // 创建 HTTP POST 请求
      var httpPost = new HttpPost(loginRequestUri);

      // 设置请求参数
      httpPost.setEntity(
          new StringEntity(
              String.format(LOGIN_REQUEST_TEMPLATE, userName, password),
              ContentType.APPLICATION_JSON));

      var token =
          client.execute(
              httpPost,
              response -> {
                // 获取响应实体
                var entity = response.getEntity();

                // 处理响应内容
                String responseBody = EntityUtils.toString(entity);
                if (response.getCode() == SC_OK) {
                  // 将 JSON 字符串解析为 Map
                  Map<String, String> map = mapper.readValue(responseBody, Map.class);
                  return map.get("token");
                } else {
                  String msg =
                      String.format(
                          "Failed to login with userName:%s and password:%s, statusCode:%s, reason:%s.",
                          userName, "******", response.getCode(), responseBody);
                  log.error(msg);
                  return null;
                }
              });
      if (token == null || token.isEmpty()) {
        throw new LoginException(
            String.format("Failed to login with userName:%s and password:%s", userName, password));
      }
      return token;
    } catch (IOException e) {
      throw new LoginException(e.getMessage());
    }
  }

  /**
   * 构造方法
   *
   * @param userName 用户
   * @param password 密码
   * @param hostInfo 主机信息
   */
  public DefaultAsyncRestClient(
      @NonNull String userName, @NonNull String password, @NonNull HostInfo hostInfo)
      throws LoginException {
    this.chatStreamRequestUri =
        URI.create(
            String.format(
                "https://%s:%d%s",
                hostInfo.host(),
                hostInfo.port(),
                CONFIGURATION
                    .httpClientConfig()
                    .httpClientRequestConfig()
                    .chatStreamRequestPath()));
    this.chatHistoryRequestUri =
        URI.create(
            String.format(
                "https://%s:%d%s",
                hostInfo.host(),
                hostInfo.port(),
                CONFIGURATION
                    .httpClientConfig()
                    .httpClientRequestConfig()
                    .conversationHistoryRequestPath()));
    this.loginRequestUri =
        URI.create(
            String.format(
                "https://%s:%d%s",
                hostInfo.host(),
                hostInfo.port(),
                CONFIGURATION.httpClientConfig().httpClientRequestConfig().loginRequestPath()));
    this.httpAsyncClient = createHttpClient(login(userName, password));
  }

  /**
   * 注册响应回调
   *
   * @param callback 响应回调
   */
  @Override
  public void register(@NonNull ResponseCallback callback) {
    responseCallbacks.add(callback);
  }

  /**
   * 创建默认的HttpClient实例，使用连接池和线程安全配置
   *
   * @param accessToken 用户凭证
   * @return CloseableHttpClient实例
   */
  @SneakyThrows({
    NoSuchAlgorithmException.class,
    KeyStoreException.class,
    KeyManagementException.class
  })
  private CloseableHttpAsyncClient createHttpClient(String accessToken) {
    HttpClientConfig httpClientConfig = CONFIGURATION.httpClientConfig();
    HttpClientConnectionPoolConfig connectionPoolConfig =
        httpClientConfig.httpClientConnectionPoolConfig();
    HttpClientProxy httpClientProxy = httpClientConfig.httpClientProxy();
    HttpClientRequestConfig httpClientRequestConfig = httpClientConfig.httpClientRequestConfig();

    // 创建连接池管理器
    var connectionManager =
        PoolingAsyncClientConnectionManagerBuilder.create()
            .setDnsResolver(SystemDefaultDnsResolver.INSTANCE)
            .setConnPoolPolicy(LIFO)
            .setPoolConcurrencyPolicy(LAX)
            // 设置TLS配置
            .setDefaultTlsConfig(
                TlsConfig.custom()
                    .setHandshakeTimeout(Timeout.ofSeconds(connectionPoolConfig.handshakeTimeout()))
                    .setSupportedCipherSuites(connectionPoolConfig.supportedCipherSuites())
                    .setSupportedProtocols(connectionPoolConfig.supportedTLSVersions())
                    .setVersionPolicy(NEGOTIATE)
                    .build())
            // 设置最大连接数
            .setMaxConnTotal(connectionPoolConfig.connectionPoolSize())
            // 设置每个路由的默认最大连接数
            .setMaxConnPerRoute(connectionPoolConfig.connectionPoolSize())
            // 配置连接池内的连接
            .setDefaultConnectionConfig(
                ConnectionConfig.custom()
                    .setValidateAfterInactivity(
                        TimeValue.ofSeconds(connectionPoolConfig.validateAfterInactivity()))
                    .setConnectTimeout(Timeout.ofSeconds(connectionPoolConfig.connectTimeout()))
                    .setTimeToLive(TimeValue.ofSeconds(connectionPoolConfig.timeToLive()))
                    .build())
            .setTlsStrategy(new BasicClientTlsStrategy(trustAllSslContext()))
            .build();

    // 创建请求配置

    RequestConfig requestConfig =
        RequestConfig.custom()
            .setRedirectsEnabled(false)
            .setCookieSpec("ignoreCookies")
            .setResponseTimeout(Timeout.ofSeconds(httpClientRequestConfig.responseTimeout()))
            .build();

    // 创建HttpClient实例
    var httpClientBuilder =
        HttpAsyncClients.custom()
            .setDefaultHeaders(
                List.of(
                    new BasicHeader(ACCESS_TOKEN, accessToken),
                    new BasicHeader(ACCEPT, TEXT_EVENT_STREAM)))
            .setKeepAliveStrategy(DefaultConnectionKeepAliveStrategy.INSTANCE)
            .setConnectionManager(connectionManager)
            .addRequestInterceptorLast(
                ((request, entity, context) -> {
                  if (log.isDebugEnabled()) {
                    log.debug("request:{}, entity:{}, context:{}", request, entity, context);
                  }
                }))
            .addResponseInterceptorFirst(
                ((response, entity, context) -> {
                  if (log.isDebugEnabled()) {
                    log.debug("response:{}, entity:{}, context:{}", response, entity, context);
                  }
                }))
            .evictIdleConnections(
                Timeout.ofSeconds(
                    httpClientConfig.httpClientConnectionPoolConfig().evictIdleTime()))
            .setDefaultRequestConfig(requestConfig)
            .evictExpiredConnections()
            .setRetryStrategy(
                new DefaultHttpRequestRetryStrategy(
                    httpClientConfig.httpClientRetiesConfig().maxRetries(),
                    Timeout.ofSeconds(httpClientConfig.httpClientRetiesConfig().retryInterval())));

    if (httpClientProxy != null
        && httpClientProxy.host() != null
        && !httpClientProxy.host().isEmpty()) {
      httpClientBuilder.setProxy(new HttpHost(httpClientProxy.host(), httpClientProxy.port()));

      if (httpClientProxy.username() != null && !httpClientProxy.username().isEmpty()) {
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
            new AuthScope(httpClientProxy.host(), httpClientProxy.port()),
            new UsernamePasswordCredentials(
                httpClientProxy.username(), httpClientProxy.password().toCharArray()));
        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
      }
    }

    CloseableHttpAsyncClient httpAsyncClient = httpClientBuilder.build();
    httpAsyncClient.start();
    return httpAsyncClient;
  }

  /**
   * 发送用户输入的问题，与LLM交互
   *
   * @param query 用户输入
   * @param conversationId 会话id
   * @return 异步任务
   */
  @Override
  public Future<?> ask(@NonNull String query, @NonNull String conversationId) {
    return httpAsyncClient.execute(
        SimpleRequestBuilder.post(chatStreamRequestUri)
            .addHeader(CONVERSATION_ID, conversationId)
            .setBody(query, TEXT_PLAIN)
            .build(),
        new FutureCallback<>() {
          @Override
          public void completed(SimpleHttpResponse response) {
            Header cidHeader = response.getFirstHeader(CONVERSATION_ID);
            String cid = cidHeader != null ? cidHeader.getValue() : null;
            String bodyText = response.getCode() == SC_OK ? response.getBodyText() : null;
            responseCallbacks.forEach(
                responseCallback ->
                    responseCallback.callback(
                        bodyText == null
                            ? responseCallback.getMessage("server.internal.error")
                            : bodyText,
                        cid));
          }

          @Override
          public void failed(Exception ex) {
            log.error("Failed to post the request[{}, {}] to server.", conversationId, query, ex);
            responseCallbacks.forEach(
                responseCallback ->
                    responseCallback.callback(
                        responseCallback.getMessage("server.internal.error"), null));
          }

          @Override
          public void cancelled() {
            log.warn("The request[{}, {}] was cancelled.", conversationId, query);
          }
        });
  }

  /**
   * 创建SSL上下文并加载信任策略
   *
   * @return ssl上下文
   * @throws NoSuchAlgorithmException 异常
   * @throws KeyManagementException 异常
   * @throws KeyStoreException 异常
   */
  private static SSLContext trustAllSslContext()
      throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
    return SSLContexts.custom().loadTrustMaterial(null, (chain, authType) -> true).build();
  }

  @Override
  @SneakyThrows(IOException.class)
  public void close() {
    if (httpAsyncClient != null) {
      httpAsyncClient.close();
    }
    responseCallbacks.clear();
  }
}
