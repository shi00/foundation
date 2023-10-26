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

package com.silong.foundation.dj.bonecrusher.message;

import lombok.Getter;

/**
 * 错误码
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-26 0:03
 */
@Getter
public enum ErrorCode {

  /** 找不到对应的class */
  CLASS_NOT_FOUND(102, "The specified class[%s] cannot be found."),

  /** 登录失败 */
  PERFORM_OPERATIONS_WITHOUT_LOGGING_IN(
      101, "Other operations can only be performed after successful login."),

  /** 登录失败 */
  LOGIN_FAILED(100, "Login failed."),

  /** 加载类成功 */
  LOADING_CLASS_SUCCESSFUL(0, "Loading class[%s] successfully."),

  /** 登录成功 */
  LOGIN_SUCCESSFUL(0, "Login successful.");

  /** 错误码 */
  final int code;

  /** 错误描述 */
  final String desc;

  ErrorCode(int code, String desc) {
    this.code = code;
    this.desc = desc;
  }
}
