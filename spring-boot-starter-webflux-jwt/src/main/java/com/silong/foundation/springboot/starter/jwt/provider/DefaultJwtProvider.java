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

import static org.springframework.util.StringUtils.hasLength;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.time.Instant;
import java.util.Map;
import org.springframework.lang.NonNull;

/**
 * jwt提供者默认实现
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-05-08 17:24
 */
public class DefaultJwtProvider
    implements com.silong.foundation.springboot.starter.tokenauth.provider.JWTProvider {

  private final Algorithm algorithm;

  private final String appName;

  private final JWTVerifier verifier;

  /**
   * 构造方法
   *
   * @param algorithm 签名算法
   * @param appName 应用名称
   */
  public DefaultJwtProvider(@NonNull Algorithm algorithm, @NonNull String appName) {
    this.algorithm = algorithm;
    this.appName = appName;
    this.verifier = JWT.require(algorithm).withIssuer(appName).build();
  }

  @NonNull
  @Override
  public String generate(String identity, Map<String, ?> extraAttributions) {
    if (!hasLength(identity)) {
      throw new IllegalArgumentException("identity must not be null or empty.");
    }
    return JWT.create()
        .withIssuer(appName)
        .withPayload(extraAttributions)
        .withAudience(identity)
        .withIssuedAt(Instant.now())
        .sign(algorithm);
  }

  @NonNull
  @Override
  public DecodedJWT verify(String token) throws JWTVerificationException {
    if (!hasLength(token)) {
      throw new IllegalArgumentException("token must not be null or empty.");
    }
    return verifier.verify(token);
  }
}
