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

package com.silong.llm.chatbot.daos;

import lombok.Getter;

/**
 * 数据库服务的常量
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-19 20:19
 */
public interface Constants {

  /** 有效 */
  byte VALID = 1;

  /** 无效 */
  byte INVALID = 0;

  /** 用户角色 */
  enum Role {

    /** 管理员 */
    ADMINISTRATOR("ADMINISTRATOR"),

    /** 用户 */
    USER("USER");

    @Getter private final String value;

    Role(String v) {
      value = v;
    }

    /**
     * 枚举类型转换
     *
     * @param v 值
     * @return 枚举类型
     */
    public static Role fromString(String v) {
      for (Role r : Role.values()) {
        if (r.value.equals(v)) {
          return r;
        }
      }
      throw new IllegalArgumentException(v);
    }
  }
}
