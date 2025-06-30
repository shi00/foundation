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

import com.silong.foundation.springboot.starter.jwt.common.Credentials;
import com.silong.foundation.springboot.starter.jwt.exception.IllegalUserException;
import com.silong.foundation.springboot.starter.jwt.provider.UserAuthenticationProvider;
import com.silong.llm.chatbot.configure.properties.LdapProperties;
import com.unboundid.ldap.sdk.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NonNull;
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

  /**
   * 构造方法
   *
   * @param ldapProperties 相关配置
   * @param connectionPool 连接池
   */
  public LdapUserProvider(
      @NonNull LdapProperties ldapProperties, @NonNull LDAPConnectionPool connectionPool) {
    this.ldapProperties = ldapProperties;
    this.connectionPool = connectionPool;
  }

  @Override
  public void checkUserExists(@NonNull String userName) {
    try (var conn = connectionPool.getConnection()) {
      if (ldapProperties.getType() == LdapProperties.Type.ACTIVE_DIRECTORY) {
        throw new UnsupportedOperationException();
      } else {
        searchResultEntry(userName, conn);
      }
    } catch (LDAPException e) {
      log.error("User {} does not exist.", userName);
      throw new IllegalUserException(String.format("User %s does not exist.", userName));
    }
  }

  @Override
  public Map<String, Object> authenticate(@NonNull Credentials credentials) {
    String userName = credentials.getUserName();
    try (LDAPConnection conn = connectionPool.getConnection()) {
      if (ldapProperties.getType() == LdapProperties.Type.ACTIVE_DIRECTORY) {
        throw new UnsupportedOperationException();
      } else {
        SearchResultEntry entry = searchResultEntry(userName, conn);
        BindResult result = conn.bind(entry.getDN(), credentials.getPassword());
        if (result.getResultCode() != ResultCode.SUCCESS) {
          log.warn("Failed to bind user {}.", userName);
          throw new IllegalUserException(String.format("%s authentication failed.", userName));
        }
        log.info("User {} authenticated.", userName);
        return Arrays.stream(USER_ATTRS)
            .map(attrKey -> new Tuple2<>(attrKey, entry.getAttribute(attrKey)))
            .filter(t2 -> t2.attribute != null && t2.attribute.hasValue())
            .peek(t2 -> log.info(t2.attribute().toString()))
            .collect(Collectors.toMap(Tuple2::attrKey, LdapUserProvider::convert2StringList));
      }
    } catch (LDAPException e) {
      log.error("Failed to authenticate user {}.", userName, e);
      throw new IllegalUserException(String.format("%s authentication failed.", userName));
    }
  }

  /**
   * 读取属性值
   *
   * @param t2 属性
   * @return 字符串列表
   */
  private static List<String> convert2StringList(Tuple2<String, Attribute> t2) {
    byte[][] valueByteArrays = t2.attribute().getValueByteArrays();
    List<String> values = new ArrayList<>(valueByteArrays.length);
    for (byte[] v : valueByteArrays) {
      values.add(new String(v, StandardCharsets.UTF_8));
    }
    return values;
  }

  private SearchResultEntry searchResultEntry(String userName, LDAPConnection conn)
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

  private record Tuple2<K, V>(K attrKey, V attribute) {}
}
