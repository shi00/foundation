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

package com.silong.llm.chatbot.controllers;

import static com.silong.foundation.springboot.starter.jwt.common.Constants.ACCESS_TOKEN;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE;
import static org.springframework.util.StringUtils.hasLength;

import com.silong.foundation.springboot.starter.jwt.common.ErrorDetail;
import com.silong.foundation.springboot.starter.jwt.security.SimpleTokenAuthentication;
import com.silong.llm.chatbot.pos.Conversation;
import com.silong.llm.chatbot.pos.PagedResult;
import com.silong.llm.chatbot.services.ChatbotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

  /** 聊天服务 */
  private final ChatbotService chatbotService;

  /**
   * 构造方法
   *
   * @param chatClient 聊天客户端
   * @param chatbotService 聊天机器人服务
   */
  public ChatbotController(@NonNull ChatClient chatClient, @NonNull ChatbotService chatbotService) {
    this.chatClient = chatClient;
    this.chatbotService = chatbotService;
  }

  /**
   * 创建新会话
   *
   * @return 新会话
   */
  @Operation(
      operationId = "newConversation",
      summary = "Create a conversation with the chatbot for a new user.",
      tags = {"Chat"},
      security = {@SecurityRequirement(name = ACCESS_TOKEN)},
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = Conversation.class))),
        @ApiResponse(
            responseCode = "401",
            description = "UNAUTHORIZED",
            content = @Content(schema = @Schema(implementation = ErrorDetail.class))),
        @ApiResponse(
            responseCode = "403",
            description = "FORBIDDEN",
            content = @Content(schema = @Schema(implementation = ErrorDetail.class))),
        @ApiResponse(
            responseCode = "500",
            description = "INTERNAL_SERVER_ERROR",
            content = @Content(schema = @Schema(implementation = ErrorDetail.class)))
      })
  @PostMapping(value = "/chat/conversations", produces = APPLICATION_JSON_VALUE)
  public Mono<Conversation> newConversation() {
    return ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .cast(SimpleTokenAuthentication.class)
        .map(
            simpleTokenAuthentication ->
                checkAuthorities(simpleTokenAuthentication, "/chat/conversations"))
        .flatMap(chatbotService::newConversation);
  }

  /**
   * 查询会话列表
   *
   * <p>示例：GET /chat/conversations?marker=0&limit=10
   *
   * @param marker 以单页最后一条消息的id作为分页标记
   * @param limit 查询返回消息列表当前页面的数量。
   * @return 会话列表
   */
  @Operation(
      operationId = "listConversations",
      summary = "Paginated query for user conversation list.",
      tags = {"Chat"},
      security = {@SecurityRequirement(name = ACCESS_TOKEN)},
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = PagedResult.class))),
        @ApiResponse(
            responseCode = "401",
            description = "UNAUTHORIZED",
            content = @Content(schema = @Schema(implementation = ErrorDetail.class))),
        @ApiResponse(
            responseCode = "403",
            description = "FORBIDDEN",
            content = @Content(schema = @Schema(implementation = ErrorDetail.class))),
        @ApiResponse(
            responseCode = "500",
            description = "INTERNAL_SERVER_ERROR",
            content = @Content(schema = @Schema(implementation = ErrorDetail.class)))
      })
  @GetMapping(value = "/chat/conversations", produces = APPLICATION_JSON_VALUE)
  public Mono<PagedResult<Conversation>> listConversations(
      @Parameter(
              name = "marker",
              required = true,
              description =
                  "Use the ID of the last message on the single page as the pagination marker.",
              in = ParameterIn.QUERY,
              schema = @Schema(type = "int"))
          @RequestParam
          int marker,
      @Parameter(
              name = "limit",
              required = true,
              description = "Query returns the number of messages on the current page.",
              in = ParameterIn.QUERY,
              schema = @Schema(type = "int"))
          @RequestParam
          int limit) {
    return ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .cast(SimpleTokenAuthentication.class)
        .map(
            simpleTokenAuthentication ->
                checkAuthorities(simpleTokenAuthentication, "/chat/conversations"))
        .map(SimpleTokenAuthentication::getUserName)
        .flatMap(userName -> chatbotService.listPagedConversations(userName, marker, limit));
  }

  /**
   * 根据会话id查询会话详情
   *
   * @param conversationId 会话id
   * @return 会话详情
   */
  @Operation(
      operationId = "getConversationDetail",
      summary = "Query conversation details by conversation ID.",
      tags = {"Chat"},
      security = {@SecurityRequirement(name = ACCESS_TOKEN)},
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = Conversation.class))),
        @ApiResponse(
            responseCode = "401",
            description = "UNAUTHORIZED",
            content = @Content(schema = @Schema(implementation = ErrorDetail.class))),
        @ApiResponse(
            responseCode = "403",
            description = "FORBIDDEN",
            content = @Content(schema = @Schema(implementation = ErrorDetail.class))),
        @ApiResponse(
            responseCode = "500",
            description = "INTERNAL_SERVER_ERROR",
            content = @Content(schema = @Schema(implementation = ErrorDetail.class)))
      })
  @GetMapping(value = "/chat/conversations/{conversation_id}", produces = APPLICATION_JSON_VALUE)
  public Mono<Conversation> getConversationDetail(
      @Parameter(
              name = "conversation_id",
              required = true,
              description = "Conversation ID",
              in = ParameterIn.PATH,
              schema = @Schema(type = "int"))
          @PathVariable("conversation_id")
          int conversationId) {
    return ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .cast(SimpleTokenAuthentication.class)
        .map(
            simpleTokenAuthentication ->
                checkAuthorities(simpleTokenAuthentication, "/chat/conversations"))
        .flatMap(auth -> chatbotService.getConversationDetails(conversationId));
  }

  /**
   * 流式聊天
   *
   * @param query 用户输入
   * @param conversationId 会话id
   * @param exchange Contract for an HTTP request-response interaction
   * @return 大模型返回响应
   */
  @Operation(
      operationId = "chatStream",
      summary = "Chatting with AI in streaming mode.",
      tags = {"Chat"},
      security = {@SecurityRequirement(name = ACCESS_TOKEN)},
      requestBody =
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "User question",
              content =
                  @Content(
                      mediaType = APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = String.class))),
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(
            responseCode = "401",
            description = "UNAUTHORIZED",
            content = @Content(schema = @Schema(implementation = ErrorDetail.class))),
        @ApiResponse(
            responseCode = "403",
            description = "FORBIDDEN",
            content = @Content(schema = @Schema(implementation = ErrorDetail.class))),
        @ApiResponse(
            responseCode = "500",
            description = "INTERNAL_SERVER_ERROR",
            content = @Content(schema = @Schema(implementation = ErrorDetail.class)))
      })
  @PostMapping(value = "/chat/stream", produces = TEXT_EVENT_STREAM_VALUE)
  public Flux<String> chatStream(
      @RequestBody String query,
      @Parameter(
              name = "conversation_id",
              required = true,
              description = "Conversation ID",
              in = ParameterIn.HEADER,
              schema = @Schema(type = "int"))
          @RequestHeader(name = CONVERSATION_ID)
          int conversationId,
      ServerWebExchange exchange) {
    return ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .cast(SimpleTokenAuthentication.class)
        .map(
            simpleTokenAuthentication ->
                checkAuthorities(simpleTokenAuthentication, "/chat/stream")) // 提取Authentication对象
        .map(chatbotService::syncUser)
        .flatMapMany(nothing -> Flux.just(query))
        .doOnNext(
            q -> {
              if (hasLength(q)) {
                log.info("User Query: {}", q);
              } else {
                throw new IllegalArgumentException("User messages must not be null or empty.");
              }
            })
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

  /**
   * 权限校验
   *
   * @param authentication 用户授权
   * @param requestUrl 请求路径
   * @return 正常则返回，否则抛出权限异常
   */
  private SimpleTokenAuthentication checkAuthorities(
      SimpleTokenAuthentication authentication, String requestUrl) {
    // TODO  补充权限校验
    return authentication;
  }
}
