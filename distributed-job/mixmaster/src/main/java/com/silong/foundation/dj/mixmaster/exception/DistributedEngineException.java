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
package com.silong.foundation.dj.mixmaster.exception;

import java.io.Serial;

/**
 * 分布式引擎异常
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-10 16:36
 */
public class DistributedEngineException extends RuntimeException {

  @Serial private static final long serialVersionUID = 7_418_338_001_099_019_448L;

  /** 构造方法 */
  public DistributedEngineException() {
    super();
  }

  /**
   * 构造方法
   *
   * @param message 异常消息
   */
  public DistributedEngineException(String message) {
    super(message);
  }

  /**
   * 构造方法
   *
   * @param message 异常消息
   * @param cause 异常
   */
  public DistributedEngineException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * 构造方法
   *
   * @param cause 异常
   */
  public DistributedEngineException(Throwable cause) {
    super(cause);
  }
}
