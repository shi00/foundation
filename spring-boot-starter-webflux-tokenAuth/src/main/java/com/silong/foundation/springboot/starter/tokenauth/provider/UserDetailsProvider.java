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

package com.silong.foundation.springboot.starter.tokenauth.provider;

import javax.annotation.Nullable;
import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 用户详情提供者接口
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-05-08 14:02
 */
public interface UserDetailsProvider {

  /**
   * 根据用户名查询用户详情
   *
   * @param userName 用户名
   * @return 用户详情
   */
  @Nullable
  UserDetails findByUserName(@NonNull String userName);
}
