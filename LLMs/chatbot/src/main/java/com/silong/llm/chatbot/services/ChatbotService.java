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

package com.silong.llm.chatbot.services;

import static com.silong.llm.chatbot.daos.RepoHelper.map2User;
import static com.silong.llm.chatbot.mysql.model.enums.ChatbotConversationsStatus.ACTIVE;

import com.silong.foundation.springboot.starter.jwt.security.SimpleTokenAuthentication;
import com.silong.llm.chatbot.daos.ChatbotRepository;
import com.silong.llm.chatbot.pos.Conversation;
import com.silong.llm.chatbot.pos.PagedResult;
import com.silong.llm.chatbot.pos.User;
import java.util.UUID;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 聊天机器人服务
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-19 20:50
 */
@Slf4j
@Service
public class ChatbotService {

  /** 聊天客户端 */
  private final ChatClient chatClient;

  /** 数据库dao */
  private final ChatbotRepository chatbotRepository;

  /**
   * 构造方法
   *
   * @param chatClient 模型客户端
   * @param chatbotRepository 数据库访问
   */
  public ChatbotService(
      @NonNull ChatClient chatClient, @NonNull ChatbotRepository chatbotRepository) {
    this.chatClient = chatClient;
    this.chatbotRepository = chatbotRepository;
  }

  /**
   * 新建会话
   *
   * @param authentication 用户鉴权信息
   * @return 新建会话
   */
  public Mono<Conversation> newConversation(SimpleTokenAuthentication authentication) {
    return Mono.just(authentication)
        .publishOn(Schedulers.boundedElastic())
        .map(
            useless ->
                Conversation.builder()
                    .conversationId(UUID.randomUUID().toString())
                    .title("") // 新建会话没有标题，待第一个问题问出后由模型总结出标题
                    .status(ACTIVE)
                    .build())
        .doOnNext(conversation -> chatbotRepository.newConversation(conversation, authentication));
  }

  /**
   * 分页查询会话信息
   *
   * @param userName 用户名
   * @param marker 上一页最后一条记录的id
   * @param limit 分页记录数
   * @return 分页会话列表
   */
  public Mono<PagedResult<Conversation>> listPagedConversations(
      String userName, int marker, int limit) {
    if (!StringUtils.hasLength(userName)) {
      throw new IllegalArgumentException("userName must not be null or empty.");
    }
    if (marker < 0) {
      throw new IllegalArgumentException("marker must not be negative.");
    }
    if (limit <= 0) {
      throw new IllegalArgumentException("limit must greater than 0.");
    }
    return Mono.empty();
  }

  /**
   * 查询会话详情
   *
   * @param conversationId 会话id
   * @return 会话详情
   */
  public Mono<Conversation> getConversationDetails(String conversationId) {
    if (!StringUtils.hasLength(conversationId)) {
      throw new IllegalArgumentException("conversationId must not be null or empty.");
    }
    return Mono.empty();
  }

  /**
   * 按需创建用户，保证用户与鉴权系统保持一致
   *
   * @param authentication 鉴权结果
   * @return nothing
   */
  public Mono<Void> syncUser(@NonNull SimpleTokenAuthentication authentication) {
    return Mono.just(authentication)
        .publishOn(Schedulers.boundedElastic())
        .doOnNext(
            auth -> {
              User user = map2User(auth);
              log.info("Prepare inserting {} into database.", user);
              chatbotRepository.insertOrUpdateUser(user);
            })
        .then();
  }
}
