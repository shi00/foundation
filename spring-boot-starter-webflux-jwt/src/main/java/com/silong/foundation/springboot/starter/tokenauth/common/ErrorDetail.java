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

package com.silong.foundation.springboot.starter.tokenauth.common;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.lang.NonNull;

/**
 * 错误信息
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-03 11:10
 */
@Data
@Builder
@Accessors(fluent = true)
public class ErrorDetail {

  private static final String JSON_FORMAT = "{\"error_code\": \"%s\",\"error_msg\": \"%s\"}";

  /** 错误码 */
  private String errorCode;

  /** 错误描述 */
  private String errorMessage;

  /**
   * 转换为json 字符串
   *
   * @return json
   */
  @NonNull
  public String toJson() {
    return String.format(JSON_FORMAT, errorCode, errorMessage);
  }
}
