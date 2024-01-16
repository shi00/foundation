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
package com.silong.foundation.crypto.digest;

import com.silong.foundation.crypto.RootKey;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * hmac工具
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-16 11:57
 */
public final class HmacToolkit {

  private static final String HMAC_SHA_256 = "HmacSHA256";

  private static final String HMAC_SHA_512 = "HmacSHA512";

  private static final ThreadLocal<Mac> HMAC_SHA_256_TL =
      ThreadLocal.withInitial(() -> getInstance(HMAC_SHA_256));

  private static final ThreadLocal<Mac> HMAC_SHA_512_TL =
      ThreadLocal.withInitial(() -> getInstance(HMAC_SHA_512));

  private static final Map<String, SecretKeySpec> CACHE = new ConcurrentHashMap<>();

  private static Mac getInstance(String algorithm) {
    try {
      return Mac.getInstance(algorithm);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  private static SecretKeySpec getKey(String workKey, String algorithm) {
    return CACHE.computeIfAbsent(
        workKey, k -> new SecretKeySpec(RootKey.getInstance().decryptWorkKey(workKey), algorithm));
  }

  /**
   * hmacsha512
   *
   * @param plainText 明文
   * @param workKey 工作密钥
   * @return 结果
   */
  public static byte[] hmacSha512Hash(String plainText, String workKey) {
    if (plainText == null || plainText.isEmpty()) {
      throw new IllegalArgumentException("plainText must not be null or empty.");
    }
    if (workKey == null || workKey.isEmpty()) {
      throw new IllegalArgumentException("workKey must not be null or empty.");
    }
    try {
      Mac mac = HMAC_SHA_512_TL.get();
      mac.init(getKey(workKey, HMAC_SHA_512));
      return mac.doFinal(plainText.getBytes(UTF_8));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * hmacsha256
   *
   * @param plainText 明文
   * @param workKey 工作密钥
   * @return 结果
   */
  public static byte[] hmacSha256Hash(String plainText, String workKey) {
    if (plainText == null || plainText.isEmpty()) {
      throw new IllegalArgumentException("plainText must not be null or empty.");
    }
    if (workKey == null || workKey.isEmpty()) {
      throw new IllegalArgumentException("workKey must not be null or empty.");
    }
    try {
      Mac mac = HMAC_SHA_256_TL.get();
      mac.init(getKey(workKey, HMAC_SHA_256));
      return mac.doFinal(plainText.getBytes(UTF_8));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * hmacsha256
   *
   * @param plainText 明文
   * @param workKey 工作密钥
   * @return 结果
   */
  public static String hmacSha256(String plainText, String workKey) {
    return Base64.getEncoder().encodeToString(hmacSha256Hash(plainText, workKey));
  }

  /**
   * hmacsha512
   *
   * @param plainText 明文
   * @param workKey 工作密钥
   * @return 结果
   */
  public static String hmacSha512(String plainText, String workKey) {
    return Base64.getEncoder().encodeToString(hmacSha512Hash(plainText, workKey));
  }
}
