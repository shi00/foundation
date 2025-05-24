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
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.ai.mcp.client.autoconfigure.properties.McpSseClientProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

/**
 * Mcp服务器配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-23 20:33
 */
@Data
@Validated
@ConfigurationProperties(prefix = McpSseClientProperties.CONFIG_PREFIX)
public class McpSseServerProperties {

  /** MCP 服务器附加属性 */
  @Data
  public static class Config {

    /** 客户端忽略ssl认证, 默认：false */
    private boolean enabledInSecureClient;

    /** 代理配置 */
    @Valid @NestedConfigurationProperty private ProxyProperties proxy = new ProxyProperties();
  }

  /** MCP Server服务器配置 */
  @NotNull private final Map<String, Config> configs = new HashMap<>();
}
