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
package com.silong.foundation.crypto.utils;

import static javax.crypto.Cipher.DECRYPT_MODE;
import static javax.crypto.Cipher.ENCRYPT_MODE;

import java.security.*;
import java.security.spec.AlgorithmParameterSpec;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * 加密器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2021-12-26 13:17
 */
public class ThreadLocalCipher {

  /**
   * Represents a function that accepts four arguments and produces a result. This is the four-arity
   * specialization of Function.
   *
   * @author louis sin
   * @version 1.0.0
   * @since 2022-01-012 16:23
   * @param <T1> the type of the first argument to the function
   * @param <T2> the type of the second argument to the function
   * @param <T3> the type of the third argument to the function
   * @param <T4> the type of the fourth argument to the function
   * @param <R> the type of the result of the function
   */
  public interface Function4<T1, T2, T3, T4, R> {
    /**
     * apply
     *
     * @param t1 param1
     * @param t2 param2
     * @param t3 param3
     * @param t4 param4
     * @return result
     */
    R apply(T1 t1, T2 t2, T3 t3, T4 t4);
  }

  /** 为了线程复用Cipher对象，此处不能在每次调用后remove */
  private static final ThreadLocal<Map<String, Cipher>> TL_CIPHER =
      ThreadLocal.withInitial(HashMap::new);

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
      Function4<Cipher, byte[], Integer, Integer, byte[]> doFinal) {
    if (doFinal == null) {
      throw new IllegalArgumentException("doFinal must not be null.");
    }
    return doFinal.apply(
        getInstance(algorithm, ENCRYPT_MODE, key, spec), plainBytes, offset, length);
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
      Function4<Cipher, byte[], Integer, Integer, byte[]> doFinal) {
    if (doFinal == null) {
      throw new IllegalArgumentException("doFinal must not be null.");
    }
    return doFinal.apply(
        getInstance(algorithm, DECRYPT_MODE, key, spec), cipherBytes, offset, length);
  }

  private static Cipher getInstance(
      String algorithm, int opMode, Key key, AlgorithmParameterSpec spec) {
    if (algorithm == null || algorithm.isEmpty()) {
      throw new IllegalArgumentException("algorithm must not be null or empty.");
    }
    if (key == null) {
      throw new IllegalArgumentException("key must not be null.");
    }
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
