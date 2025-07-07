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

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 错误信息
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-03 11:10
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detailed error information")
public class ErrorDetail {

  /** 错误码 */
  @JsonProperty("error_code")
  @Schema(description = "Error code", example = "xxx.1001", requiredMode = REQUIRED)
  private String errorCode;

  /** 错误描述 */
  @JsonProperty("error_message")
  @Schema(
      description = "Error message",
      example = "Invalid input parameters.",
      requiredMode = REQUIRED)
  private String errorMessage;
}
