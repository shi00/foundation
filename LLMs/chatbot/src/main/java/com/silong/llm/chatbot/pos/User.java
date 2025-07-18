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

package com.silong.llm.chatbot.pos;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-19 20:50
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User")
public class User {
  /** 用户编号 */
  @Schema(description = "User ID", example = "123123")
  private int id;

  /** 用户名 */
  @Schema(description = "User name", example = "messi")
  private String name;

  /** 显示名 */
  @Schema(description = "User nick name", example = "Leo")
  private String displayName;

  /** 邮箱地址 */
  @Schema(description = "User email", example = "[\"messi@gmail.com\"]]")
  private List<String> mails;

  /** 移动电话号码 */
  @Schema(description = "User phone number", example = "[\"+86 13511091123\"]]")
  private List<String> mobiles;

  /** 用户角色 */
  @Schema(
      description = "List of roles assigned to the user",
      example = "[\"Administrators\",\"Users\"]]")
  private List<String> roles;

  /** 用户描述 */
  @Schema(description = "User description", example = "GOD of football")
  private String desc;
}
