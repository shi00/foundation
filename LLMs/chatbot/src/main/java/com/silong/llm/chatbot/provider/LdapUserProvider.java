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

package com.silong.llm.chatbot.provider;

import com.silong.foundation.springboot.starter.jwt.common.Credentials;
import com.silong.foundation.springboot.starter.jwt.exception.IllegalUserException;
import com.silong.foundation.springboot.starter.jwt.provider.UserAuthenticationProvider;
import com.silong.llm.chatbot.configure.properties.LdapProperties;
import com.unboundid.ldap.sdk.*;
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

      } else {
        Filter filter = Filter.createEqualityFilter("uid", Filter.encodeValue(userName));
        SearchResultEntry entry =
            conn.searchForEntry(ldapProperties.getBaseDn(), SearchScope.SUB, filter);
        if (entry == null) {
          log.warn("User {} does not exist.", userName);
          throw new IllegalUserException(String.format("%s authentication failed.", userName));
        }
      }
    } catch (LDAPException e) {
      log.error("User {} does not exist.", userName);
      throw new IllegalUserException(String.format("User %s does not exist.", userName));
    }
  }

  @Override
  public void authenticate(@NonNull Credentials credentials) {
    String userName = credentials.getUserName();
    try (LDAPConnection conn = connectionPool.getConnection()) {
      if (ldapProperties.getType() == LdapProperties.Type.ACTIVE_DIRECTORY) {

      } else {
        Filter filter = Filter.createEqualityFilter("uid", Filter.encodeValue(userName));
        SearchResultEntry entry =
            conn.searchForEntry(ldapProperties.getBaseDn(), SearchScope.SUB, filter);
        if (entry == null) {
          log.warn("User {} does not exist.", userName);
          throw new IllegalUserException(String.format("%s authentication failed.", userName));
        }
        BindResult result = conn.bind(entry.getDN(), credentials.getPassword());
        if (result.getResultCode() != ResultCode.SUCCESS) {
          log.warn("Failed to bind user {}.", userName);
          throw new IllegalUserException(String.format("%s authentication failed.", userName));
        }
      }
    } catch (LDAPException e) {
      log.error("Failed to authenticate user {}.", userName, e);
      throw new IllegalUserException(String.format("%s authentication failed.", userName));
    }
  }
}
