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

package com.silong.llm.chatbot.pos;

import com.silong.llm.chatbot.mysql.model.enums.ChatbotConversationsStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户会话信息
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-19 20:50
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User conversation")
public class Conversation {
  /** 会话id */
  @Schema(description = "Conversation ID", example = "12345")
  private int id;

  /** 用户id */
  @Schema(description = "User ID", example = "4532")
  private int userId;

  /** 会话标题 */
  @Schema(description = "Conversation Title", example = "AI chat")
  private String title;

  /** 会话状态，是否激活 */
  @Schema(description = "Conversation status", example = "ACTIVE")
  private ChatbotConversationsStatus status;

  /** 会话聊天消息列表 */
  @Schema(description = "List of conversation dialogue content")
  private List<ChatRound> chatRounds;
}
