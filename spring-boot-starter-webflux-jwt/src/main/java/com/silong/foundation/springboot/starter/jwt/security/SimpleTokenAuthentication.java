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

package com.silong.foundation.springboot.starter.jwt.security;

import java.util.Collection;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * token认证信息
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-05-09 11:27
 */
@Data
@AllArgsConstructor
public class SimpleTokenAuthentication implements Authentication {

  /** token对应的权限列表 */
  private Collection<GrantedAuthority> authorities;

  /** 鉴权通过 */
  private boolean authenticated;

  /** 请求token */
  private String token;

  /** 用户名 */
  private String userName;

  @Override
  public String getCredentials() {
    return token;
  }

  @Override
  public Object getDetails() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object getPrincipal() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getName() {
    return userName;
  }
}
