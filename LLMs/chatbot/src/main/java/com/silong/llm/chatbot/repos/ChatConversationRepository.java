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

package com.silong.llm.chatbot.repos;

import static com.silong.llm.chatbot.mysql.model.Tables.*;

import com.silong.llm.chatbot.po.ChatRound;
import java.util.List;
import lombok.NonNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 数据库访问服务
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-19 20:19
 */
@Repository
public class ChatConversationRepository {

  private final DSLContext dslContext;

  /**
   * 构造方法
   *
   * @param dslContext jooq dsl context
   */
  public ChatConversationRepository(@NonNull DSLContext dslContext) {
    this.dslContext = dslContext;
  }

  @Transactional(readOnly = true)
  public List<ChatRound> getConversationById(@NonNull String id) {
    return dslContext
        .select(
            CHAT_CONVERSATION.ID,
            CHAT_CONVERSATION.ORDER,
            SYSTEM_MESSAGE.MESSAGE,
            PROMPT_MESSAGE.MESSAGE,
            USER_MESSAGE.MESSAGE,
            TOOL_MESSAGE.MESSAGE,
            ASSISTANT_MESSAGE.MESSAGE,
            CHAT_CONVERSATION.CREATED_AT)
        .from(CHAT_CONVERSATION)
        .innerJoin(SYSTEM_MESSAGE)
        .on(CHAT_CONVERSATION.SYSTEM_MSG_ID.eq(SYSTEM_MESSAGE.ID))
        .innerJoin(PROMPT_MESSAGE)
        .on(CHAT_CONVERSATION.PROMPT_MSG_ID.eq(PROMPT_MESSAGE.ID))
        .innerJoin(TOOL_MESSAGE)
        .on(CHAT_CONVERSATION.TOOL_MSG_ID.eq(TOOL_MESSAGE.ID))
        .innerJoin(ASSISTANT_MESSAGE)
        .on(CHAT_CONVERSATION.ASSISTANT_MSG_ID.eq(ASSISTANT_MESSAGE.ID))
        .innerJoin(USER_MESSAGE)
        .on(CHAT_CONVERSATION.USER_MSG_ID.eq(USER_MESSAGE.ID))
        .where(CHAT_CONVERSATION.ID.eq(id))
        .fetchInto(ChatRound.class);
  }
}
