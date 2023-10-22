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

  /** token校验结果 */
  @Getter
  @Setter
  @AllArgsConstructor
  @Builder
  @EqualsAndHashCode
  @ToString
  public static class Result {
    public static final Result VALID = new Result(true, null);

    /** 是否有效 */
    private boolean valid;

    /** 错误原因 */
    private String cause;
  }

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
  public Result verifyJwtToken(
      @NonNull String jwtToken, @NonNull Function<Map<String, Claim>, Result> payloadsVerifier) {
    try {
      DecodedJWT jwt = verifier.verify(jwtToken);
      Result result = checkSubject(jwt.getSubject());
      if (!result.valid) {
        return result;
      }
      result = checkIssuer(jwt.getIssuer());
      if (!result.valid) {
        return result;
      }

      result = checkJwtId(jwt.getId());
      if (!result.valid) {
        return result;
      }

      result = checkAudiences(jwt.getAudience());
      if (!result.valid) {
        return result;
      }

      result = payloadsVerifier.apply(jwt.getClaims());
      if (!result.valid) {
        return result;
      }
      return Result.VALID;
    } catch (JWTVerificationException e) {
      log.error("Failed to verify the token.", e);
      return new Result(false, e.getMessage());
    }
  }

  private Result checkSubject(String jwtSubject) {
    if (subject == null) {
      return Result.VALID;
    } else if (subject.equals(jwtSubject)) {
      return Result.VALID;
    } else {
      log.error("subject:{} != jwtSubject:{}", subject, jwtSubject);
      return new Result(false, "Invalid Subject.");
    }
  }

  private Result checkIssuer(String jwtIssuer) {
    if (issuer == null) {
      return Result.VALID;
    } else if (issuer.equals(jwtIssuer)) {
      return Result.VALID;
    } else {
      log.error("issuer:{} != jwtIssuer:{}", issuer, jwtIssuer);
      return new Result(false, "Invalid Issuer.");
    }
  }

  private Result checkJwtId(String jwtId) {
    if (this.jwtId == null) {
      return Result.VALID;
    } else if (this.jwtId.equals(jwtId)) {
      return Result.VALID;
    } else {
      log.error("jwtId:{} != Id:{}", this.jwtId, jwtId);
      return new Result(false, "Invalid JwtId.");
    }
  }

  private Result checkAudiences(List<String> jwtAudiences) {
    if (this.audience == null) {
      return Result.VALID;
    } else if (jwtAudiences != null
        && jwtAudiences.size() == 1
        && jwtAudiences.contains(audience)) {
      return Result.VALID;
    } else {
      return new Result(false, "Invalid Audience.");
    }
  }
}
