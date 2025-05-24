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

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

/**
 * 聊天助手配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-19 20:24
 */
@Data
@Validated
@ConfigurationProperties(prefix = "chatbot")
public class ChatbotProperties {

  /** 代理配置 */
  @NestedConfigurationProperty @Valid private ProxyProperties proxy = new ProxyProperties();

  /** 与大模型交互的客户端日志配置 */
  @NestedConfigurationProperty @Valid
  private LogProperties chatClientLogConfig = new LogProperties();

  /** 与MCP服务器交互的客户端日志配置 */
  @NestedConfigurationProperty @Valid
  private LogProperties mcpClientLogConfig = new LogProperties();

  /** 每个会话保留的聊天记录条数阈值，最小为10 */
  @Min(10)
  @NotNull
  private Integer chatHistoryThresholdPerConversation;

  /** 响应结果读超时，默认：60秒 */
  @NotNull private Duration readTimeout;

  /** 传递一条“系统”类型的消息作为输入。系统消息为对话提供高级指令。例如，您可以使用系统消息指示生成器像某个角色一样行事，或以特定格式提供答案。 */
  @NotBlank private String systemMessage;

  /** 客户端忽略ssl认证，默认：false */
  private boolean enabledInSecureClient;
}
