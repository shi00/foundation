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
package com.silong.foundation.devastator.exception;

import java.io.Serial;

/**
 * 数据访问异常
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-10 16:36
 */
public class DataAccessException extends RuntimeException {

  @Serial private static final long serialVersionUID = 8765046106324470702L;

  /** 构造方法 */
  public DataAccessException() {
    super();
  }

  /**
   * 构造方法
   *
   * @param message 异常消息
   */
  public DataAccessException(String message) {
    super(message);
  }

  /**
   * 构造方法
   *
   * @param message 异常消息
   * @param cause 异常
   */
  public DataAccessException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * 构造方法
   *
   * @param cause 异常
   */
  public DataAccessException(Throwable cause) {
    super(cause);
  }
}
