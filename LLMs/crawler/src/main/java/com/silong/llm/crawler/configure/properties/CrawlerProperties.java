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

package com.silong.llm.crawler.configure.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

/**
 * 爬虫配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-22 22:48
 */
@Data
@Validated
@ConfigurationProperties(prefix = "crawler")
public class CrawlerProperties {

  @Data
  public static class Proxy {
    /** 代理主机 */
    private String host;

    /** 代理端口 */
    private Integer port;

    /** 代理账号 */
    private String username;

    /** 代理账户密码 */
    private String password;
  }

  /** 代理配置 */
  @NestedConfigurationProperty private Proxy proxy = new Proxy();
}
