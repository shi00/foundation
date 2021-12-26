package com.silong.fundation.crypto;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.security.spec.AlgorithmParameterSpec;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static javax.crypto.Cipher.DECRYPT_MODE;
import static javax.crypto.Cipher.ENCRYPT_MODE;

/**
 * 加密器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2021-12-26 13:17
 */
public class ThreadLocalCipher {

  /** 为了线程复用Cipher对象，此处不能在每次调用后remove */
  private static final ThreadLocal<Map<String, Cipher>> TL_CIPHER =
      ThreadLocal.withInitial(HashMap::new);

  @FunctionalInterface
  private interface Function5<T1, T2, T3, T4, R> {
    /**
     * 加密/解密
     *
     * @param t1 param1
     * @param t2 param2
     * @param t3 param3
     * @param t4 param4
     * @return result
     */
    R apply(T1 t1, T2 t2, T3 t3, T4 t4);
  }

  /**
   * 加密数据
   *
   * @param plainBytes 明文数据
   * @param offset 数据偏移位置
   * @param length 数据长度
   * @param algorithm 算法
   * @param key 加密密钥
   * @param spec 算法参数
   * @return 加密结果
   */
  public static byte[] encrypt(
      byte[] plainBytes,
      int offset,
      int length,
      String algorithm,
      Key key,
      AlgorithmParameterSpec spec) {
    try {
      return getInstance(algorithm, ENCRYPT_MODE, key, spec).doFinal(plainBytes, offset, length);
    } catch (IllegalBlockSizeException | BadPaddingException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * 加密数据
   *
   * @param plainBytes 明文数据
   * @param offset 数据偏移位置
   * @param length 数据长度
   * @param algorithm 算法
   * @param key 加密密钥
   * @param spec 算法参数
   * @param doFinal 加密方法
   * @return 加密结果
   */
  public static byte[] encrypt(
      byte[] plainBytes,
      int offset,
      int length,
      String algorithm,
      Key key,
      AlgorithmParameterSpec spec,
      Function5<Cipher, byte[], Integer, Integer, byte[]> doFinal) {
    return requireNonNull(doFinal, "doFinal must not be null.")
        .apply(getInstance(algorithm, ENCRYPT_MODE, key, spec), plainBytes, offset, length);
  }

  /**
   * 解密数据
   *
   * @param cipherBytes 加密数据
   * @param offset 数据偏移位置
   * @param length 数据长度
   * @param algorithm 算法
   * @param key 解密密钥
   * @param spec 算法参数
   * @return 解密结果
   */
  public static byte[] decrypt(
      byte[] cipherBytes,
      int offset,
      int length,
      String algorithm,
      Key key,
      AlgorithmParameterSpec spec) {
    try {
      return getInstance(algorithm, DECRYPT_MODE, key, spec).doFinal(cipherBytes, offset, length);
    } catch (IllegalBlockSizeException | BadPaddingException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * 解密数据
   *
   * @param cipherBytes 加密数据
   * @param offset 数据偏移位置
   * @param length 数据长度
   * @param algorithm 解密算法
   * @param key 解密密钥
   * @param spec 算法参数，可能是null
   * @param doFinal 解密方法
   * @return 解密结果
   */
  public static byte[] decrypt(
      byte[] cipherBytes,
      int offset,
      int length,
      String algorithm,
      Key key,
      AlgorithmParameterSpec spec,
      Function5<Cipher, byte[], Integer, Integer, byte[]> doFinal) {
    return requireNonNull(doFinal, "doFinal must not be null.")
        .apply(getInstance(algorithm, DECRYPT_MODE, key, spec), cipherBytes, offset, length);
  }

  private static Cipher getInstance(
      String algorithm, int opMode, Key key, AlgorithmParameterSpec spec) {
    try {
      Cipher cipher = TL_CIPHER.get().computeIfAbsent(algorithm, ThreadLocalCipher::getInstance);
      SecureRandom secureRandom = ThreadLocalSecureRandom.get();
      if (spec == null) {
        cipher.init(opMode, key, secureRandom);
      } else {
        cipher.init(opMode, key, spec, secureRandom);
      }
      return cipher;
    } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
      throw new RuntimeException(e);
    }
  }

  private static Cipher getInstance(String algorithm) {
    try {
      return Cipher.getInstance(algorithm);
    } catch (NoSuchPaddingException | NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }
}
