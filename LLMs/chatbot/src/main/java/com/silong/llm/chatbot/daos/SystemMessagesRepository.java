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

package com.silong.llm.chatbot.daos;

import static com.silong.llm.chatbot.daos.Constants.INVALID;
import static com.silong.llm.chatbot.daos.Constants.VALID;
import static com.silong.llm.chatbot.mysql.model.Tables.*;

import com.silong.foundation.crypto.digest.HmacToolkit;
import com.silong.llm.chatbot.pos.SystemMessage;
import java.util.Objects;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jooq.*;
import org.springframework.beans.factory.annotation.Value;
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
@Slf4j
public class SystemMessagesRepository {

  private static final Condition VALID_SYS_MSG_CONDITION = SYSTEM_MESSAGES.VALID.eq(VALID);

  private final DSLContext dslContext;

  private final String workKey;

  /**
   * 构造方法
   *
   * @param dslContext jooq dsl context
   * @param workKey key
   */
  public SystemMessagesRepository(
      @NonNull DSLContext dslContext, @Value("${crypto.work-key}") String workKey) {
    this.dslContext = dslContext;
    this.workKey = workKey;
  }

  private static void validateSystemMessage(SystemMessage message) {
    if (message == null) {
      throw new IllegalArgumentException("message must not be null.");
    }
    if (message.getContent() == null || message.getContent().isEmpty()) {
      throw new IllegalArgumentException("content must not be null or empty.");
    }
    if (message.getName() == null || message.getName().isEmpty()) {
      throw new IllegalArgumentException("name must not be null or empty.");
    }
  }

  /**
   * 签名
   *
   * @param message 内容
   * @return 签名
   */
  private String sign(String message) {
    return HmacToolkit.hmacSha256(message, workKey);
  }

  /**
   * 插入系统消息
   *
   * @param message 系统消息
   * @return 消息id
   */
  @Transactional
  public Integer insert(SystemMessage message) {
    validateSystemMessage(message);
    Integer id =
        dslContext
            .insertInto(SYSTEM_MESSAGES)
            .set(SYSTEM_MESSAGES.SIGNATURE, sign(message.getContent()))
            .set(SYSTEM_MESSAGES.NAME, message.getName())
            .set(SYSTEM_MESSAGES.DESC, message.getDesc())
            .set(SYSTEM_MESSAGES.CONTENT, message.getContent())
            .returning(SYSTEM_MESSAGES.ID)
            .fetchOne(SYSTEM_MESSAGES.ID);
    message.setId(Objects.requireNonNull(id));
    return id;
  }

  /**
   * 更新系统消息
   *
   * @param message 系统消息
   */
  @Transactional
  public void update(SystemMessage message) {
    validateSystemMessage(message);
    dslContext
        .update(SYSTEM_MESSAGES)
        .set(SYSTEM_MESSAGES.SIGNATURE, sign(message.getContent()))
        .set(SYSTEM_MESSAGES.NAME, message.getName())
        .set(SYSTEM_MESSAGES.CONTENT, message.getContent())
        .where(SYSTEM_MESSAGES.ID.eq(message.getId()).and(VALID_SYS_MSG_CONDITION))
        .execute();
  }

  /**
   * 删除系统信息(逻辑删除)
   *
   * @param id 系统消息id
   */
  @Transactional
  public void delete(int id) {
    if (id <= 0) {
      throw new IllegalArgumentException("id must be greater than 0.");
    }
    dslContext
        .update(SYSTEM_MESSAGES)
        .set(SYSTEM_MESSAGES.VALID, INVALID)
        .where(SYSTEM_MESSAGES.ID.eq(id))
        .execute();
  }

  /**
   * 删除系统消息(逻辑删除)
   *
   * @param content 消息内容
   */
  @Transactional
  public void delete(String content) {
    if (content == null || content.isEmpty()) {
      throw new IllegalArgumentException("content must not be null or empty.");
    }
    dslContext
        .update(SYSTEM_MESSAGES)
        .set(SYSTEM_MESSAGES.VALID, INVALID)
        .where(SYSTEM_MESSAGES.SIGNATURE.eq(sign(content)))
        .execute();
  }

  /**
   * 系统消息是否存在
   *
   * @param id 消息id
   * @return true or false
   */
  @Transactional(readOnly = true)
  public boolean exist(int id) {
    if (id <= 0) {
      throw new IllegalArgumentException("id must be greater than 0.");
    }
    return dslContext.fetchExists(
        SYSTEM_MESSAGES, SYSTEM_MESSAGES.ID.eq(id).and(VALID_SYS_MSG_CONDITION));
  }

  /**
   * 系统消息是否存在
   *
   * @param content 消息内容
   * @return true or false
   */
  @Transactional(readOnly = true)
  public boolean exist(String content) {
    if (content == null || content.isEmpty()) {
      throw new IllegalArgumentException("content must not be null or empty.");
    }
    return dslContext.fetchExists(
        SYSTEM_MESSAGES, SYSTEM_MESSAGES.SIGNATURE.eq(sign(content)).and(VALID_SYS_MSG_CONDITION));
  }
}
