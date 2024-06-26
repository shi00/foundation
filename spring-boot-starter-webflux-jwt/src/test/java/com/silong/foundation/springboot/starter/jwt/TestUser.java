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

package com.silong.foundation.springboot.starter.jwt;

import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 用户
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-05-20 14:08
 */
@Data
@AllArgsConstructor
public class TestUser implements UserDetails {

  private static final Collection<? extends GrantedAuthority> DEFAULT_AUTHORITIES =
      List.of(
          new SimpleGrantedAuthority("guest"),
          new SimpleGrantedAuthority("operator"),
          new SimpleGrantedAuthority("admin"));

  public static TestUser SAM =
      new TestUser(
          DEFAULT_AUTHORITIES,
          "c76f0d9ab3e9c5649ba15bbb29ddb89eaee2053931590b373ab25adedc9ad22a17bca94375f73ea4963606d53d10b6b4",
          "sam",
          true,
          true,
          true,
          true);

  public static TestUser TOM =
      new TestUser(
          DEFAULT_AUTHORITIES,
          "d52edb12c56147f2d853bbcebfafb2128ac343edafa4b9822ba16e52255044dd47ca6de446afaa22783d34a937f24e68",
          "tom",
          true,
          true,
          true,
          true);

  private Collection<? extends GrantedAuthority> authorities;

  private String password;

  private String userName;

  private boolean isAccountNonExpired;

  private boolean isAccountNonLocked;

  private boolean isCredentialNonExpired;

  private boolean enabled;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return userName;
  }

  @Override
  public boolean isAccountNonExpired() {
    return isAccountNonExpired;
  }

  @Override
  public boolean isAccountNonLocked() {
    return isAccountNonLocked;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return isCredentialNonExpired;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }
}
