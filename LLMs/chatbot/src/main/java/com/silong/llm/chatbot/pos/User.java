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
 * 用户
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-19 20:50
 */
@Data
@Builder
public class User {
  /** 用户编号 */
  private int id;

  /** 用户名 */
  private String name;

  /** 显示名 */
  private String displayName;

  /** 邮箱地址 */
  private List<String> mails;

  /** 移动电话号码 */
  private List<String> mobiles;

  /** 用户角色 */
  private List<String> roles;

  /** 用户描述 */
  private String desc;
}
