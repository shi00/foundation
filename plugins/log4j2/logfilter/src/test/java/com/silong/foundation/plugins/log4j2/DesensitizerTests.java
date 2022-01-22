package com.silong.foundation.plugins.log4j2;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.silong.foundation.plugins.log4j2.BuildInDesensitizer.IMEI;
import static com.silong.foundation.plugins.log4j2.Desensitizer.DEFAULT_REPLACE_STR;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
    assertEquals(DEFAULT_REPLACE_STR + DEFAULT_REPLACE_STR, desensitize);
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
  @DisplayName("IMEI-2")
  void test33() {
    String imei1 = "443187498504422";
    String imei2 = "336919698689615";
    String s1 = RandomStringUtils.randomAlphanumeric(10);
    String s2 = RandomStringUtils.randomAlphanumeric(100);
    String s3 = RandomStringUtils.randomAlphanumeric(20);
    String desensitize = desensitizer.desensitize(s1 + imei1 + s2 + imei2 + s3);
    assertEquals(s1 + DEFAULT_REPLACE_STR + s2 + DEFAULT_REPLACE_STR + s3, desensitize);
  }
}
