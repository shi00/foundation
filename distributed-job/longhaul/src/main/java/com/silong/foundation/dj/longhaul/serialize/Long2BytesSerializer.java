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
 * long转byte数组
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-10 18:30
 */
public class Long2BytesSerializer implements BiConverter<Long, byte[]> {

  /** 单例 */
  public static final Long2BytesSerializer INSTANCE = new Long2BytesSerializer();

  /** 构造方法 */
  private Long2BytesSerializer() {}

  @Override
  public byte[] to(Long data) {
    if (data == null) {
      return null;
    }
    return new byte[] {
      (byte) ((data >> 56) & 0xff),
      (byte) ((data >> 48) & 0xff),
      (byte) ((data >> 40) & 0xff),
      (byte) ((data >> 32) & 0xff),
      (byte) ((data >> 24) & 0xff),
      (byte) ((data >> 16) & 0xff),
      (byte) ((data >> 8) & 0xff),
      (byte) (data & 0xff),
    };
  }

  @Override
  public Long from(byte[] bytes) {
    if (bytes == null || bytes.length != Long.BYTES) {
      return null;
    }
    return (bytes[0] & 0xFFL) << 56
        | (bytes[1] & 0xFFL) << 48
        | (bytes[2] & 0xFFL) << 40
        | (bytes[3] & 0xFFL) << 32
        | (bytes[4] & 0xFFL) << 24
        | (bytes[5] & 0xFFL) << 16
        | (bytes[6] & 0xFFL) << 8
        | (bytes[7] & 0xFFL);
  }
}
