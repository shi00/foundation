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
package com.silong.foundation.crypto.aes;

import static com.silong.foundation.crypto.RootKey.ENABLED_CACHE;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.silong.foundation.crypto.RootKey;
import com.silong.foundation.crypto.utils.SecurityWrapper;
import com.silong.foundation.crypto.utils.ThreadLocalCipher;
import com.silong.foundation.crypto.utils.ThreadLocalSecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES GCM加解密工具
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2021-12-26 09:20
 */
public final class AesGcmToolkit {

  public static final String AES = "AES";

  private static final String GCM = "AES/GCM/NoPadding";

  private static final int GCM_IV_LENGTH = 12;

  private static final int GCM_TAG_LENGTH = 16 * Byte.SIZE;

  private static final Map<String, SecretKey> WK_CACHE = new ConcurrentHashMap<>();

  /** 禁止实例化 */
  private AesGcmToolkit() {}

  /**
   * 计算Sk
   *
   * @param workKey 工作密钥
   * @return sk
   */
  private static SecretKey decryptWorkKey(String workKey) {
    return ENABLED_CACHE
        ? WK_CACHE.computeIfAbsent(
            workKey, k -> new SecretKeySpec(RootKey.getInstance().decryptWorkKey(k), AES))
        : new SecretKeySpec(RootKey.getInstance().decryptWorkKey(workKey), AES);
  }

  /**
   * 使用指定密钥和初始向量加密数据
   *
   * @param plainBytes 明文数据
   * @param offset 数据偏移
   * @param length 数据长度
   * @param key 密钥
   * @param iv 初始向量
   * @return 加密结果
   */
  public static String encrypt(
      byte[] plainBytes, int offset, int length, SecretKey key, byte[] iv) {
    byte[] encrypt =
        ThreadLocalCipher.encrypt(
            plainBytes, offset, length, GCM, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
    byte[] bytes = new byte[encrypt.length + iv.length];
    System.arraycopy(encrypt, 0, bytes, 0, encrypt.length);
    System.arraycopy(iv, 0, bytes, encrypt.length, iv.length);
    String encode = Base64.getEncoder().encodeToString(bytes);
    return SecurityWrapper.wrap(encode);
  }

  /**
   * 解密数据
   *
   * @param cipherBytes 加密数据
   * @param offset 数据偏移
   * @param length 数据长度
   * @param key 解密密钥
   * @return 解密结果
   */
  public static byte[] decrypt(byte[] cipherBytes, int offset, int length, SecretKey key) {
    return ThreadLocalCipher.decrypt(
        cipherBytes,
        offset,
        length - GCM_IV_LENGTH,
        GCM,
        key,
        new GCMParameterSpec(
            GCM_TAG_LENGTH, cipherBytes, offset + length - GCM_IV_LENGTH, GCM_IV_LENGTH));
  }

  /**
   * 使用给定密钥解密字符串
   *
   * @param cipherText 加密字符串
   * @param key 密钥
   * @return 解密结果
   */
  public static byte[] decrypt(String cipherText, SecretKey key) {
    String unwrap = SecurityWrapper.unwrap(cipherText);
    byte[] decode = Base64.getDecoder().decode(unwrap);
    return decrypt(decode, 0, decode.length, key);
  }

  /**
   * 使用工作密钥加密字符串
   *
   * @param plainText 明文字符串
   * @param workKey 工作密钥
   * @return 加密结果
   */
  public static String encrypt(String plainText, String workKey) {
    if (plainText == null || plainText.isEmpty()) {
      throw new IllegalArgumentException("plainText must not be null or empty.");
    }
    if (workKey == null || workKey.isEmpty()) {
      throw new IllegalArgumentException("workKey must not be null or empty.");
    }
    byte[] bytes = plainText.getBytes(UTF_8);
    return encrypt(bytes, 0, bytes.length, decryptWorkKey(workKey), randomIv());
  }

  /**
   * 使用工作密钥解密字符串
   *
   * @param cipherText 加密字符串
   * @param workKey 工作密钥
   * @return 解密结果
   */
  public static String decrypt(String cipherText, String workKey) {
    if (cipherText == null || cipherText.isEmpty()) {
      throw new IllegalArgumentException("cipherText must not be null or empty.");
    }
    if (workKey == null || workKey.isEmpty()) {
      throw new IllegalArgumentException("workKey must not be null or empty.");
    }
    return new String(decrypt(cipherText, decryptWorkKey(workKey)), UTF_8);
  }

  /**
   * 随机生成AES GCM初始向量
   *
   * @return 初始向量
   */
  public static byte[] randomIv() {
    return ThreadLocalSecureRandom.random(GCM_IV_LENGTH);
  }
}
