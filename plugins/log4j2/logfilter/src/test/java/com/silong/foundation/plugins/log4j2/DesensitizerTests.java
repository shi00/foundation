/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.silong.foundation.plugins.log4j2;

import com.github.javafaker.Faker;
import com.silong.foundation.crypto.RootKey;
import com.silong.foundation.crypto.aes.AesGcmToolkit;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static com.silong.foundation.plugins.log4j2.BuildInDesensitizer.*;
import static com.silong.foundation.plugins.log4j2.Desensitizer.DEFAULT_REPLACE_STR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 脱敏器单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-22 22:11
 */
public class DesensitizerTests {

  static Desensitizer desensitizer = BuildInDesensitizer.getInstance();

  static RootKey rootKey;

  String workKey;

  String plaintext;

  @BeforeAll
  static void init() throws IOException {
    Path dir = new File("target/test-classes").toPath();
    RootKey.export(
        RootKey.DEFAULT_ROOT_KEY_PARTS.stream()
            .map(s -> dir.resolve(s).toFile())
            .toArray(File[]::new));
    rootKey = RootKey.initialize();
  }

  @BeforeEach
  void initEatch() {
    workKey = rootKey.encryptWorkKey(RandomStringUtils.random(RandomUtils.nextInt(1, Short.SIZE)));
    plaintext = RandomStringUtils.random(RandomUtils.nextInt(1, Short.SIZE));
  }

  @Test
  @DisplayName("IMEI-1")
  void test1() {
    String imei = RandomIMEI.generateIMEI();
    String desensitize = IMEI.desensitize(imei);
    assertEquals(DEFAULT_REPLACE_STR, desensitize);
  }

  @Test
  @DisplayName("IMEI-2")
  void test2() {
    String imei1 = RandomIMEI.generateIMEI();
    String imei2 = RandomIMEI.generateIMEI();
    String formatter = "%s%s%s%s%s";
    String s1 = RandomStringUtils.random(10, true, false);
    String s2 = RandomStringUtils.random(10, true, false);
    String s3 = RandomStringUtils.random(10, true, false);
    String desensitize = IMEI.desensitize(String.format(formatter, s1, imei1, s2, imei2, s3));
    assertEquals(
        String.format(formatter, s1, DEFAULT_REPLACE_STR, s2, DEFAULT_REPLACE_STR, s3),
        desensitize);
  }

  @Test
  @DisplayName("IMEI-3")
  void test3() {
    String imei1 = RandomIMEI.generateIMEI();
    String imei2 = RandomIMEI.generateIMEI();
    String desensitize = IMEI.desensitize(imei1 + imei2);
    assertTrue(desensitize.contains(DEFAULT_REPLACE_STR));
  }

  @Test
  @DisplayName("IMEI-4")
  void test4() {
    String imei1 = RandomIMEI.generateIMEI();
    String imei2 = RandomIMEI.generateIMEI();
    String desensitize = IMEI.desensitize("a" + imei1 + imei2 + "b");
    assertEquals("a" + DEFAULT_REPLACE_STR + DEFAULT_REPLACE_STR + "b", desensitize);
  }

  @Test
  @DisplayName("PASSWORD-1")
  void test5() {
    String password = RandomIPassword.generatePassword(8);
    String desensitize = PASSWORD.desensitize(password);
    assertEquals(DEFAULT_REPLACE_STR, desensitize);
  }

  @Test
  @DisplayName("PASSWORD-2")
  void test6() {
    String password = RandomIPassword.generatePassword(16);
    String desensitize = PASSWORD.desensitize(password);
    assertEquals(DEFAULT_REPLACE_STR, desensitize);
  }

  @Test
  @DisplayName("PASSWORD-3")
  void test7() {
    String password = RandomIPassword.generatePassword(20);
    String desensitize = PASSWORD.desensitize(password);
    assertEquals(DEFAULT_REPLACE_STR, desensitize);
  }

  @Test
  @DisplayName("PASSWORD-4")
  void test8() {
    String password = RandomIPassword.generatePassword(21);
    String desensitize = PASSWORD.desensitize(password);
    assertTrue(desensitize.contains(DEFAULT_REPLACE_STR));
  }

  @Test
  @DisplayName("PASSWORD-5")
  void test9() {
    String password = RandomIPassword.generatePassword(7);
    String desensitize = PASSWORD.desensitize(password);
    assertEquals(password, desensitize);
  }

