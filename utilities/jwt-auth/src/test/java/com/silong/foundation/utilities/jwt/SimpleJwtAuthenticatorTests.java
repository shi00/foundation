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

import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import lombok.SneakyThrows;
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
public class SimpleJwtAuthenticatorTests {

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

  private record TestObj(int i, String v) {}

  private static final Map<String, Object> HEADERS =
      Map.of(
          RandomStringUtils.random(RandomUtils.nextInt(1, 32)),
          RandomUtils.nextInt(),
          RandomStringUtils.random(RandomUtils.nextInt(1, 32)),
          RandomUtils.nextBoolean(),
          RandomStringUtils.random(RandomUtils.nextInt(1, 32)),
          RandomUtils.nextBytes(28),
          RandomStringUtils.random(RandomUtils.nextInt(1, 32)),
          RandomUtils.nextDouble(),
          RandomStringUtils.random(RandomUtils.nextInt(1, 32)),
          RandomUtils.nextFloat(),
          RandomStringUtils.random(RandomUtils.nextInt(1, 32)),
          RandomUtils.nextLong(),
          RandomStringUtils.random(RandomUtils.nextInt(1, 32)),
          RandomStringUtils.random(RandomUtils.nextInt(1, 128)),
          RandomStringUtils.random(RandomUtils.nextInt(1, 32)),
          new TestObj(RandomUtils.nextInt(), RandomStringUtils.random(RandomUtils.nextInt(1, 32))));

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
    Algorithm algorithm = Algorithm.HMAC256(randomKey(256));
    SimpleJwtAuthenticator authenticator =
        SimpleJwtAuthenticator.builder().signatureAlgorithm(algorithm).build();
    String token = authenticator.generate(PAYLOAD);
    Result result = authenticator.verify(token, SimpleJwtAuthenticatorTests::check);
    Assertions.assertTrue(result.isValid(), result::cause);
  }

  @Test
  public void test2() {
    Algorithm algorithm = Algorithm.HMAC384(randomKey(384));
    SimpleJwtAuthenticator authenticator =
        SimpleJwtAuthenticator.builder().signatureAlgorithm(algorithm).build();
    String token = authenticator.generate(PAYLOAD);
    Result result = authenticator.verify(token, SimpleJwtAuthenticatorTests::check);
    Assertions.assertTrue(result.isValid(), result::cause);
  }

  @Test
  public void test3() {
    Algorithm algorithm = Algorithm.HMAC512(randomKey(512));
    SimpleJwtAuthenticator authenticator =
        SimpleJwtAuthenticator.builder().signatureAlgorithm(algorithm).build();
    String token = authenticator.generate(PAYLOAD);
    Result result = authenticator.verify(token, SimpleJwtAuthenticatorTests::check);
    Assertions.assertTrue(result.isValid(), result::cause);
  }

  @Test
  public void test4() {
    Algorithm algorithm = Algorithm.HMAC512(randomKey(512));
    SimpleJwtAuthenticator authenticatorA =
        SimpleJwtAuthenticator.builder().signatureAlgorithm(algorithm).issuer("A").build();
    SimpleJwtAuthenticator authenticatorB =
        SimpleJwtAuthenticator.builder().signatureAlgorithm(algorithm).issuer("B").build();
    String token = authenticatorA.generate(PAYLOAD);
    Result result = authenticatorB.verify(token, m -> Result.VALID);
    Assertions.assertFalse(result.isValid(), result::cause);
  }

  @Test
  public void test5() {
    Algorithm algorithm = Algorithm.HMAC256(randomKey(256));
    SimpleJwtAuthenticator authenticatorA =
        SimpleJwtAuthenticator.builder()
            .signatureAlgorithm(algorithm)
            .audiences(new String[] {"A"})
            .build();
    SimpleJwtAuthenticator authenticatorB =
        SimpleJwtAuthenticator.builder()
            .signatureAlgorithm(algorithm)
            .audiences(new String[] {"A", "B"})
            .build();
    String token = authenticatorA.generate(PAYLOAD);
    Result result = authenticatorB.verify(token, m -> Result.VALID);
    Assertions.assertFalse(result.isValid(), result::cause);
  }

  @Test
  public void test6() {
    Algorithm algorithm = Algorithm.HMAC384(randomKey(384));
    SimpleJwtAuthenticator authenticatorA =
        SimpleJwtAuthenticator.builder().signatureAlgorithm(algorithm).subject("A").build();
    SimpleJwtAuthenticator authenticatorB =
        SimpleJwtAuthenticator.builder().signatureAlgorithm(algorithm).subject("B").build();
    String token = authenticatorA.generate(PAYLOAD);
    Result result = authenticatorB.verify(token, m -> Result.VALID);
    Assertions.assertFalse(result.isValid(), result::cause);
  }

  @Test
  public void test7() {
    Algorithm algorithm = Algorithm.HMAC384(randomKey(384));
    SimpleJwtAuthenticator authenticatorA =
        SimpleJwtAuthenticator.builder().signatureAlgorithm(algorithm).jwtId("A").build();
    SimpleJwtAuthenticator authenticatorB =
        SimpleJwtAuthenticator.builder().signatureAlgorithm(algorithm).jwtId("B").build();
    String token = authenticatorA.generate(PAYLOAD);
    Result result = authenticatorB.verify(token, m -> Result.VALID);
    Assertions.assertFalse(result.isValid(), result::cause);
  }

  @Test
  public void test8() {
    Algorithm algorithm = Algorithm.HMAC384(randomKey(384));
    SimpleJwtAuthenticator authenticatorA =
        SimpleJwtAuthenticator.builder()
            .signatureAlgorithm(algorithm)
            .headers(Map.of("A", new TestObj(1, "s")))
            .build();
    SimpleJwtAuthenticator authenticatorB =
        SimpleJwtAuthenticator.builder()
            .signatureAlgorithm(algorithm)
            .headers(Map.of("B", new TestObj(1, "s")))
            .build();
    String token = authenticatorA.generate(PAYLOAD);
    Result result = authenticatorB.verify(token, m -> Result.VALID);
    Assertions.assertTrue(result.isValid(), result::cause);
  }

  @Test
  public void testHMC256WithoutExp() {
    Algorithm algorithm = Algorithm.HMAC256(randomKey(256));
    Result result = doTest(algorithm, null, 0, null);
    Assertions.assertTrue(result.isValid(), result::cause);
  }

  @Test
  public void testHMC256Exp() {
    Algorithm algorithm = Algorithm.HMAC256(randomKey(256));
    Result result =
        doTest(algorithm, Duration.of(3, SECONDS), 0, SimpleJwtAuthenticatorTests::sleep);
    Assertions.assertFalse(result.isValid(), result::cause);
  }

  @Test
  public void testHMC256Exp1() {
    Algorithm algorithm = Algorithm.HMAC256(randomKey(256));
    Result result =
        doTest(algorithm, Duration.of(3, SECONDS), 1, SimpleJwtAuthenticatorTests::sleep);
    Assertions.assertFalse(result.isValid(), result::cause);
  }

  @Test
  public void testHMC256Exp2() {
    Algorithm algorithm = Algorithm.HMAC256(randomKey(256));
    Result result =
        doTest(algorithm, Duration.of(3, SECONDS), 3, SimpleJwtAuthenticatorTests::sleep);
    Assertions.assertTrue(result.isValid(), result::cause);
  }

  @Test
  public void testHMC384WithoutExp() {
    Algorithm algorithm = Algorithm.HMAC384(randomKey(384));
    Result result = doTest(algorithm, null, 0, null);
    Assertions.assertTrue(result.isValid(), result::cause);
  }

  @Test
  public void testHMC384Exp() {
    Algorithm algorithm = Algorithm.HMAC384(randomKey(384));
    Result result =
        doTest(algorithm, Duration.of(3, SECONDS), 0, SimpleJwtAuthenticatorTests::sleep);
    Assertions.assertFalse(result.isValid(), result::cause);
  }

  @Test
  public void testHMC384Exp1() {
    Algorithm algorithm = Algorithm.HMAC384(randomKey(384));
    Result result =
        doTest(algorithm, Duration.of(3, SECONDS), 1, SimpleJwtAuthenticatorTests::sleep);
    Assertions.assertFalse(result.isValid(), result::cause);
  }

  @Test
  public void testHMC384Exp2() {
    Algorithm algorithm = Algorithm.HMAC384(randomKey(384));
    Result result =
        doTest(algorithm, Duration.of(3, SECONDS), 3, SimpleJwtAuthenticatorTests::sleep);
    Assertions.assertTrue(result.isValid(), result::cause);
  }

  @Test
  public void testHMC512WithoutExp() {
    Algorithm algorithm = Algorithm.HMAC512(randomKey(512));
    Result result = doTest(algorithm, null, 0, null);
    Assertions.assertTrue(result.isValid(), result::cause);
  }

  @Test
  public void testHMC512Exp() {
    Algorithm algorithm = Algorithm.HMAC512(randomKey(512));
    Result result =
        doTest(algorithm, Duration.of(3, SECONDS), 0, SimpleJwtAuthenticatorTests::sleep);
    Assertions.assertFalse(result.isValid(), result::cause);
  }

  @Test
  public void testHMC512Exp1() {
    Algorithm algorithm = Algorithm.HMAC512(randomKey(512));
    Result result =
        doTest(algorithm, Duration.of(3, SECONDS), 1, SimpleJwtAuthenticatorTests::sleep);
    Assertions.assertFalse(result.isValid(), result::cause);
  }

  @Test
  public void testHMC512Exp2() {
    Algorithm algorithm = Algorithm.HMAC512(randomKey(512));
    Result result =
        doTest(algorithm, Duration.of(3, SECONDS), 3, SimpleJwtAuthenticatorTests::sleep);
    Assertions.assertTrue(result.isValid(), result::cause);
  }

  private Result doTest(
      Algorithm algorithm, Duration period, Integer leeway, Consumer<Duration> timeConsumer) {
    SimpleJwtAuthenticator authenticator =
        SimpleJwtAuthenticator.builder()
            .signatureAlgorithm(algorithm)
            .headers(HEADERS)
            .leeway(leeway)
            .period(period)
            .audiences(
                new String[] {
                  RandomStringUtils.random(RandomUtils.nextInt(1, 128)),
                  RandomStringUtils.random(RandomUtils.nextInt(1, 128)),
                  RandomStringUtils.random(RandomUtils.nextInt(1, 128)),
                  RandomStringUtils.random(RandomUtils.nextInt(1, 128))
                })
            .issuer(RandomStringUtils.random(RandomUtils.nextInt(1, 128)))
            .jwtId(RandomStringUtils.random(RandomUtils.nextInt(1, 128)))
            .subject(RandomStringUtils.random(RandomUtils.nextInt(1, 128)))
            .build();
    String token = authenticator.generate(PAYLOAD);
    if (timeConsumer != null) {
      timeConsumer.accept(period);
    }
    return authenticator.verify(token, SimpleJwtAuthenticatorTests::check);
  }

  @SneakyThrows
  private static void sleep(Duration d) {
    Thread.sleep(d.toMillis() + 2000);
  }
}
