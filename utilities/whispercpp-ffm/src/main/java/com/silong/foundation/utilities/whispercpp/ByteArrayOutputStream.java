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

package com.silong.foundation.utilities.whispercpp;

/**
 * 字节缓存输出流
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-04-26 16:04
 */
public class ByteArrayOutputStream extends java.io.ByteArrayOutputStream {

  /**
   * 构造方法
   *
   * @param size byte array长度
   */
  public ByteArrayOutputStream(int size) {
    super(size);
  }

  /**
   * 返回内部缓存
   *
   * @return 内部缓存
   */
  public byte[] buf() {
    return buf;
  }
}
