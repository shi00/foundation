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
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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

//  @Test
//  @DisplayName("PHONENUMBER-1")
//  void test13() {
//    Faker faker = new Faker();
//    String cellPhone = faker.phoneNumber().cellPhone();
//    String desensitize = INTERNATIONAL_PHONE_NUMBER.desensitize(cellPhone);
//    assertEquals(cellPhone, desensitize);
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
