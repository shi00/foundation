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

package com.silong.llm.chatbot.configure;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.util.StringUtils.hasLength;

import com.azure.core.http.ProxyOptions;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.silong.llm.chatbot.configure.properties.ChatbotProperties;
import com.silong.llm.chatbot.configure.properties.LogProperties;
import com.silong.llm.chatbot.configure.properties.McpSseServerProperties;
import com.silong.llm.chatbot.configure.properties.ProxyProperties;
import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Locale;
import javax.net.ssl.SSLException;
import lombok.SneakyThrows;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.mcp.client.autoconfigure.NamedClientMcpTransport;
import org.springframework.ai.mcp.client.autoconfigure.properties.McpClientCommonProperties;
import org.springframework.ai.model.azure.openai.autoconfigure.AzureOpenAIClientBuilderCustomizer;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider.Proxy;

/**
 * 聊天助手自动配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-19 20:21
 */
@Configuration
@EnableConfigurationProperties({
  ChatbotProperties.class,
  McpClientCommonProperties.class,
  McpSseServerProperties.class
})
public class ChatbotAutoConfiguration {

  private static final SslContext TRUST_ALL_SSL_CONTEXT = trustAllSslContext();

  private ChatbotProperties chatbotProperties;

  private McpSseServerProperties mcpSseServerProperties;

  private McpClientCommonProperties mcpClientCommonProperties;

  @SneakyThrows(SSLException.class)
  private static SslContext trustAllSslContext() {
    return SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
  }

  @Bean
  ChatMemoryRepository chatMemoryRepository() {
    return new InMemoryChatMemoryRepository();
  }

  @Bean
  ChatMemory chatMemory() {
    return MessageWindowChatMemory.builder()
        .chatMemoryRepository(chatMemoryRepository())
        .maxMessages(chatbotProperties.getChatHistoryThresholdPerConversation())
        .build();
  }

  @Bean
  ChatClient chatClient(ChatClient.Builder builder, ToolCallbackProvider toolCallbackProvider) {
    return builder
        .defaultToolCallbacks(toolCallbackProvider)
        .defaultSystem(chatbotProperties.getSystemMessage())
        .defaultAdvisors(
            new SimpleLoggerAdvisor(),
            MessageChatMemoryAdvisor.builder(chatMemory())
                .scheduler(Schedulers.boundedElastic())
                .build())
        .build();
  }

  @Bean
  public AzureOpenAIClientBuilderCustomizer configureAzureOpenAIClient() {
    return openAiClientBuilder -> {
      ProxyOptions proxyOptions = null;
      ProxyProperties proxy = chatbotProperties.getProxy();
      if (proxy.isEnabled()) {
        proxyOptions =
            new ProxyOptions(
                ProxyOptions.Type.valueOf(proxy.getProxyType()),
                new InetSocketAddress(proxy.getHost(), proxy.getPort()));
        if (hasLength(proxy.getUsername()) && hasLength(proxy.getPassword())) {
          proxyOptions.setCredentials(proxy.getUsername(), proxy.getPassword());
        }
      }

      HttpClient httpClient =
          HttpClient.create()
              .keepAlive(true)
              .disableRetry(false)
              .compress(true)
              .responseTimeout(chatbotProperties.getReadTimeout());

      // 开启日志
      LogProperties chatClientLogConfig = chatbotProperties.getChatClientLogConfig();
      if (chatClientLogConfig.isEnabled()) {
        httpClient.wiretap(
            chatClientLogConfig.getCategory(),
            chatClientLogConfig.getLogLevel(),
            chatClientLogConfig.getFormat(),
            UTF_8);
      }

      // 忽略ssl证书校验
      if (chatbotProperties.isEnabledInSecureClient()) {
        httpClient.secure(sslSpec -> sslSpec.sslContext(TRUST_ALL_SSL_CONTEXT));
      }

      // 是否启用代理
      NettyAsyncHttpClientBuilder builder = new NettyAsyncHttpClientBuilder(httpClient);
      if (proxy.isEnabled()) {
        builder.proxy(proxyOptions);
      }
      openAiClientBuilder.httpClient(builder.build());
    };
  }

  @Bean
  public List<NamedClientMcpTransport> mcpClientTransport() {
    return mcpSseServerProperties.getConfigs().entrySet().stream()
        .map(
            entry -> {
              String url = entry.getValue().getSseParameters().url();
              String serverName = entry.getKey();
              McpSseServerProperties.Config config =
                  mcpSseServerProperties.getConfigs().get(serverName);
              HttpClient httpClient =
                  HttpClient.create()
                      .keepAlive(true)
                      .baseUrl(url)
                      .responseTimeout(chatbotProperties.getReadTimeout())
                      .disableRetry(false)
                      .compress(true);

              LogProperties mcpClientLogConfig = config.getMcpClientLogConfig();
              if (mcpClientLogConfig.isEnabled()) {
                httpClient.wiretap(
                    mcpClientLogConfig.getCategory(),
                    mcpClientLogConfig.getLogLevel(),
                    mcpClientLogConfig.getFormat(),
                    UTF_8);
              }

              if (url.toLowerCase(Locale.ROOT).startsWith("https")
                  && config.isEnabledInSecureClient()) {
                httpClient.secure(sslSpec -> sslSpec.sslContext(TRUST_ALL_SSL_CONTEXT));
              }

              ProxyProperties configProxy = config.getProxy();
              if (configProxy.isEnabled()) {
                httpClient.proxy(
                    proxy ->
                        proxy
                            .type(Proxy.valueOf(configProxy.getProxyType()))
                            .host(configProxy.getHost())
                            .port(configProxy.getPort())
                            .username(configProxy.getUsername())
                            .password(userName -> configProxy.getPassword()));
              }

              return new NamedClientMcpTransport(
                  serverName,
                  WebFluxSseClientTransport.builder(
                          WebClient.builder()
                              .clientConnector(new ReactorClientHttpConnector(httpClient)))
                      .sseEndpoint(entry.getValue().getSseParameters().sseEndpoint())
                      .objectMapper(new ObjectMapper())
                      .build());
            })
        .toList();
  }

  @Autowired
  public void setMcpSseServerProperties(McpSseServerProperties mcpSseServerProperties) {
    this.mcpSseServerProperties = mcpSseServerProperties;
  }

  @Autowired
  public void setMcpClientCommonProperties(McpClientCommonProperties mcpClientCommonProperties) {
    this.mcpClientCommonProperties = mcpClientCommonProperties;
  }

  @Autowired
  public void setChatbotProperties(ChatbotProperties properties) {
    this.chatbotProperties = properties;
  }
}
