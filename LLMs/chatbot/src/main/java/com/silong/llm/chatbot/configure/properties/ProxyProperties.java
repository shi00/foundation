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

package com.silong.llm.chatbot.configure.properties;

import com.silong.llm.chatbot.validator.annotation.NullOrNotBlank;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 代理配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-19 20:27
 */
@Data
public class ProxyProperties {

  /** 是否启用代理 */
  private boolean enabled;

  /** 代理主机 */
  @NullOrNotBlank private String host;

  /** 代理主机端口 */
  @Min(1025)
  @Max(65535)
  private Integer port;

  /** 代理用户 */
  @NullOrNotBlank private String username;

  /** 代理密码 */
  @NullOrNotBlank private String password;

  /** 代理类型 */
  @NullOrNotBlank private String proxyType;
}
