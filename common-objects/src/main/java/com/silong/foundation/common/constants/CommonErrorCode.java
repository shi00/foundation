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
package com.silong.foundation.common.constants;

import com.silong.foundation.common.model.ErrorDetail;
import lombok.Getter;

/**
 * 通用错误信息
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-14 09:16
 */
public enum CommonErrorCode {

  /** 服务内部错误 */
  SERVICE_INTERNAL_ERROR("%s.error.0001", "An internal error occurred in the %s."),

  /** 权限不足 */
  INSUFFICIENT_PERMISSIONS("%s.error.0002", "Not authorized to operate."),

  /** 鉴权失败 */
  AUTHENTICATION_FAILED("%s.error.0003", "Authentication failed.");

  /** 错误码 */
  @Getter private final String code;

  /** 错误提示 */
  @Getter private final String message;

  CommonErrorCode(String code, String message) {
    this.code = code;
    this.message = message;
  }

  /**
   * 根据服务名生成错误详情信息，供返回
   *
   * @param serviceName 服务名
   * @return 错误详情
   */
  public ErrorDetail format(String serviceName) {
    return ErrorDetail.builder()
        .errorCode(String.format(code, serviceName))
        .errorMessage(String.format(message, serviceName))
        .build();
  }
}
