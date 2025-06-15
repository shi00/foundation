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

package com.silong.foundation.springboot.starter.jwt.provider;

import static java.util.stream.Collectors.toUnmodifiableSet;

import com.silong.foundation.springboot.starter.jwt.common.Credentials;
import com.silong.foundation.springboot.starter.jwt.exception.IllegalUserException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

/**
 * 默认用户信息提供者
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-05-14 22:55
 */
@Slf4j
public class DefaultUserAuthenticationProvider implements UserAuthenticationProvider {

  private final Map<String, UserDetails> userDetailsMap = new ConcurrentSkipListMap<>();

  private final PasswordEncoder passwordEncoder;

  /**
   * 构造方法
   *
   * @param passwordEncoder 密码编码器
   */
  public DefaultUserAuthenticationProvider(@NonNull PasswordEncoder passwordEncoder) {
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * 添加用户
   *
   * @param userDetails 用户信息
   */
  public void addUser(@NonNull UserDetails userDetails) {
    userDetailsMap.put(userDetails.getUsername(), userDetails);
  }

  /**
   * 删除用户
   *
   * @param userName 用户名
   */
  public void removeUser(@NonNull String userName) {
    userDetailsMap.remove(userName);
  }

  /** 清除所有用户 */
  public void clearUsers() {
    userDetailsMap.clear();
  }

  @Nullable
  public UserDetails findByUserName(@NonNull String userName) {
    return userDetailsMap.get(userName);
  }

  @NonNull
  public Set<String> findUserRoles(@NonNull String userName) {
    UserDetails userDetails = userDetailsMap.get(userName);
    return userDetails == null
        ? Set.of()
        : userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(StringUtils::hasLength)
            .collect(toUnmodifiableSet());
  }

  @Override
  public void checkUserExists(String userName) {
    // 查找用户
    UserDetails userDetails = findByUserName(userName);
    if (userDetails == null) {
      log.error("{} could not be found.", userName);
      throw new IllegalUserException("Illegal Access Token.");
    }
  }

  @Override
  public void authenticate(Credentials credentials) {
    String userName = credentials.getUserName();
    UserDetails userDetails = findByUserName(userName);
    if (userDetails == null) {
      log.warn("userDetails can't be found by userName: {}", userName);
      throw new IllegalUserException("Illegal user: " + userName);
    }

    if (!userDetails.isEnabled()) {
      log.warn("user account is not available. {}", userDetails);
      throw new IllegalUserException("Illegal user: " + userName);
    }

    if (!userDetails.isAccountNonExpired()) {
      log.warn("user's account has expired. {}", userDetails);
      throw new AccountExpiredException("user's account has expired. userName: " + userName);
    }

    // 密码过期校验
    if (!userDetails.isCredentialsNonExpired()) {
      log.warn("user's password has expired. {}", userDetails);
      throw new CredentialsExpiredException("user's password has expired. userName: " + userName);
    }

    if (!userDetails.isAccountNonLocked()) {
      log.warn("user's account has locked. {}", userDetails);
      throw new LockedException("user's account has locked. userName: " + userName);
    }

    // 密码校验
    if (!passwordEncoder.matches(credentials.getPassword(), userDetails.getPassword())) {
      log.warn("user has an incorrect password. userName: {}", userName);
      throw new BadCredentialsException("Incorrect password.");
    }
  }
}
