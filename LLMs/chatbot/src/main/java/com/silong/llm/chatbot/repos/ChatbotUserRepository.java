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

import static com.silong.llm.chatbot.mysql.model.Tables.CHATBOT_ROLES;
import static com.silong.llm.chatbot.mysql.model.Tables.CHATBOT_USERS;
import static com.silong.llm.chatbot.repos.Constants.INVALID;
import static com.silong.llm.chatbot.repos.Constants.VALID;

import com.silong.llm.chatbot.po.PageableResult;
import com.silong.llm.chatbot.po.User;
import com.silong.llm.chatbot.repos.Constants.Role;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jooq.*;
import org.jooq.impl.DSL;
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
public class ChatbotUserRepository {

  private static final RecordMapper<Record4<Integer, String, String, String>, User>
      USER_RECORD_MAPPER =
          record ->
              User.builder()
                  .id(record.get(CHATBOT_USERS.ID))
                  .desc(record.get(CHATBOT_USERS.DESC))
                  .name(record.get(CHATBOT_USERS.NAME))
                  .role(Role.fromString(record.get(CHATBOT_ROLES.NAME)))
                  .build();

  private static final Condition USER_ID_IN_ROLES_CONDITION =
      DSL.condition("? MEMBER OF({0})", DSL.val(CHATBOT_USERS.ID), CHATBOT_ROLES.USER_IDS);

  private static final Condition VALID_USER_CONDITION = CHATBOT_USERS.VALID.eq(VALID);

  private static final Condition VALID_ROLE_CONDITION = CHATBOT_ROLES.VALID.eq(VALID);

  private final DSLContext dslContext;

  /**
   * 构造方法
   *
   * @param dslContext jooq dsl context
   */
  public ChatbotUserRepository(@NonNull DSLContext dslContext) {
    this.dslContext = dslContext;
  }

  /**
   * 插入用户记录
   *
   * @param user 用户
   * @return 用户id
   */
  @Transactional
  public Integer insert(User user) {
    if (user == null) {
      throw new IllegalArgumentException("user must not be null.");
    }
    Integer id =
        dslContext
            .insertInto(CHATBOT_USERS)
            .set(CHATBOT_USERS.NAME, user.getName())
            .set(CHATBOT_USERS.DESC, user.getDesc())
            .returning(CHATBOT_USERS.ID) // 指定返回的字段
            .fetchOne(CHATBOT_USERS.ID);
    dslContext
        .update(CHATBOT_ROLES)
        .set(
            CHATBOT_ROLES.USER_IDS,
            DSL.field(
                "JSON_ARRAY_APPEND({0}, '$', {1})",
                CHATBOT_ROLES.USER_IDS.getDataType(), // 保持类型安全
                DSL.val(id)))
        .where(CHATBOT_ROLES.NAME.eq(user.getRole().getValue()))
        .execute();
    user.setId(Objects.requireNonNull(id));
    return id;
  }

  /**
   * 删除用户(逻辑删除)
   *
   * @param id 用户id
   */
  @Transactional
  public void delete(int id) {
    if (id <= 0) {
      throw new IllegalArgumentException("id must be greater than 0.");
    }
    dslContext
        .update(CHATBOT_USERS)
        .set(CHATBOT_USERS.VALID, INVALID)
        .where(CHATBOT_USERS.ID.eq(id))
        .execute();

    dslContext
        .update(CHATBOT_ROLES)
        .set(
            CHATBOT_ROLES.USER_IDS,
            DSL.field(
                "JSON_REMOVE({0}, JSON_UNQUOTE(JSON_SEARCH({0}, 'one', {1})))",
                JSON.class,
                CHATBOT_ROLES.USER_IDS, // {0}: JSON 数组字段
                DSL.val(id) // {1}: 目标整数值
                ))
        .where(DSL.condition("? MEMBER OF({0})", DSL.val(id), CHATBOT_ROLES.USER_IDS))
        .execute();
  }

  /**
   * 删除用户(逻辑删除)
   *
   * @param name 用户名
   */
  @Transactional
  public void delete(String name) {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("name must not be null or empty.");
    }
    Integer id =
        dslContext
            .update(CHATBOT_USERS)
            .set(CHATBOT_USERS.VALID, INVALID)
            .where(CHATBOT_USERS.NAME.eq(name))
            .returning(CHATBOT_USERS.ID)
            .fetchOne(CHATBOT_USERS.ID);

