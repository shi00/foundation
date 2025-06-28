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

import com.silong.llm.chatbot.mysql.model.enums.ChatbotRolesName;
import com.silong.llm.chatbot.po.PageableResult;
import com.silong.llm.chatbot.po.User;
import java.util.List;
import java.util.Objects;
import lombok.NonNull;
import org.jooq.DSLContext;
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
public class ChatbotUserRepository {

  private final DSLContext dslContext;

  static final byte VALID = 1;
  static final byte INVALID = 0;

  /**
   * 构造方法
   *
   * @param dslContext jooq dsl context
   */
  public ChatbotUserRepository(@NonNull DSLContext dslContext) {
    this.dslContext = dslContext;
  }

  /**
   * 批量插入用户，用户id会更新到入参user对象的id字段
   *
   * @param users 待插入用户
   */
  @Transactional
  public void insert(@NonNull List<User> users) {
    var step = dslContext.insertInto(CHATBOT_USERS).columns(CHATBOT_USERS.NAME, CHATBOT_USERS.DESC);
    for (User user : users) {
      step = step.values(user.getName(), user.getDesc()); // 动态追加值
    }

    // 执行并返回所有插入记录的 ID
    step.returning(CHATBOT_USERS.ID, CHATBOT_USERS.NAME)
        .fetch()
        .forEach(
            r -> {
              for (User user : users) {
                if (r.getName().equals(user.getName())) {
                  user.setId(r.getId());
                }
              }
            }); // 返回包含 ID 的结果集
  }

  /**
   * 插入用户记录
   *
   * @param user 用户
   * @return 用户id
   */
  @Transactional
  public Integer insert(@NonNull User user) {
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
        .where(CHATBOT_ROLES.NAME.eq(ChatbotRolesName.USER))
        .execute();
    user.setId(Objects.requireNonNull(id));
    return id;
  }

  /**
   * 删除用户(逻辑删除)
   *
   * @param userId 用户id
   */
  @Transactional
  public void delete(int userId) {
    dslContext
        .update(CHATBOT_USERS)
        .set(CHATBOT_USERS.VALID, INVALID)
        .where(CHATBOT_USERS.ID.eq(userId))
        .execute();
  }

  /**
   * 删除用户(逻辑删除)
   *
   * @param userName 用户名
   */
  @Transactional
  public void delete(String userName) {
    dslContext
        .update(CHATBOT_USERS)
        .set(CHATBOT_USERS.VALID, INVALID)
        .where(CHATBOT_USERS.NAME.eq(userName))
        .execute();
  }

  /**
   * 分页查询用户
   *
   * @param pageSize 每页结果数量
   * @param pageNumber 页码
   * @return 分页结果
   */
  @Transactional(readOnly = true)
  public PageableResult<User> list(int pageSize, int pageNumber) {

    long total =
        dslContext.selectCount().from(CHATBOT_USERS).where(CHATBOT_USERS.VALID.eq(VALID)).stream()
            .count();

    List<User> users =
        dslContext
            .selectFrom(CHATBOT_USERS)
            .where(CHATBOT_USERS.VALID.eq(VALID))
            .orderBy(CHATBOT_USERS.ID)
            .limit(pageSize)
            .offset((pageNumber - 1) * pageSize)
            .fetchInto(User.class);

    var result = new PageableResult<User>();
    result.setTotalCount((int) total);
    result.setPageResults(users);

    return result;
  }

  /**
   * 查询当前系统中的所有用户
   *
   * @return 用户列表
   */
  @Transactional(readOnly = true)
  public List<User> listAll() {
    return dslContext
        .select()
        .from(CHATBOT_USERS)
        .where(CHATBOT_USERS.VALID.eq(VALID))
        .orderBy(CHATBOT_USERS.ID.asc())
        .fetchInto(User.class);
  }

  /**
   * 查询当前系统中的所有用户
   *
   * @param id 用户id
   * @return 用户列表
   */
  @Transactional(readOnly = true)
  public User get(int id) {
    return dslContext
        .select()
        .from(CHATBOT_USERS)
        .where(CHATBOT_USERS.ID.eq(id).and(CHATBOT_USERS.VALID.eq(VALID)))
        .fetchAnyInto(User.class);
  }

  /**
   * 用户是否存在
   *
   * @param id 用户id
   * @return 用户列表
   */
  @Transactional(readOnly = true)
  public boolean exist(int id) {
    return dslContext.fetchExists(
        CHATBOT_USERS, CHATBOT_USERS.ID.eq(id).and(CHATBOT_USERS.VALID.eq(VALID)));
  }

  /**
   * 用户是否存在
   *
   * @param name 用户名
   * @return 用户列表
   */
  @Transactional(readOnly = true)
  public boolean exist(@NonNull String name) {
    return dslContext.fetchExists(
        CHATBOT_USERS, CHATBOT_USERS.NAME.eq(name).and(CHATBOT_USERS.VALID.eq(VALID)));
  }

  /**
   * 查询当前系统中的所有用户
   *
   * @param name 用户名
   * @return 用户列表
   */
  @Transactional(readOnly = true)
  public User get(@NonNull String name) {
    return dslContext
        .select()
        .from(CHATBOT_USERS)
        .where(CHATBOT_USERS.NAME.eq(name).and(CHATBOT_USERS.VALID.eq(VALID)))
        .fetchAnyInto(User.class);
  }
}
