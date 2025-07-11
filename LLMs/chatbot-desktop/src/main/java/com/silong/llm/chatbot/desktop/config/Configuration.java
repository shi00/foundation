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

package com.silong.llm.chatbot.desktop.config;

/**
 * 配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-31 16:55
 */
public record Configuration(
    // 图标
    String iconPath,

    // 国际化资源路径
    String i18nPath,

    // 登录窗口尺寸
    Size loginWindowSize,

    // 聊天窗口尺寸
    Size chatWindowSize,

    // restful client配置
    HttpClientConfig httpClientConfig,

    // 登录界面配置
    String chatViewPath,

    // 登录界面配置
    String loginViewPath) {}
