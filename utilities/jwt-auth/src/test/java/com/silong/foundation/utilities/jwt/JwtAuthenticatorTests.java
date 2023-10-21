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

import static com.silong.foundation.utilities.jwt.JwtAuthenticator.SignatureAlgorithm.*;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.auth0.jwt.interfaces.Claim;
import com.silong.foundation.utilities.jwt.JwtAuthenticator.Result;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-21 19:02
 */
public class JwtAuthenticatorTests {

  private static final Instant NOW_INSTANT = Instant.now().truncatedTo(SECONDS);
  private static final Date NOW_DATE = Date.from(NOW_INSTANT);

  private static final String STR_VALUE = RandomStringUtils.random(RandomUtils.nextInt(1, 64));

  private static final Double DOUBLE_VALUE = RandomUtils.nextDouble();

  private static final Long LONG_VALUE = RandomUtils.nextLong();

  private static final Boolean BOOLEAN_VALUE = RandomUtils.nextBoolean();

  private static final Integer INTEGER_VALUE = RandomUtils.nextInt();

  private static final List<?> LIST_VALUE =
      IntStream.range(0, 100)
          .mapToObj(i -> RandomStringUtils.random(RandomUtils.nextInt(1, 32)))
          .toList();

  private static final Map<?, ?> MAP_VALUE =
      Map.of(
          RandomStringUtils.random(RandomUtils.nextInt(1, 32)),
          RandomStringUtils.random(RandomUtils.nextInt(1, 32)),
          RandomStringUtils.random(RandomUtils.nextInt(1, 32)),
          RandomStringUtils.random(RandomUtils.nextInt(1, 32)),
          RandomStringUtils.random(RandomUtils.nextInt(1, 32)),
          RandomStringUtils.random(RandomUtils.nextInt(1, 32)),
          RandomStringUtils.random(RandomUtils.nextInt(1, 32)),
          RandomStringUtils.random(RandomUtils.nextInt(1, 32)),
          RandomStringUtils.random(RandomUtils.nextInt(1, 32)),
          RandomStringUtils.random(RandomUtils.nextInt(1, 32)),
          RandomStringUtils.random(RandomUtils.nextInt(1, 32)),
          RandomStringUtils.random(RandomUtils.nextInt(1, 32)),
          RandomStringUtils.random(RandomUtils.nextInt(1, 32)),
          RandomStringUtils.random(RandomUtils.nextInt(1, 32)),
          RandomStringUtils.random(RandomUtils.nextInt(1, 32)),
          RandomStringUtils.random(RandomUtils.nextInt(1, 32)),
          RandomStringUtils.random(RandomUtils.nextInt(1, 32)),
          RandomStringUtils.random(RandomUtils.nextInt(1, 32)),
          RandomStringUtils.random(RandomUtils.nextInt(1, 32)),
          RandomStringUtils.random(RandomUtils.nextInt(1, 32)));

  private static final Map<String, ?> PAYLOAD =
      new HashMap<>(
          Map.ofEntries(
              Map.entry("1", STR_VALUE),
              Map.entry("2", NOW_DATE),
              Map.entry("3", NOW_INSTANT),
              Map.entry("4", LIST_VALUE),
              Map.entry("5", MAP_VALUE),
              Map.entry("6", INTEGER_VALUE),
              Map.entry("7", BOOLEAN_VALUE),
              Map.entry("9", DOUBLE_VALUE),
              Map.entry("10", LONG_VALUE)));

  private static Result check(Map<String, Claim> m) {
    boolean result =
        m.get("1").asString().equals(STR_VALUE)
            && m.get("4").as(List.class).equals(LIST_VALUE)
            && m.get("5").as(Map.class).equals(MAP_VALUE)
            && m.get("6").asInt().equals(INTEGER_VALUE)
            && m.get("7").asBoolean().equals(BOOLEAN_VALUE)
            && m.get("9").as(Double.class).equals(DOUBLE_VALUE)
            && m.get("10").as(Long.class).equals(LONG_VALUE)
            && m.get("11").isNull()
            && m.get("3").asInstant().equals(NOW_INSTANT)
            && m.get("2").asDate().equals(NOW_DATE);
    return result ? Result.VALID : new Result(false, "Invalid Payloads.");
  }

