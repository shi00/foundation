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

package com.silong.foundation.springboot.starter.jwt.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-05-07 9:33
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {
  /** 鉴权内部错误 */
  INTERNAL_ERROR("%s.005"),
  /** token中找不到identity */
  IDENTITY_NOT_FOUND("%s.004"),
  /** 请求头中找不到token */
  TOKEN_NOT_FOUND("%s.003"),
  /** 认证失败 */
  UNAUTHENTICATED("%s.002"),
  /** 禁止访问 */
  FORBIDDEN("%s.001");

  /** 错误码格式 */
  private final String format;

  public String format(String prefix) {
    return String.format(format, prefix);
  }
}
