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

package com.silong.foundation.dj.longhaul.serialize;

import com.silong.foundation.common.utils.BiConverter;

/**
 * int转byte数组
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-10 18:30
 */
public class Int2BytesSerializer implements BiConverter<Integer, byte[]> {

  /** 单例 */
  public static final Int2BytesSerializer INSTANCE = new Int2BytesSerializer();

  /** 构造方法 */
  private Int2BytesSerializer() {}

  @Override
  public byte[] to(Integer value) {
    if (value == null) {
      return null;
    }
    return new byte[] {
      (byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value.intValue()
    };
  }

  @Override
  public Integer from(byte[] bytes) {
    if (bytes == null || bytes.length != Integer.BYTES) {
      return null;
    }
    return ((bytes[0] & 0xFF) << 24)
        | ((bytes[1] & 0xFF) << 16)
        | ((bytes[2] & 0xFF) << 8)
        | ((bytes[3] & 0xFF));
  }
}
