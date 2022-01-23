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
package com.silong.foundation.crypto.rsa;

import lombok.Getter;

/**
 * RSA密钥长度
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2021-12-26 10:57
 */
public enum RsaKeySize {
  /** 不推荐 */
  @Deprecated
  BITS_1024(1024),
  /** 不推荐 */
  @Deprecated
  BITS_192(2048),
  /** 推荐使用 */
  BITS_3072(3072),
  /** 更安全，但是性能会更慢 */
  BITS_4096(4096);

  @Getter private final int bits;

  /**
   * 构造方法
   *
   * @param bits 密钥长度
   */
  RsaKeySize(int bits) {
    this.bits = bits;
  }

  /**
   * 字节数
   *
   * @return 字节数
   */
  public int getBytes() {
    return bits / Byte.SIZE;
  }
}
