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
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.stereotype.Component;

/**
 * 基于数据库实现的用户提供者
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-19 20:21
 */
@Component
@Slf4j
public class OpenLdapUserProvider implements UserAuthenticationProvider {

  private final LdapTemplate ldapTemplate;

  /**
   * 构造方法
   *
   * @param ldapTemplate ldap client
   */
  public OpenLdapUserProvider(@NonNull LdapTemplate ldapTemplate) {
    this.ldapTemplate = ldapTemplate;
  }

  @Override
  public void checkUserExists(@NonNull String userName) {
    AndFilter filter = new AndFilter();
    filter.and(new EqualsFilter("objectClass", "inetOrgPerson")); // 根据实际对象类调整
    filter.and(new EqualsFilter("uid", userName)); // 用户名属性可能是 uid、cn、sAMAccountName 等
    List<?> results =
        ldapTemplate.search(
            LdapUtils.emptyLdapName(), filter.encode(), (ContextMapper<Boolean>) ctx -> true);
    if (results.isEmpty()) {
      log.warn("{} does not exist.", userName);
      throw new IllegalUserException(String.format("%s does not exist.", userName));
    }
  }

  @Override
  public void authenticate(@NonNull Credentials credentials) {
    AndFilter filter =
        new AndFilter()
            .and(new EqualsFilter("objectClass", "inetOrgPerson"))
            .and(new EqualsFilter("uid", credentials.getUserName()));
    if (!ldapTemplate.authenticate(
        LdapUtils.emptyLdapName(), filter.encode(), credentials.getPassword())) {
      log.warn("{} authentication failed.", credentials.getUserName());
      throw new IllegalUserException(
          String.format("%s authentication failed.", credentials.getUserName()));
    }
  }
}
