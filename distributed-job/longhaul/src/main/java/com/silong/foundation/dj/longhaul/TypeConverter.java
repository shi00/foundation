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

package com.silong.foundation.dj.longhaul;

/**
 * 类型转换，必须提供默认构造方法，否则会导致反序列化异常
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-26 15:26
 * @param <T> 对象类型
 */
public interface TypeConverter<T> {

  /**
   * 序列化
   *
   * @return 二进制结果
   * @throws Exception 异常
   */
  byte[] serialize() throws Exception;

  /**
   * 反序列化
   *
   * @param bytes 二进制序列化数据
   * @return 对象
   * @throws Exception 异常
   */
  T deserialize(byte[] bytes) throws Exception;
}