  private static byte[] randomKey(int bits) {
    return RandomUtils.nextBytes(bits / 8);
  }

  @BeforeAll
  public static void initialize() {
    PAYLOAD.put("11", null);
  }

  @Test
  public void test1() {
    JwtAuthenticator authenticator =
        new JwtAuthenticator(
            HMAC_SHA256,
            randomKey(256),
            -1,
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)),
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)),
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)),
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)));
    Assertions.assertTrue(
        authenticator
            .verifyJwtToken(authenticator.generateJwtToken(PAYLOAD), JwtAuthenticatorTests::check)
            .isValid());
  }

  @Test
  public void test2() {
    JwtAuthenticator authenticator =
        new JwtAuthenticator(
            HMAC_SHA512,
            randomKey(512),
            -1,
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)),
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)),
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)),
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)));
    Assertions.assertTrue(
        authenticator
            .verifyJwtToken(authenticator.generateJwtToken(PAYLOAD), JwtAuthenticatorTests::check)
            .isValid());
  }

  @Test
  public void test3() {
    JwtAuthenticator authenticator =
        new JwtAuthenticator(
            HMAC_SHA384,
            randomKey(384),
            -1,
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)),
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)),
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)),
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)));
    Assertions.assertTrue(
        authenticator
            .verifyJwtToken(authenticator.generateJwtToken(PAYLOAD), JwtAuthenticatorTests::check)
            .isValid());
  }

  @Test
  public void test4() throws InterruptedException {
    JwtAuthenticator authenticator =
        new JwtAuthenticator(
            HMAC_SHA512,
            randomKey(512),
            2,
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)),
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)),
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)),
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)));
    String jwtToken = authenticator.generateJwtToken(PAYLOAD);
    Thread.sleep(5000);
    Assertions.assertFalse(
        authenticator.verifyJwtToken(jwtToken, JwtAuthenticatorTests::check).isValid());
  }

  @Test
  public void test5() throws InterruptedException {
    JwtAuthenticator authenticator =
        new JwtAuthenticator(
            HMAC_SHA256,
            randomKey(256),
            2,
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)),
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)),
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)),
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)));
    String jwtToken = authenticator.generateJwtToken(PAYLOAD);
    Thread.sleep(5000);
    Assertions.assertFalse(
        authenticator.verifyJwtToken(jwtToken, JwtAuthenticatorTests::check).isValid());
  }

  @Test
  public void test6() throws InterruptedException {
    JwtAuthenticator authenticator =
        new JwtAuthenticator(
            HMAC_SHA384,
            randomKey(384),
            1,
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)),
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)),
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)),
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)));
    String jwtToken = authenticator.generateJwtToken(PAYLOAD);
    Thread.sleep(5000);
    Assertions.assertFalse(
        authenticator.verifyJwtToken(jwtToken, JwtAuthenticatorTests::check).isValid());
  }

  @Test
  public void test7() throws InterruptedException {
    JwtAuthenticator authenticator =
        new JwtAuthenticator(
            HMAC_SHA384,
            randomKey(384),
            1,
            null,
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)),
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)),
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)));
    String jwtToken = authenticator.generateJwtToken(PAYLOAD);
    Thread.sleep(5000);
    Assertions.assertFalse(
        authenticator.verifyJwtToken(jwtToken, JwtAuthenticatorTests::check).isValid());
  }

  @Test
  public void test8() {
    JwtAuthenticator authenticator =
        new JwtAuthenticator(
            HMAC_SHA384,
            randomKey(384),
            1,
            null,
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)),
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)),
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)));
    String jwtToken = authenticator.generateJwtToken(PAYLOAD);
    Assertions.assertTrue(
        authenticator.verifyJwtToken(jwtToken, JwtAuthenticatorTests::check).isValid());
  }

  @Test
  public void test9() {
    JwtAuthenticator authenticator =
        new JwtAuthenticator(
            HMAC_SHA384,
            randomKey(384),
            1,
            null,
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)),
            null,
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)));
    String jwtToken = authenticator.generateJwtToken(PAYLOAD);
    Assertions.assertTrue(
        authenticator.verifyJwtToken(jwtToken, JwtAuthenticatorTests::check).isValid());
  }

  @Test
  public void test10() {
    JwtAuthenticator authenticator =
        new JwtAuthenticator(
            HMAC_SHA384,
            randomKey(384),
            1,
            null,
            null,
            null,
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)));
    String jwtToken = authenticator.generateJwtToken(PAYLOAD);
    Assertions.assertTrue(
        authenticator.verifyJwtToken(jwtToken, JwtAuthenticatorTests::check).isValid());
  }

  @Test
  public void test11() {
    JwtAuthenticator authenticator =
        new JwtAuthenticator(HMAC_SHA384, randomKey(384), 1, null, null, null, null);
    String jwtToken = authenticator.generateJwtToken(PAYLOAD);
    Assertions.assertTrue(
        authenticator.verifyJwtToken(jwtToken, JwtAuthenticatorTests::check).isValid());
  }

  @Test
  public void test81() {
    JwtAuthenticator authenticator =
        new JwtAuthenticator(
            HMAC_SHA256,
            randomKey(256),
            1,
            null,
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)),
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)),
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)));
    String jwtToken = authenticator.generateJwtToken(PAYLOAD);
    Assertions.assertTrue(
        authenticator.verifyJwtToken(jwtToken, JwtAuthenticatorTests::check).isValid());
  }

  @Test
  public void test91() {
    JwtAuthenticator authenticator =
        new JwtAuthenticator(
            HMAC_SHA256,
            randomKey(256),
            1,
            null,
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)),
            null,
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)));
    String jwtToken = authenticator.generateJwtToken(PAYLOAD);
    Assertions.assertTrue(
        authenticator.verifyJwtToken(jwtToken, JwtAuthenticatorTests::check).isValid());
  }

  @Test
  public void test101() {
    JwtAuthenticator authenticator =
        new JwtAuthenticator(
            HMAC_SHA256,
            randomKey(256),
            1,
            null,
            null,
            null,
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)));
    String jwtToken = authenticator.generateJwtToken(PAYLOAD);
    Assertions.assertTrue(
        authenticator.verifyJwtToken(jwtToken, JwtAuthenticatorTests::check).isValid());
  }

  @Test
  public void test111() {
    JwtAuthenticator authenticator =
        new JwtAuthenticator(HMAC_SHA256, randomKey(256), 1, null, null, null, null);
    String jwtToken = authenticator.generateJwtToken(PAYLOAD);
    Assertions.assertTrue(
        authenticator.verifyJwtToken(jwtToken, JwtAuthenticatorTests::check).isValid());
  }

  @Test
  public void test811() {
    JwtAuthenticator authenticator =
        new JwtAuthenticator(
            HMAC_SHA512,
            randomKey(512),
            1,
            null,
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)),
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)),
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)));
    String jwtToken = authenticator.generateJwtToken(PAYLOAD);
    Assertions.assertTrue(
        authenticator.verifyJwtToken(jwtToken, JwtAuthenticatorTests::check).isValid());
  }

  @Test
  public void test911() {
    JwtAuthenticator authenticator =
        new JwtAuthenticator(
            HMAC_SHA512,
            randomKey(512),
            1,
            null,
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)),
            null,
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)));
    String jwtToken = authenticator.generateJwtToken(PAYLOAD);
    Assertions.assertTrue(
        authenticator.verifyJwtToken(jwtToken, JwtAuthenticatorTests::check).isValid());
  }

  @Test
  public void test1101() {
    JwtAuthenticator authenticator =
        new JwtAuthenticator(
            HMAC_SHA512,
            randomKey(512),
            1,
            null,
            null,
            null,
            RandomStringUtils.random(RandomUtils.nextInt(1, 128)));
    String jwtToken = authenticator.generateJwtToken(PAYLOAD);
    Assertions.assertTrue(
        authenticator.verifyJwtToken(jwtToken, JwtAuthenticatorTests::check).isValid());
  }

  @Test
  public void test1111() {
    JwtAuthenticator authenticator =
        new JwtAuthenticator(HMAC_SHA512, randomKey(512), 1, null, null, null, null);
    String jwtToken = authenticator.generateJwtToken(PAYLOAD);
    Assertions.assertTrue(
        authenticator.verifyJwtToken(jwtToken, JwtAuthenticatorTests::check).isValid());
  }
}
