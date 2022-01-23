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

/**
 * 加密字符串包裹工具
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2021-12-26 19:28
 */
public final class SecurityWrapper {

  private static final String SECURITY_PREFIX = "security:";

  private static final String SECURITY_FORMAT = SECURITY_PREFIX + "%s";

  /**
   * 使用特定前缀格式化加密字符串
   *
   * @param cipherText 加密字符串
   * @return 格式化结果
   */
  public static String wrap(String cipherText) {
    if (cipherText == null || cipherText.isEmpty()) {
      throw new IllegalArgumentException("cipherText must not be null or empty.");
    }
    return String.format(SECURITY_FORMAT, cipherText);
  }

  /**
   * 剔除安全格式化后的字符串包裹信息
   *
   * @param securityText 包裹字符串
   * @return 解包结果
   */
  public static String unwrap(String securityText) {
    if (securityText == null || securityText.isEmpty()) {
      throw new IllegalArgumentException("securityText must not be null or empty.");
    }
    if (!securityText.startsWith(SECURITY_PREFIX)) {
      throw new IllegalArgumentException(
          String.format(
              "securityText format is not valid, and valid securityText must start with %s.",
              SECURITY_PREFIX));
    }
    return securityText.substring(SECURITY_PREFIX.length());
  }
}
