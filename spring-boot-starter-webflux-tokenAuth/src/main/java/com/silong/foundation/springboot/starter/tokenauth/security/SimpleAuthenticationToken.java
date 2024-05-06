/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.silong.foundation.springboot.starter.tokenauth.security;

import java.util.Collection;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * 认证token
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-16 11:30
 */
@Getter
@Setter
@Accessors(fluent = true)
public class SimpleAuthenticationToken extends AbstractAuthenticationToken {

  /** 访问令牌 */
  private String accessToken;

  /**
   * 构造方法
   *
   * @param authorities 授权列表
   * @param authenticated 鉴权结果
   */
  public SimpleAuthenticationToken(
      Collection<SimpleGrantedAuthority> authorities, boolean authenticated) {
    this(null, authorities, authenticated);
  }

  /**
   * 构造方法
   *
   * @param accessToken 访问token
   * @param authorities 授权列表
   * @param authenticated 鉴权结果
   */
  public SimpleAuthenticationToken(
      String accessToken, Collection<SimpleGrantedAuthority> authorities, boolean authenticated) {
    super(authorities);
    this.accessToken = accessToken;
    setAuthenticated(authenticated);
  }

  @Override
  public Object getCredentials() {
    return null;
  }

  @Override
  public Object getPrincipal() {
    return null;
  }
}
