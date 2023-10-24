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
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/**
 * HMS算法签名JWT鉴权工具
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-21 17:07
 */
@Slf4j
@ToString
@EqualsAndHashCode
@Accessors(fluent = true)
public class SimpleJwtAuthenticator implements JwtAuthenticator {

  /** 构造器 */
  public class Builder {
    public Builder signatureAlgorithm(Algorithm signatureAlgorithm) {
      SimpleJwtAuthenticator.this.signatureAlgorithm = signatureAlgorithm;
      return this;
    }

    public Builder leeway(int leeway) {
      SimpleJwtAuthenticator.this.leeway = leeway;
      return this;
    }

    public Builder headers(Map<String, Object> headers) {
      SimpleJwtAuthenticator.this.headers = headers;
      return this;
    }

    public Builder issuer(String issuer) {
      SimpleJwtAuthenticator.this.issuer = issuer;
      return this;
    }

    public Builder subject(String subject) {
      SimpleJwtAuthenticator.this.subject = subject;
      return this;
    }

    public Builder jwtId(String jwtId) {
      SimpleJwtAuthenticator.this.jwtId = jwtId;
      return this;
    }

    public Builder audiences(String[] audiences) {
      SimpleJwtAuthenticator.this.audiences = audiences;
      return this;
    }

    public Builder period(Duration period) {
      SimpleJwtAuthenticator.this.period = period;
      return this;
    }

    public Builder defaultPayloads(Map<String, ?> defaultPayloads) {
      SimpleJwtAuthenticator.this.defaultPayloads = defaultPayloads;
      return this;
    }

    public Builder defaultPayloadsVerifier(
        Function<Map<String, Claim>, Result> defaultPayloadsVerifier) {
      SimpleJwtAuthenticator.this.defaultPayloadsVerifier = defaultPayloadsVerifier;
      return this;
    }

    public SimpleJwtAuthenticator build() {
      SimpleJwtAuthenticator.this.verifier = SimpleJwtAuthenticator.this.buildVerifier();
      return SimpleJwtAuthenticator.this;
    }
  }

  /** 校验器 */
  @ToString.Exclude private JWTVerifier verifier;

  /** 签名算法 */
  @Getter private Algorithm signatureAlgorithm;

  /** headers */
  @Getter private Map<String, Object> headers;

  /** token issuer */
  @Getter private String issuer;

  /** token subject */
  @Getter private String subject;

  /** token jwtId */
  @Getter private String jwtId;

  /** token audience */
  @Getter private String[] audiences;

  /** token有效时间窗 */
  @Getter private Duration period;

  /** 超期时间余地，单位：秒 */
  @Getter private int leeway;

  /** 默认token负载 */
  @Getter private Map<String, ?> defaultPayloads;

  /** 默认token负载校验器 */
  @Getter private Function<Map<String, Claim>, Result> defaultPayloadsVerifier;

  /** 构造方法 */
  private SimpleJwtAuthenticator() {}

  @Override
  public String generate(@NonNull Map<String, ?> payloads) {
    JWTCreator.Builder builder = JWT.create().withPayload(payloads);
    if (jwtId != null) {
      builder.withJWTId(jwtId);
    }
    if (headers != null && !headers.isEmpty()) {
      builder.withHeader(headers);
    }
    if (subject != null) {
      builder.withSubject(subject);
    }
    if (issuer != null) {
      builder.withIssuer(issuer);
    }
    if (audiences != null) {
      builder.withAudience(audiences);
    }

    // 指定超时时间
    if (period != null) {
      builder.withExpiresAt(Instant.now().truncatedTo(SECONDS).plusSeconds(period.toSeconds()));
    }
    return builder.sign(
        Objects.requireNonNull(signatureAlgorithm, "Signature algorithm must be specified."));
  }

  @Override
  public String generateByDefaultPayloads() {
    return generate(Objects.requireNonNull(defaultPayloads, "defaultPayloads must not be null."));
  }

  private JWTVerifier buildVerifier() {
    Verification verification =
        JWT.require(
            Objects.requireNonNull(signatureAlgorithm, "Signature algorithm must be specified."));
    if (issuer != null) {
      verification.withIssuer(this.issuer);
    }
    if (subject != null) {
      verification.withSubject(this.subject);
    }
    if (jwtId != null) {
      verification.withJWTId(this.jwtId);
    }
    if (audiences != null) {
      verification.withAudience(this.audiences);
    }
    if (leeway > 0) {
      verification.acceptExpiresAt(leeway);
    }
    return verification.build();
  }

  @Override
  public Result verify(
      @NonNull String jwtToken, @NonNull Function<Map<String, Claim>, Result> payloadsVerifier) {
    try {
      DecodedJWT jwt = verifier.verify(jwtToken);
      return payloadsVerifier.apply(jwt.getClaims());
    } catch (JWTVerificationException e) {
      log.error("Failed to verify the jwtToken: {}.", jwtToken, e);
      return new Result(false, e.getMessage());
    }
  }

  @Override
  public Result verifyByDefaultPayloadsVerifier(String jwtToken) {
    return verify(
        jwtToken,
        Objects.requireNonNull(
            defaultPayloadsVerifier, "defaultPayloadsVerifier must not be null."));
  }

  /**
   * 构造器模式
   *
   * @return 构造器
   */
  public static Builder builder() {
    return new SimpleJwtAuthenticator().new Builder();
  }
}
