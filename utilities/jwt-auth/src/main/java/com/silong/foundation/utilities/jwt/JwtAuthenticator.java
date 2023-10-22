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

package com.silong.foundation.utilities.jwt;

import com.auth0.jwt.interfaces.Claim;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT鉴权接口
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-22 9:32
 */
public interface JwtAuthenticator {

  /**
   * 对指定载荷进行签名，产生签名Token
   *
   * @param payloads 载荷集合
   * @return 签名token
   */
  String generate(Map<String, ?> payloads);

  /**
   * 校验token
   *
   * @param jwtToken token
   * @param payloadsVerifier 载荷校验器
   * @return 校验结果
   */
  Result verify(String jwtToken, Function<Map<String, Claim>, Result> payloadsVerifier);
}