    dslContext
        .update(CHATBOT_ROLES)
        .set(
            CHATBOT_ROLES.USER_IDS,
            DSL.field(
                "JSON_REMOVE({0}, JSON_UNQUOTE(JSON_SEARCH({0}, 'one', {1})))",
                JSON.class,
                CHATBOT_ROLES.USER_IDS, // {0}: JSON 数组字段
                DSL.val(id) // {1}: 目标整数值
                ))
        .where(DSL.condition("? MEMBER OF({0})", DSL.val(id), CHATBOT_ROLES.USER_IDS))
        .execute();
  }

  /**
   * 用户是否存在
   *
   * @param id 用户id
   * @return 用户列表
   */
  @Transactional(readOnly = true)
  public boolean exist(int id) {
    if (id <= 0) {
      throw new IllegalArgumentException("id must be greater than 0.");
    }
    return dslContext.fetchExists(CHATBOT_USERS, CHATBOT_USERS.ID.eq(id).and(VALID_USER_CONDITION));
  }

  /**
   * 用户是否存在
   *
   * @param name 用户名
   * @return 用户列表
   */
  @Transactional(readOnly = true)
  public boolean exist(String name) {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("name must not be null or empty.");
    }
    return dslContext.fetchExists(
        CHATBOT_USERS, CHATBOT_USERS.NAME.eq(name).and(VALID_USER_CONDITION));
  }

  /**
   * 查询当前系统中的所有用户
   *
   * @param name 用户名
   * @return 用户列表
   */
  @Transactional(readOnly = true)
  public User get(String name) {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("name must not be null or empty.");
    }
    return dslContext
        .select(CHATBOT_USERS.ID, CHATBOT_USERS.NAME, CHATBOT_ROLES.NAME, CHATBOT_USERS.DESC)
        .from(CHATBOT_USERS, CHATBOT_ROLES)
        .where(
            CHATBOT_USERS
                .NAME
                .eq(name)
                .and(CHATBOT_USERS.VALID.eq(VALID))
                .and(USER_ID_IN_ROLES_CONDITION)
                .and(CHATBOT_ROLES.VALID.eq(VALID)))
        .fetchOne(USER_RECORD_MAPPER);
  }

  /**
   * 查询当前系统中的所有用户
   *
   * @param id 用户id
   * @return 用户列表
   */
  @Transactional(readOnly = true)
  public User get(int id) {
    if (id <= 0) {
      throw new IllegalArgumentException("id must be greater than 0.");
    }
    return dslContext
        .select(CHATBOT_USERS.ID, CHATBOT_USERS.NAME, CHATBOT_ROLES.NAME, CHATBOT_USERS.DESC)
        .from(CHATBOT_USERS, CHATBOT_ROLES)
        .where(
            CHATBOT_USERS
                .ID
                .eq(id)
                .and(VALID_USER_CONDITION)
                .and(DSL.condition("? MEMBER OF({0})", DSL.val(id), CHATBOT_ROLES.USER_IDS))
                .and(VALID_ROLE_CONDITION))
        .fetchOne(USER_RECORD_MAPPER);
  }

  /**
   * 查询当前系统中的所有有效用户
   *
   * @return 用户列表
   */
  @Transactional(readOnly = true)
  public PageableResult<User> listAll() {
    int total = dslContext.selectCount().from(CHATBOT_USERS).where(VALID_USER_CONDITION).execute();

    List<User> users =
        dslContext
            .select(CHATBOT_USERS.ID, CHATBOT_USERS.NAME, CHATBOT_ROLES.NAME, CHATBOT_USERS.DESC)
            .from(CHATBOT_USERS, CHATBOT_ROLES)
            .where(VALID_USER_CONDITION.and(USER_ID_IN_ROLES_CONDITION).and(VALID_ROLE_CONDITION))
            .orderBy(CHATBOT_USERS.ID.asc())
            .fetch(USER_RECORD_MAPPER);
    return PageableResult.<User>builder().pageResults(users).totalCount(total).build();
  }

  /**
   * 分页查询用户
   *
   * @param pageSize 每页结果数量
   * @param pageNumber 页码
   * @param usersCondition 用户查询条件
   * @param rolesCondition 角色查询条件
   * @return 分页结果
   */
  @Transactional(readOnly = true)
  public PageableResult<User> list(
      int pageSize,
      int pageNumber,
      @Nullable Condition usersCondition,
      @Nullable Condition rolesCondition) {
    if (pageSize <= 0) {
      throw new IllegalArgumentException("pageSize must be greater than 0.");
    }

    if (pageNumber <= 0) {
      throw new IllegalArgumentException("pageNumber must be greater than 0.");
    }

    if (usersCondition == null) {
      usersCondition = VALID_USER_CONDITION;
    }

    if (rolesCondition == null) {
      rolesCondition = VALID_ROLE_CONDITION;
    }

    int total = dslContext.selectCount().from(CHATBOT_USERS).where(usersCondition).execute();

    List<User> users =
        dslContext
            .select(CHATBOT_USERS.ID, CHATBOT_USERS.NAME, CHATBOT_ROLES.NAME, CHATBOT_USERS.DESC)
            .from(CHATBOT_USERS, CHATBOT_ROLES)
            .where(usersCondition.and(USER_ID_IN_ROLES_CONDITION).and(rolesCondition))
            .orderBy(CHATBOT_USERS.ID.asc())
            .limit(pageSize)
            .offset((pageNumber - 1) * pageSize)
            .fetch(USER_RECORD_MAPPER);
    return PageableResult.<User>builder().pageResults(users).totalCount(total).build();
  }
}
