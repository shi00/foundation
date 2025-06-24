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

package com.silong.llm.chatbot.controller;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.util.StringUtils.hasLength;

import com.silong.llm.chatbot.po.ChatRound;
import java.util.List;
import java.util.UUID;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;

/**
 * 聊天机器人控制器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-19 20:50
 */
@Slf4j
@RestController
public class ChatbotController {

  private static final String CONVERSATION_ID = "Conversation-ID";

  /** 聊天客户端 */
  private final ChatClient chatClient;

  /**
   * 构造方法
   *
   * @param chatClient 聊天客户端
   */
  public ChatbotController(@NonNull ChatClient chatClient) {
    this.chatClient = chatClient;
  }

  /**
   * 查询会话列表
   *
   * @param pageIndex 页码
   * @param sizePerPage 每页包含的记录数
   * @return 会话列表
   */
  @GetMapping(value = "/chat/history", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<ChatRound> listConversations(
      @RequestParam int pageIndex, @RequestParam int sizePerPage) {
    return List.of();
  }

  /**
   * 流式聊天
   *
   * @param query 用户输入
   * @param conversationId 会话id
   * @param exchange Contract for an HTTP request-response interaction
   * @return 大模型返回响应
   */
  @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<String> chatStream(
      @RequestBody String query,
      @RequestHeader(name = CONVERSATION_ID, required = false) String conversationId,
      ServerWebExchange exchange) {
    return Flux.just(query)
        .doOnNext(
            q -> {
              if (hasLength(q)) {
                log.info("User Query: {}", q);
              } else {
                throw new IllegalArgumentException("User messages must not be null or empty.");
              }
            })
        .map(
            q ->
                hasLength(conversationId)
                    ? conversationId
                    : UUID.randomUUID().toString().replace("-", ""))
        .doOnNext(id -> exchange.getResponse().getHeaders().add(CONVERSATION_ID, id))
        .flatMap(
            id ->
                chatClient
                    .prompt()
                    .system(spec -> spec.param("field", "in many fields"))
                    .user(query)
                    .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, id))
                    .stream()
                    .content()
                    .doOnNext(text -> log.info("Assistant response: {} --- {}", id, text))
                    .doOnComplete(
                        () -> log.info("Assistant response finish with conversationId: {}.", id)))
        .onErrorResume(
            t ->
                switch (t) {
                  case IllegalArgumentException e -> {
                    log.error("BadRequest with conversationId: {}", conversationId, e);
                    exchange.getResponse().setStatusCode(BAD_REQUEST);
                    yield Flux.empty();
                  }
                  default -> {
                    log.error(
                        "Exception with query: {} and conversationId: {}",
                        query,
                        conversationId,
                        t);
                    exchange.getResponse().setStatusCode(INTERNAL_SERVER_ERROR);
                    yield Flux.empty();
                  }
                });
  }
}
