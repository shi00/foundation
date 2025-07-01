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

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 角色
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-19 20:50
 */
@Data
@Builder
public class Role {
  /** 角色编号 */
  private int id;

  /** 角色名 */
  private String name;

  /** 归属此角色的用户列表 */
  private List<Integer> members;

  /** 授权的功能访问路径，相对路径 */
  private List<String> authorizedPaths;

  /** 用户描述 */
  private String desc;
}
