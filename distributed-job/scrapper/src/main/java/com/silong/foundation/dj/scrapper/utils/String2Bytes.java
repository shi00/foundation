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

package com.silong.foundation.dj.scrapper.utils;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.silong.foundation.common.utils.BiConverter;

/**
 * 字符串，字节数组转换器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-10 18:47
 */
public class String2Bytes implements BiConverter<String, byte[]> {

  /** 单例 */
  public static final String2Bytes INSTANCE = new String2Bytes();

  /** 构造方法 */
  private String2Bytes() {}

  @Override
  public byte[] to(String str) {
    if (str == null) {
      return null;
    }
    return str.getBytes(UTF_8);
  }

  @Override
  public String from(byte[] bytes) {
    if (bytes == null) {
      return null;
    }
    return new String(bytes, UTF_8);
  }
}
