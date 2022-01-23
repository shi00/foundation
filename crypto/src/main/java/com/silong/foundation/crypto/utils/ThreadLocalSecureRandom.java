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

import java.security.SecureRandom;

/**
 * 安全随机数线程局部变量
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2021-12-26 09:27
 */
public final class ThreadLocalSecureRandom {

  private static final ThreadLocal<SecureRandom> TLSR = ThreadLocal.withInitial(SecureRandom::new);

  /** 工具类，禁止实例化 */
  private ThreadLocalSecureRandom() {}

  /**
   * 获取安全随机数生成器
   *
   * @return 随机数生成器
   */
  public static SecureRandom get() {
    return TLSR.get();
  }

  /**
   * 生成指定长度字节数组
   *
   * @param size 数组长度
   * @return 数组
   * @throws IllegalArgumentException size < 0
   */
  public static byte[] random(int size) {
    if (size < 0) {
      throw new IllegalArgumentException("size must be greater than or equals to 0.");
    }
    byte[] array = new byte[size];
    TLSR.get().nextBytes(array);
    return array;
  }

  /**
   * 生成随机字节数组
   *
   * @param array 字节数组
   * @return array
   * @throws NullPointerException array == null
   */
  public static byte[] random(byte[] array) {
    if (array == null || array.length == 0) {
      throw new IllegalArgumentException("array must not be null or empty.");
    }
    TLSR.get().nextBytes(array);
    return array;
  }
}
