/*
*
•  * Licensed to the Apache Software Foundation (ASF) under one

•  * or more contributor license agreements.  See the NOTICE file

•  * distributed with this work for additional information

•  * regarding copyright ownership.  The ASF licenses this file

•  * to you under the Apache License, Version 2.0 (the

•  * "License"); you may not use this file except in compliance

•  * with the License.  You may obtain a copy of the License at

•  *

•  *      http://www.apache.org/licenses/LICENSE-2.0

•  *

•  * Unless required by applicable law or agreed to in writing,

•  * software distributed under the License is distributed on an

•  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY

•  * KIND, either express or implied.  See the License for the

•  * specific language governing permissions and limitations

•  * under the License.

*
*/

package com.silong.llm.chatbot.daos;

import static com.silong.llm.chatbot.daos.RepoHelper.*;
import static com.silong.llm.chatbot.mysql.model.Tables.*;
import static com.silong.llm.chatbot.mysql.model.enums.ChatbotConversationsStatus.ACTIVE;
import static com.silong.llm.chatbot.mysql.model.enums.ChatbotConversationsStatus.INACTIVE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.silong.llm.chatbot.pos.Conversation;
import com.silong.llm.chatbot.pos.Role;
import com.silong.llm.chatbot.pos.User;
import jakarta.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 数据库服务
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-19 20:19
 */
@Repository
@Slf4j
public class ChatbotRepository {

  static final Condition VALID_ROLE_CONDITION = CHATBOT_ROLES.VALID.eq(VALID);

  static final Condition VALID_CONVERSATIONS_CONDITION = CHATBOT_CONVERSATIONS.VALID.eq(VALID);

  static final Condition VALID_USER_CONDITION = CHATBOT_USERS.VALID.eq(VALID);

  private final DSLContext dslContext;

  private final ObjectMapper objectMapper;

  /**
   * • 构造方法
   *
   * @param dslContext jooq dsl context
   * @param objectMapper jackson
   */
  public ChatbotRepository(@NonNull DSLContext dslContext, @NonNull ObjectMapper objectMapper) {
    this.dslContext = dslContext;
    this.objectMapper = objectMapper;
  }

  @SneakyThrows({JsonProcessingException.class})
  private <T> T parseJson(String json, Class<T> type) {
    return objectMapper.readValue(json, type);
  }

  @SneakyThrows(JsonProcessingException.class)
  private JSON conver2Json(Object json) {
    return JSON.valueOf(objectMapper.writeValueAsString(json));
  }

  private static void validateConversation(Conversation conversation) {
    if (conversation == null) {
      throw new IllegalArgumentException("conversation must not be null.");
    }

    if (conversation.getStatus() == null) {
      throw new IllegalArgumentException("conversationStatus must not be null.");
    }
  }

  private static void validateUser(User user) {
    if (user == null) {
      throw new IllegalArgumentException("user must not be null.");
    }

    String userName = user.getName();
    if (userName == null || userName.isEmpty()) {
      throw new IllegalArgumentException("userName must not be null or empty.");
    }

    List<String> roles = user.getRoles();
    if (roles == null
        || roles.isEmpty()
        || roles.stream().anyMatch(role -> role == null || role.isEmpty())) {
      throw new IllegalArgumentException("userRoles must not be null or empty.");
    }
  }

