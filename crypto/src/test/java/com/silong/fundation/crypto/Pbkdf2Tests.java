package com.silong.fundation.crypto;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import static com.silong.fundation.crypto.Pbkdf2.MIN_SALT_LENGTH;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2021-12-26 10:27
 */
public class Pbkdf2Tests {

  @Test
  void test1() throws NoSuchAlgorithmException, InvalidKeySpecException {
    byte[] salt = ThreadLocalSecureRandom.random(MIN_SALT_LENGTH);
    String chars = RandomStringUtils.random(RandomUtils.nextInt(1, 1000));
    byte[] array = Pbkdf2.generate(chars.toCharArray(), 20000, salt, 256);
    assertEquals(array.length * Byte.SIZE, 256);
  }

  @Test
  void test2() throws NoSuchAlgorithmException, InvalidKeySpecException {
    byte[] salt = ThreadLocalSecureRandom.random(MIN_SALT_LENGTH * 10);
    String chars = RandomStringUtils.random(RandomUtils.nextInt(1, Short.SIZE));
    byte[] array = Pbkdf2.generate(chars.toCharArray(), 20000, salt, 128);
    assertEquals(array.length * Byte.SIZE, 128);
  }

  @Test
  void test3() {
    Assertions.assertThrowsExactly(
        IllegalArgumentException.class,
        () -> {
          byte[] salt = ThreadLocalSecureRandom.random(MIN_SALT_LENGTH - 1);
          String chars = RandomStringUtils.random(RandomUtils.nextInt(1, Short.SIZE));
          byte[] array = Pbkdf2.generate(chars.toCharArray(), 20000, salt, 128);
        });
  }
}
