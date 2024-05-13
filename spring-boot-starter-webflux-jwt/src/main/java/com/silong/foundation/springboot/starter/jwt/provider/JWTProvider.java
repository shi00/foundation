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

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.validation.constraints.NotEmpty;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * JWT 生产，校验提供者
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-05-08 17:16
 */
public interface JWTProvider {

  /**
   * 生成token
   *
   * @param identity 唯一标识
   * @param extraAttributions 附加信息
   * @return token
   */
  @Nonnull
  String generate(@NotEmpty String identity, @Nullable Map<String, ?> extraAttributions);

  /**
   * 校验token
   *
   * @param token token字符串
   * @return token信息
   * @throws Exception 异常
   */
  @Nonnull
  DecodedJWT verify(@NotEmpty String token) throws Exception;
}
