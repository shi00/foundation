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

import static com.silong.llm.chatbot.mysql.model.Tables.CHAT_CONVERSATION;

import com.silong.llm.chatbot.mysql.model.tables.records.ChatConversationRecord;
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
  public List<ChatConversationRecord> getConversationById(@NonNull String id) {
    return dslContext.selectFrom(CHAT_CONVERSATION).where(CHAT_CONVERSATION.ID.eq(id)).stream()
        .toList();
  }
}
