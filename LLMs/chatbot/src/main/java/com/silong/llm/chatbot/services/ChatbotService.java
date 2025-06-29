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
import com.silong.llm.chatbot.daos.ChatbotUserRepository;
import com.silong.llm.chatbot.daos.SystemMessagesRepository;
import com.silong.llm.chatbot.pos.User;
import com.unboundid.ldap.sdk.Attribute;
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
  private final ChatbotUserRepository chatbotUserRepository;

  /** 数据库dao */
  private final SystemMessagesRepository systemMessagesRepository;

  /**
   * 构造方法
   *
   * @param chatClient 模型客户端
   * @param chatbotUserRepository 数据库访问
   * @param systemMessagesRepository 数据库访问
   */
  public ChatbotService(
      @NonNull ChatClient chatClient,
      @NonNull ChatbotUserRepository chatbotUserRepository,
      @NonNull SystemMessagesRepository systemMessagesRepository) {
    this.chatClient = chatClient;
    this.chatbotUserRepository = chatbotUserRepository;
    this.systemMessagesRepository = systemMessagesRepository;
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
              String userName = auth.getUserName();
              Attribute memberOf = auth.getAttribute(MEMBER_OF_ATTRIBUTION);
              Attribute displayName = auth.getAttribute(DISPLAY_NAME_ATTRIBUTION);
              Attribute mobiles = auth.getAttribute(MOBILE_ATTRIBUTION);
              Attribute mails = auth.getAttribute(MAIL_ATTRIBUTION);
              Attribute desc = auth.getAttribute(DESCRIPTION_ATTRIBUTION);
              User user =
                  User.builder()
                      .name(userName)
                      .roles(getValues(memberOf))
                      .displayName(getValue(displayName))
                      .mails(getValues(mails))
                      .desc(getValue(desc))
                      .mobiles(getValues(mobiles))
                      .build();
              chatbotUserRepository.insertOrUpdate(user);
              log.info("User {} was created or updated.", user);
            })
        .then();
  }

  private String[] getValues(Attribute attribute) {
    return attribute == null || !attribute.hasValue() ? null : attribute.getValues();
  }

  private String getValue(Attribute attribute) {
    return attribute == null || !attribute.hasValue() ? null : attribute.getValue();
  }
}
