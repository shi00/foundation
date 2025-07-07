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
import java.util.Collection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 角色
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-19 20:50
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User role")
public class Role {
  /** 角色编号 */
  @Schema(description = "Role ID", example = "12324")
  private int id;

  /** 角色名 */
  @Schema(description = "Role name", example = "Administrators")
  private String name;

  /** 归属此角色的用户列表 */
  @Schema(description = "List of user IDs authorized for this role", example = "[1,2,3,4]")
  private Collection<Integer> members;

  /** 角色授权的功能访问路径，相对路径 */
  @Schema(
      description = "List of functions authorized for this role",
      example = "[\"list\\users\",\"list\\groups\"]")
  private Collection<String> authorizedPaths;

  /** 角色描述 */
  @Schema(description = "User role description")
  private String desc;
}
