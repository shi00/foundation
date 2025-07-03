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

package com.silong.llm.chatbot.providers;

import static com.silong.llm.chatbot.configure.properties.LdapProperties.Type.ACTIVE_DIRECTORY;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.silong.foundation.springboot.starter.jwt.common.Credentials;
import com.silong.foundation.springboot.starter.jwt.exception.IllegalUserException;
import com.silong.foundation.springboot.starter.jwt.provider.UserAuthenticationProvider;
import com.silong.llm.chatbot.configure.properties.LdapProperties;
import com.silong.llm.chatbot.pos.Role;
import com.unboundid.ldap.sdk.*;
import jakarta.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.*;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * 基于LDAP实现的用户提供者
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-19 20:21
 */
@Slf4j
public class LdapUserProvider implements UserAuthenticationProvider {

  /** 邮箱地址属性 */
  public static final String MAIL_ATTRIBUTION = "mail";

  /** 移动电话号码属性 */
  public static final String MOBILE_ATTRIBUTION = "mobile";

  /** 显示名属性 */
  public static final String DISPLAY_NAME_ATTRIBUTION = "displayName";

  /** 归属属性 */
  public static final String MEMBER_OF_ATTRIBUTION = "memberOf";

  /** 描述信息 */
  public static final String DESCRIPTION_ATTRIBUTION = "description";

  /** 描通用名 */
  public static final String CN_ATTRIBUTION = "cn";

  /** 用户属性名，用于查询 */
  private static final String[] USER_ATTRS = {
    MAIL_ATTRIBUTION,
    MOBILE_ATTRIBUTION,
    DISPLAY_NAME_ATTRIBUTION,
    MEMBER_OF_ATTRIBUTION,
    DESCRIPTION_ATTRIBUTION
  };

  private final LdapProperties ldapProperties;

  private final LDAPConnectionPool connectionPool;

  private final ObjectMapper objectMapper;

  /**
   * 构造方法
   *
   * @param ldapProperties 相关配置
   * @param connectionPool 连接池
   * @param objectMapper jackson
   */
  public LdapUserProvider(
      @NonNull LdapProperties ldapProperties,
      @NonNull LDAPConnectionPool connectionPool,
      @NonNull ObjectMapper objectMapper) {
    this.ldapProperties = ldapProperties;
    this.connectionPool = connectionPool;
    this.objectMapper = objectMapper;
  }

  @Override
  public void checkUserExists(@NonNull String userName) {
    try (var conn = connectionPool.getConnection()) {
      if (ldapProperties.getType() == ACTIVE_DIRECTORY) {
        throw new UnsupportedOperationException();
      } else {
        searchUserResultEntry(userName, conn);
      }
    } catch (LDAPException e) {
      log.error("User {} does not exist.", userName);
      throw new IllegalUserException(String.format("User %s does not exist.", userName));
    }
  }

  @Override
  public Map<String, Object> authenticate(@NonNull Credentials credentials) {
    String userName = credentials.getUserName();
    String password = credentials.getPassword();
    try (LDAPConnection conn = connectionPool.getConnection()) {
      if (ldapProperties.getType() == ACTIVE_DIRECTORY) {
        throw new UnsupportedOperationException();
      } else {
        SearchResultEntry entry = searchUserResultEntry(userName, conn);
        BindResult result = conn.bind(entry.getDN(), password);
        if (result.getResultCode() != ResultCode.SUCCESS) {
          log.warn("Failed to bind user {}.", userName);
          throw new IllegalUserException(String.format("%s authentication failed.", userName));
        }
        log.info("User {} authenticated.", userName);
        Map<String, Object> map = new HashMap<>();
        for (String attrKey : USER_ATTRS) {
          Attribute attribute = entry.getAttribute(attrKey);
          if (attribute != null && attribute.hasValue()) {
            log.info(attribute.toString());
            map.put(attrKey, convert2StringList(userName, conn, attrKey, attribute));
          }
        }
        return map;
      }
    } catch (LDAPException e) {
      log.error("Failed to authenticate user {}.", userName, e);
      throw new IllegalUserException(String.format("%s authentication failed.", userName));
    }
  }

  @SneakyThrows(JsonProcessingException.class)
  private List<String> convert2StringList(
      String userName, LDAPConnection conn, String attrKey, Attribute attribute)
      throws LDAPException {
    byte[][] valueByteArrays = attribute.getValueByteArrays();
    List<String> values = new ArrayList<>(valueByteArrays.length);
    for (byte[] bytes : valueByteArrays) {
      String attributeValue = new String(bytes, StandardCharsets.UTF_8);
      if (MEMBER_OF_ATTRIBUTION.equals(attrKey)) {
        SearchResultEntry searchEntry = searchGroupResultEntry(userName, attributeValue, conn);
        var roleName = attribute2String(searchEntry.getAttribute(CN_ATTRIBUTION));
        var roleDesc = attribute2String(searchEntry.getAttribute(DESCRIPTION_ATTRIBUTION));
        values.add(
            objectMapper.writeValueAsString(Role.builder().desc(roleDesc).name(roleName).build()));
      } else {
        values.add(attributeValue);
      }
    }
    return values;
  }

  @Nullable
  private static String attribute2String(Attribute attr) {
    return attr == null || !attr.hasValue()
        ? null
        : new String(attr.getValueByteArray(), StandardCharsets.UTF_8);
  }

  private SearchResultEntry searchUserResultEntry(String userName, LDAPConnection conn)
      throws LDAPSearchException {
    Filter filter = Filter.createEqualityFilter("uid", Filter.encodeValue(userName));
    SearchResultEntry entry =
        conn.searchForEntry(ldapProperties.getBaseDn(), SearchScope.SUB, filter, USER_ATTRS);
    if (entry == null) {
      log.warn("User {} does not exist.", userName);
      throw new IllegalUserException(String.format("%s authentication failed.", userName));
    }
    log.info("User {} found.", userName);
    return entry;
  }

  private SearchResultEntry searchGroupResultEntry(
      String userName, String groupDn, LDAPConnection conn) throws LDAPException {
    SearchResultEntry entry = conn.getEntry(groupDn, CN_ATTRIBUTION, DESCRIPTION_ATTRIBUTION);
    if (entry == null) {
      log.error("The group {} that user {} belongs to does not exist.", groupDn, userName);
      throw new IllegalUserException(String.format("%s authentication failed.", userName));
    }
    log.info("The group {} that user {} belongs to was found. {}", groupDn, userName, entry);
    return entry;
  }
}
