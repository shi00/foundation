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

import static org.springframework.util.StringUtils.hasLength;

import com.azure.core.http.ProxyOptions;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.silong.llm.chatbot.configure.properties.AzureOpenAIProxyProperties;
import com.silong.llm.chatbot.configure.properties.ChatbotProperties;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.net.InetSocketAddress;
import javax.net.ssl.SSLException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.model.azure.openai.autoconfigure.AzureOpenAIClientBuilderCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.netty.http.client.HttpClient;

/**
 * 聊天助手自动配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-19 20:21
 */
@Configuration
@EnableConfigurationProperties(ChatbotProperties.class)
public class ChatbotAutoConfiguration {

  private ChatbotProperties properties;

  @Bean
  ChatMemoryRepository chatMemoryRepository() {
    return new InMemoryChatMemoryRepository();
  }

  @Bean
  ChatMemory chatMemory() {
    return MessageWindowChatMemory.builder()
        .chatMemoryRepository(chatMemoryRepository())
        .maxMessages(properties.getChatHistoryThresholdPerConversation())
        .build();
  }

  @Bean
  ChatClient chatClient(ChatClient.Builder builder) {
    return builder
        .defaultSystem(properties.getSystemMessage())
        .defaultSystem(spec -> spec.text(properties.getSystemMessage()))
        //        .defaultAdvisors(advisorSpec -> advisorSpec.advisors())
        .build();
  }

  @Bean
  public AzureOpenAIClientBuilderCustomizer configureAzureOpenAIClient() {
    return openAiClientBuilder -> {
      ProxyOptions proxyOptions = null;
      AzureOpenAIProxyProperties proxy = properties.getProxy();
      if (hasLength(proxy.getHost()) && proxy.getPort() != null) {
        proxyOptions =
            new ProxyOptions(
                proxy.getProxyType(), new InetSocketAddress(proxy.getHost(), proxy.getPort()));
        if (hasLength(proxy.getUsername()) && hasLength(proxy.getPassword())) {
          proxyOptions.setCredentials(proxy.getUsername(), proxy.getPassword());
        }
      }

      if (properties.isEnabledInSecureClient()) {
        try {
          // 创建一个接受所有证书的SSL上下文
          SslContext sslContext =
              SslContextBuilder.forClient()
                  .trustManager(InsecureTrustManagerFactory.INSTANCE)
                  .build();

          NettyAsyncHttpClientBuilder builder =
              new NettyAsyncHttpClientBuilder(
                  HttpClient.create()
                      .keepAlive(true)
                      .responseTimeout(properties.getReadTimeout())
                      .secure(sslSpec -> sslSpec.sslContext(sslContext)));
          if (proxyOptions != null) {
            builder.proxy(proxyOptions);
          }
          openAiClientBuilder.httpClient(builder.build());
        } catch (SSLException e) {
          throw new RuntimeException(e);
        }
      }
    };
  }

  @Autowired
  public void setChatbotProperties(ChatbotProperties properties) {
    this.properties = properties;
  }
}
