package com.silong.fundation.crypto.pbkdf2;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import static com.silong.fundation.crypto.utils.ThreadLocalSecureRandom.random;

/**
 * PBKDF2密钥生成工具，使用PBKDF2WithHmacSHA512算法。<br>
 * 盐值最小长度为16，小于此长度会抛出异常。
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2021-12-26 09:24
 */
public final class Pbkdf2 {

  /** 最小有效盐值，为确保安全，盐值至少16 */
  public static final int MIN_SALT_LENGTH = 16;

  private static final String PBKDF_2_WITH_HMAC_SHA_512 = "PBKDF2WithHmacSHA512";

  /**
   * 默认迭代次数：10000<br>
   * 可以通过pbkdf2.iteration.count在启动时配置
   */
  private static final int DEFAULT_ITERATIONS =
      Integer.parseInt(System.getProperty("pbkdf2.iteration.count", "10000"));

  /** 禁止实例化 */
  private Pbkdf2() {}

  /**
   * 根据给字符数组生成指定长度的密钥
   *
   * @param chars 字符串
   * @param iterations 算法迭代次数
   * @param salt 盐值
   * @param keyLength 密钥长度，单位：bit
   * @return 密钥
   */
  public static byte[] generate(char[] chars, int iterations, byte[] salt, int keyLength)
      throws NoSuchAlgorithmException, InvalidKeySpecException {
    if (salt == null || salt.length < MIN_SALT_LENGTH) {
      throw new IllegalArgumentException(
          String.format("length of salt must be greater than %d.", MIN_SALT_LENGTH));
    }
    return SecretKeyFactory.getInstance(PBKDF_2_WITH_HMAC_SHA_512)
        .generateSecret(new PBEKeySpec(chars, salt, iterations, keyLength))
        .getEncoded();
  }

  /**
   * 根据给字符数组生成指定长度的密钥
   *
   * @param chars 字符串
   * @param salt 盐值
   * @param keyLength 密钥长度，单位：bit
   * @return 密钥
   */
  public static byte[] generate(char[] chars, byte[] salt, int keyLength)
      throws NoSuchAlgorithmException, InvalidKeySpecException {
    return generate(chars, DEFAULT_ITERATIONS, salt, keyLength);
  }

  /**
   * 根据给字符数组生成指定长度的密钥
   *
   * @param chars 字符串
   * @param keyLength 密钥长度，单位：bit
   * @return 密钥
   */
  public static byte[] generate(char[] chars, int keyLength)
      throws NoSuchAlgorithmException, InvalidKeySpecException {
    return generate(chars, DEFAULT_ITERATIONS, random(MIN_SALT_LENGTH), keyLength);
  }
}