  private static void validateRole(Role role) {
    if (role == null) {
      throw new IllegalArgumentException("role must not be null.");
    }

    String name = role.getName();
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("roleName must not be null or empty.");
    }
  }

  private static <T> void insertSuccessfully(T t) {
    log.info("Successfully inserted {} into database.", t);
  }

  /**
   * 根据用户名查询用户id
   *
   * @param userName 用户名
   * @return 用户id
   */
  @Nullable
  @Transactional(readOnly = true)
  public Integer getUserIdByName(String userName) {
    if (userName == null || userName.isEmpty()) {
      throw new IllegalArgumentException("userName must not be null or empty.");
    }
    return dslContext
        .select(CHATBOT_USERS.ID)
        .from(CHATBOT_USERS)
        .where(CHATBOT_USERS.NAME.eq(userName).and(VALID_USER_CONDITION))
        .fetchOne(CHATBOT_USERS.ID);
  }

  /**
   * 新建会话记录，每个用户只能有一个激活会话
   *
   * @param conversation 会话
   * @param user 用户
   */
  @Transactional
  public void newConversation(Conversation conversation, User user) {
    validateConversation(conversation);
    insertOrUpdateUser(user);
    conversation.setUserId(user.getId());

    // 每个用户只有一个激活的会话
    dslContext
        .update(CHATBOT_CONVERSATIONS)
        .set(CHATBOT_CONVERSATIONS.STATUS, INACTIVE)
        .where(
            CHATBOT_CONVERSATIONS
                .USER_ID
                .eq(user.getId())
                .and(CHATBOT_CONVERSATIONS.STATUS.eq(ACTIVE))
                .and(VALID_CONVERSATIONS_CONDITION))
        .execute();

    Integer id =
        dslContext
            .insertInto(CHATBOT_CONVERSATIONS)
            .set(CHATBOT_CONVERSATIONS.STATUS, conversation.getStatus())
            .set(CHATBOT_CONVERSATIONS.USER_ID, conversation.getUserId())
            .set(CHATBOT_CONVERSATIONS.TITLE, conversation.getTitle())
            .returning(CHATBOT_CONVERSATIONS.ID)
            .fetchOne(CHATBOT_CONVERSATIONS.ID);
    conversation.setId(Objects.requireNonNull(id));
    insertSuccessfully(conversation);
  }

  /**
   * 用户是否存在
   *
   * @param name 用户名
   * @return 用户列表
   */
  @Transactional(readOnly = true)
  public boolean existRole(String name) {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("roleName must not be null or empty.");
    }
    return dslContext.fetchExists(
        CHATBOT_ROLES, CHATBOT_ROLES.NAME.eq(name).and(VALID_ROLE_CONDITION));
  }

  /**
   * 插入角色
   *
   * @param role 角色
   */
  @Transactional
  public void insertRole(Role role) {
    validateRole(role);
    Collection<String> authorizedPaths = role.getAuthorizedPaths();
    Collection<Integer> members = role.getMembers();
    Integer id =
        dslContext
            .insertInto(CHATBOT_ROLES)
            .set(CHATBOT_ROLES.DESC, role.getDesc())
            .set(
                CHATBOT_ROLES.AUTHORIZED_PATHS,
                conver2Json(authorizedPaths == null ? List.of() : authorizedPaths))
            .set(CHATBOT_ROLES.USER_IDS, conver2Json(members == null ? List.of() : members))
            .set(CHATBOT_ROLES.NAME, role.getName())
            .returning(CHATBOT_ROLES.ID)
            .fetchOne(CHATBOT_ROLES.ID);

    role.setId(Objects.requireNonNull(id));

    insertSuccessfully(role);
  }

  /**
   * 根据用户组名称查询用户id
   *
   * @param name 用户组名
   * @return 用户组id
   */
  @Nullable
  @Transactional(readOnly = true)
  public Integer getRoleIdByName(String name) {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("roleName must not be null or empty.");
    }
    return dslContext
        .select(CHATBOT_ROLES.ID)
        .from(CHATBOT_ROLES)
        .where(CHATBOT_ROLES.NAME.eq(name).and(VALID_ROLE_CONDITION))
        .fetchOne(CHATBOT_ROLES.ID);
  }

  /**
   * 查询当前系统中的所有用户
   *
   * @param name 用户名
   * @return 用户列表
   */
  @Nullable
  @Transactional(readOnly = true)
  public Role getRole(String name) {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("roleName" + " must not be null or empty.");
    }
    return dslContext
        .select(
            CHATBOT_ROLES.ID,
            CHATBOT_ROLES.NAME,
            CHATBOT_ROLES.USER_IDS,
            CHATBOT_ROLES.AUTHORIZED_PATHS,
            CHATBOT_ROLES.DESC)
        .from(CHATBOT_ROLES)
        .where(CHATBOT_ROLES.NAME.eq(name).and(VALID_ROLE_CONDITION))
        .fetchOne(
            record ->
                Role.builder()
                    .id(record.get(CHATBOT_ROLES.ID))
                    .desc(record.get(CHATBOT_ROLES.DESC))
                    .name(record.get(CHATBOT_ROLES.NAME))
                    .members(
                        Arrays.stream(
                                parseJson(record.get(CHATBOT_ROLES.USER_IDS).data(), int[].class))
                            .boxed()
                            .collect(Collectors.toUnmodifiableSet()))
                    .authorizedPaths(
                        Arrays.stream(
                                parseJson(
                                    record.get(CHATBOT_ROLES.AUTHORIZED_PATHS).data(),
                                    String[].class))
                            .collect(Collectors.toUnmodifiableSet()))
                    .build());
  }

  /**
   * 插入用户记录
   *
   * @param user 用户
   */
  @Transactional
  @SneakyThrows({JsonProcessingException.class})
  public void insertOrUpdateUser(User user) {
    validateUser(user);
    var mobiles =
        user.getMobiles() == null || user.getMobiles().isEmpty()
            ? null
            : String.join(DELIMITER, user.getMobiles());
    var mails =
        user.getMails() == null || user.getMails().isEmpty()
            ? null
            : String.join(DELIMITER, user.getMails());
    var username = user.getName();
    var desc = user.getDesc();
    var displayName = user.getDisplayName();

    Integer id =
        dslContext
            .insertInto(CHATBOT_USERS)
            .set(CHATBOT_USERS.NAME, username)
            .set(CHATBOT_USERS.MOBILES, mobiles)
            .set(CHATBOT_USERS.DESC, desc)
            .set(CHATBOT_USERS.MAILS, mails)
            .set(CHATBOT_USERS.DISPLAY_NAME, displayName)
            .onDuplicateKeyUpdate()
            .set(CHATBOT_USERS.DISPLAY_NAME, displayName)
            .set(CHATBOT_USERS.MOBILES, mobiles)
            .set(CHATBOT_USERS.DESC, desc)
            .set(CHATBOT_USERS.MAILS, mails)
            .returning(CHATBOT_USERS.ID) // 指定返回的字段
            .fetchOne(CHATBOT_USERS.ID);

    if (id != null) {
      user.setId(id);
    } else {
      // mysql执行更新操作时不会返回主键
      id = Objects.requireNonNull(getUserIdByName(username));
      user.setId(id);
    }

    // 插入或更新用户所属的用户组信息
    for (var roleJson : user.getRoles()) {
      var role = objectMapper.readValue(roleJson, Role.class);
      if (existRole(role.getName())) {
        appendUserId2Role(id, role);
      } else {
        role.setMembers(List.of(id));
        insertRole(role);
      }
    }
    insertSuccessfully(user);
  }

  /**
   * 从角色中移除用户id
   *
   * @param userId 用户id
   * @param role 角色
   */
  @Transactional
  public void removeUserIdFromRole(Integer userId, Role role) {
    if (userId == null || userId <= 0) {
      throw new IllegalArgumentException("userId must be a positive number");
    }
    validateRole(role);
    dslContext
        .update(CHATBOT_ROLES)
        .set(
            CHATBOT_ROLES.USER_IDS,
            DSL.field(
                "(SELECT JSON_ARRAYAGG(id) FROM JSON_TABLE({0}, '$[*]' COLUMNS (id INT PATH '$')) AS t WHERE id != {1})",
                JSON.class, CHATBOT_ROLES.USER_IDS, DSL.val(userId)))
        .where(
            DSL.condition(
                "JSON_CONTAINS({0}, CAST({1} AS JSON))", CHATBOT_ROLES.USER_IDS, DSL.val(userId)))
        .execute();

    log.info("Removing userId:{} from the userIds field of {}.", userId, role.getName());
  }

  /**
   * 用户添加到用户组
   *
   * @param userId 用户id
   * @param role 角色
   */
  @Transactional
  public void appendUserId2Role(Integer userId, Role role) {
    if (userId == null || userId <= 0) {
      throw new IllegalArgumentException("userId must be a positive number");
    }
    validateRole(role);

    String roleName = role.getName();
    dslContext
        .update(CHATBOT_ROLES)
        .set(CHATBOT_ROLES.DESC, role.getDesc())
        .set(
            CHATBOT_ROLES.USER_IDS,
            DSL.when(
                    DSL.condition(
                        "JSON_CONTAINS(COALESCE({0}, '[]'), cast({1} as JSON)) = 0",
                        CHATBOT_ROLES.USER_IDS, DSL.inline(userId) // 直接传递数值类型
                        ),
                    DSL.field(
                        "JSON_ARRAY_APPEND({0}, '$', cast({1} as JSON))",
                        CHATBOT_ROLES.USER_IDS.getDataType(),
                        CHATBOT_ROLES.USER_IDS,
                        DSL.inline(userId) // 直接传递数值类型
                        ))
                .otherwise(CHATBOT_ROLES.USER_IDS))
        .where(CHATBOT_ROLES.NAME.eq(roleName))
        .execute();

    log.info("Appending userId:{} into the user_ids of {} in database.", userId, roleName);
  }

  /**
   * 删除用户(逻辑删除)
   *
   * @param id 用户id
   */
  @Transactional
  public void deleteUser(int id) {
    if (id <= 0) {
      throw new IllegalArgumentException("userId must be greater than 0.");
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
  public void deleteUser(String name) {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("userName must not be null or empty.");
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
  public boolean existUser(int id) {
    if (id <= 0) {
      throw new IllegalArgumentException("userId must be greater than 0.");
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
  public boolean existUser(String name) {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("userName must not be null or empty.");
    }
    return dslContext.fetchExists(
        CHATBOT_USERS, CHATBOT_USERS.NAME.eq(name).and(VALID_USER_CONDITION));
  }
}
