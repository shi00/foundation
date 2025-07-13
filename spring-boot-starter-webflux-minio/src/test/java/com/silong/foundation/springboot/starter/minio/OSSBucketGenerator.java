/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package com.silong.foundation.springboot.starter.minio;

import java.util.Random;

/**
 * 随机生成符合要求的obs桶名
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-03-27 21:33
 */
public final class OSSBucketGenerator {
  private static final String FIRST_LAST_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
  private static final String MIDDLE_CHARS = FIRST_LAST_CHARS + "-";
  private static final Random random = new Random();

  private OSSBucketGenerator() {}

  public static String generate(int length) {
    if (length < 3 || length > 63) {
      throw new IllegalArgumentException("长度需在3-63之间");
    }

    StringBuilder sb = new StringBuilder(length);
    // 首字符（字母/数字）
    sb.append(FIRST_LAST_CHARS.charAt(random.nextInt(FIRST_LAST_CHARS.length())));

    // 中间部分（允许短横线）
    for (int i = 1; i < length - 1; i++) {
      sb.append(MIDDLE_CHARS.charAt(random.nextInt(MIDDLE_CHARS.length())));
    }

    // 尾字符（字母/数字）
    sb.append(FIRST_LAST_CHARS.charAt(random.nextInt(FIRST_LAST_CHARS.length())));

    return sb.toString();
  }
}
