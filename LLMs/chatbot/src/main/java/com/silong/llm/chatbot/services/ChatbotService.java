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

import static com.silong.llm.chatbot.providers.LdapUserProvider.*;

import com.silong.foundation.springboot.starter.jwt.security.SimpleTokenAuthentication;
import com.silong.llm.chatbot.daos.ChatbotRepository;
import com.silong.llm.chatbot.pos.User;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
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
              List<String> memberOf = auth.getAttribute(MEMBER_OF_ATTRIBUTION);
              List<String> displayName = auth.getAttribute(DISPLAY_NAME_ATTRIBUTION);
              List<String> mobiles = auth.getAttribute(MOBILE_ATTRIBUTION);
              List<String> mails = auth.getAttribute(MAIL_ATTRIBUTION);
              List<String> desc = auth.getAttribute(DESCRIPTION_ATTRIBUTION);
              User user =
                  User.builder()
                      .name(auth.getUserName())
                      .roles(memberOf)
                      .mobiles(mobiles)
                      .mails(mails)
                      .displayName(displayName == null ? null : displayName.getFirst())
                      .desc(desc == null ? null : desc.getFirst())
                      .build();
              chatbotRepository.insertOrUpdateUser(user);
            })
        .then();
  }
}
