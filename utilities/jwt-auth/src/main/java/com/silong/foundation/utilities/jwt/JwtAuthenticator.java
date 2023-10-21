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

import static java.time.temporal.ChronoUnit.SECONDS;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.auth0.jwt.interfaces.Verification;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT 鉴权工具
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-21 17:07
 */
@Slf4j
@ToString
@EqualsAndHashCode
@Accessors(fluent = true)
public class JwtAuthenticator {

  /**
   * 签名算法
   *
   * @author louis sin
   * @version 1.0.0
   * @since 2023-10-21 17:05
   */
  public enum SignatureAlgorithm {
    /** HMAC-ShA256 */
    HMAC_SHA256,
    /** HMAC-ShA512 */
    HMAC_SHA512,
    /** HMAC-ShA384 */
    HMAC_SHA384
  }

  /** 校验器 */
  @ToString.Exclude private final JWTVerifier verifier;

  /** 签名算法 */
  @Getter private final Algorithm algorithm;

  /** token issuer */
  @Getter private String issuer;

  /** token subject */
  @Getter private String subject;

  /** token jwtId */
  @Getter private String jwtId;

  /** token audience */
  @Getter private String audience;

  /** token有效时间窗，单位：秒 */
  @Getter private int leeway;

  /**
   * 构造方法
   *
   * @param algorithm 签名算法
   * @param key 签名密钥
   * @param leeway the window in seconds in which the Expires At Claim will still be valid
   * @param issuer issuer
   * @param subject subject
   * @param jwtId jwtId
   * @param audience audience
   */
  public JwtAuthenticator(
      @NonNull SignatureAlgorithm algorithm,
      byte[] key,
      int leeway,
      String issuer,
      String subject,
      String jwtId,
      String audience) {
    this.algorithm =
        switch (algorithm) {
          case HMAC_SHA256 -> Algorithm.HMAC256(key);
          case HMAC_SHA384 -> Algorithm.HMAC384(key);
          case HMAC_SHA512 -> Algorithm.HMAC512(key);
        };
    Verification verification = JWT.require(this.algorithm);
    if (issuer != null) {
      verification.withIssuer(this.issuer = issuer);
    }
    if (subject != null) {
      verification.withSubject(this.subject = subject);
    }
    if (jwtId != null) {
      verification.withJWTId(this.jwtId = jwtId);
    }
    if (audience != null) {
      verification.withAudience(this.audience = audience);
    }
    if (leeway > 0) {
      verification.acceptExpiresAt(this.leeway = leeway);
    }
    this.verifier = verification.build();
  }

  /**
   * 根据指定payload生成JwtToken
   *
   * @param payloadClaims payload
   * @return token
   */
  public String generateJwtToken(@NonNull Map<String, ?> payloadClaims) {
    JWTCreator.Builder builder = JWT.create().withPayload(payloadClaims);
    if (jwtId != null) {
      builder.withJWTId(jwtId);
    }
    if (audience != null) {
      builder.withAudience(audience);
    }
    if (subject != null) {
      builder.withSubject(subject);
    }
    if (issuer != null) {
      builder.withIssuer(issuer);
    }
    if (leeway > 0) {
      builder.withExpiresAt(Instant.now().truncatedTo(SECONDS).plusSeconds(leeway));
    }
    return builder.sign(algorithm);
  }

  /**
   * 校验token
   *
   * @param jwtToken token
   * @return true or false
   */
  public boolean verifyJwtToken(
      @NonNull String jwtToken, @NonNull Function<Map<String, Claim>, Boolean> payloadsVerifier) {
    try {
      DecodedJWT jwt = verifier.verify(jwtToken);
      return checkSubject(jwt.getSubject())
          && checkIssuer(jwt.getIssuer())
          && checkJwtId(jwt.getId())
          && checkAudiences(jwt.getAudience())
          && payloadsVerifier.apply(jwt.getClaims());
    } catch (JWTVerificationException e) {
      log.error("Failed to verify the token.", e);
      return false;
    }
  }

  private boolean checkSubject(String jwtSubject) {
    if (subject == null) {
      return true;
    } else {
      return subject.equals(jwtSubject);
    }
  }

  private boolean checkIssuer(String jwtIssuer) {
    if (issuer == null) {
      return true;
    } else {
      return issuer.equals(jwtIssuer);
    }
  }

  private boolean checkJwtId(String jwtId) {
    if (this.jwtId == null) {
      return true;
    } else {
      return this.jwtId.equals(jwtId);
    }
  }

  private boolean checkAudiences(List<String> jwtAudiences) {
    if (this.audience == null) {
      return true;
    } else {
      return jwtAudiences != null && jwtAudiences.contains(audience) && jwtAudiences.size() == 1;
    }
  }
}
