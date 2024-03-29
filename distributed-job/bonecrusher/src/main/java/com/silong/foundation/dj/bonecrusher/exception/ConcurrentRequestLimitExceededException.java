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

package com.silong.foundation.dj.bonecrusher.exception;

import java.io.Serial;

/**
 * 并发请求数超出限制异常
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-01 14:26
 */
public class ConcurrentRequestLimitExceededException extends Exception {
  @Serial private static final long serialVersionUID = 7484126504532421056L;

  /** 构造方法 */
  public ConcurrentRequestLimitExceededException() {}

  /**
   * 构造方法
   *
   * @param message 异常消息
   */
  public ConcurrentRequestLimitExceededException(String message) {
    super(message);
  }
}
