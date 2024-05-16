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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import javax.annotation.Nullable;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;

/**
 * 默认用户信息提供者
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-05-14 22:55
 */
public class DefaultUserDetailsProvider implements UserDetailsProvider {

  private final Map<String, UserDetails> userDetailsMap = new ConcurrentSkipListMap<>();

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
  @Override
  public UserDetails findByUserName(@NonNull String userName) {
    return userDetailsMap.get(userName);
  }

  @NonNull
  @Override
  public Set<String> findUserRoles(@NonNull String userName) {
    UserDetails userDetails = userDetailsMap.get(userName);
    return userDetails == null
        ? Set.of()
        : userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(StringUtils::hasLength)
            .collect(toUnmodifiableSet());
  }
}