  @Test
  @DisplayName("PASSWORD-6")
  void test10() {
    String password1 = RandomIPassword.generatePassword(10);
    String password2 = RandomIPassword.generatePassword(14);
    String formatter = "%s%s%s%s%s";
    String s1 = RandomStringUtils.random(10, true, false);
    String s2 = RandomStringUtils.random(10, true, false);
    String s3 = RandomStringUtils.random(10, true, false);
    String desensitize =
        PASSWORD.desensitize(String.format(formatter, s1, password1, s2, password2, s3));
    assertTrue(desensitize.contains(DEFAULT_REPLACE_STR));
  }

  @Test
  @DisplayName("PASSWORD-7")
  void test11() {
    String password1 = RandomIPassword.generatePassword(10);
    String password2 = RandomIPassword.generatePassword(14);
    String desensitize = PASSWORD.desensitize(password2 + password1);
    assertTrue(desensitize.contains(DEFAULT_REPLACE_STR));
  }

  @Test
  @DisplayName("PASSWORD-8")
  void test12() {
    String password1 = RandomIPassword.generatePassword(15);
    String password2 = RandomIPassword.generatePassword(8);
    String desensitize = PASSWORD.desensitize("d" + password2 + password1 + "a");
    assertTrue(desensitize.contains(DEFAULT_REPLACE_STR));
  }

  @Test
  @DisplayName("SECURITY-1")
  void test13() {
    String encrypt = AesGcmToolkit.encrypt(plaintext, workKey);
    String desensitize = SECURITY_BASE64.desensitize(encrypt);
    assertEquals(DEFAULT_REPLACE_STR, desensitize);
  }

  @Test
  @DisplayName("SECURITY-2")
  void test14() {
    String encrypt1 = AesGcmToolkit.encrypt(plaintext, workKey);
    plaintext = RandomStringUtils.random(RandomUtils.nextInt(1, Short.SIZE));
    String encrypt2 = AesGcmToolkit.encrypt(plaintext, workKey);
    String desensitize = SECURITY_BASE64.desensitize(encrypt1 + "abc" + encrypt2);
    assertEquals(DEFAULT_REPLACE_STR + "abc" + DEFAULT_REPLACE_STR, desensitize);
  }

  @Test
  @DisplayName("SECURITY-3")
  void test15() {
    String encrypt1 = AesGcmToolkit.encrypt(plaintext, workKey);
    plaintext = RandomStringUtils.random(RandomUtils.nextInt(1, Short.SIZE));
    String encrypt2 = AesGcmToolkit.encrypt(plaintext, workKey);
    String desensitize = SECURITY_BASE64.desensitize(encrypt1 + encrypt2);
    assertEquals(DEFAULT_REPLACE_STR + DEFAULT_REPLACE_STR, desensitize);
  }

  @Test
  @DisplayName("SECURITY-4")
  void test16() {
    String encrypt1 = AesGcmToolkit.encrypt(plaintext, workKey);
    plaintext = RandomStringUtils.random(RandomUtils.nextInt(1, Short.SIZE));
    String encrypt2 = AesGcmToolkit.encrypt(plaintext, workKey);
    String desensitize = SECURITY_BASE64.desensitize("a" + encrypt1 + "b" + encrypt2 + "c");
    assertEquals("a" + DEFAULT_REPLACE_STR + "b" + DEFAULT_REPLACE_STR + "c", desensitize);
  }

  @Test
  @DisplayName("EMAIL")
  void test17() {
    Faker faker = new Faker();
    for (int i = 0; i < 100; i++) {
      String emailAddress = faker.internet().emailAddress();
      String desensitize = EMAIL.desensitize(emailAddress);
      assertEquals(emailAddress, desensitize);
    }
  }

  //  @Test
  //  @DisplayName("CREDITCARD-1")
  //  void test18() {
  //    Faker faker = new Faker();
  //    faker.idNumber().
  //    String emailAddress1 = faker.internet().emailAddress();
  //    String emailAddress2 = faker.internet().emailAddress();
  //    String desensitize = EMAIL.desensitize(emailAddress1 + emailAddress2);
  //    assertEquals(DEFAULT_REPLACE_STR, desensitize);
  //  }

  //  @Test
  //  @DisplayName("IMEI-2")
  //  void test33() {
  //    String imei1 = "443187498504422";
  //    String imei2 = "336919698689615";
  //    String s1 = RandomStringUtils.randomAlphanumeric(10);
  //    String s2 = RandomStringUtils.randomAlphanumeric(100);
  //    String s3 = RandomStringUtils.randomAlphanumeric(20);
  //    String desensitize = desensitizer.desensitize(s1 + imei1 + s2 + imei2 + s3);
  //    assertEquals(s1 + DEFAULT_REPLACE_STR + s2 + DEFAULT_REPLACE_STR + s3, desensitize);
  //  }
}
