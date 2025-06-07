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

import org.apache.hc.core5.http.ssl.TLS;

/**
 * client连接池配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-31 17:28
 */
public record HttpClientConnectionPoolConfig(
    // 连接超时时间：单位：秒
    int connectTimeout,

    // 连接池内连接最大空闲时长，单位：秒
    int evictIdleTime,

    // 连接池内连接存活的时间，单位：秒
    int timeToLive,

    // 支持的TLS协议版本列表
    TLS[] supportedTLSVersions,

    // 支持的加密算法列表
    String[] supportedCipherSuites,

    // 握手超时时间，单位：秒
    int handshakeTimeout,

    // 表示连接在多长时间内未被使用后，再次使用时需要先验证其有效性，单位：秒
    int validateAfterInactivity,

    // 连接池大小
    int connectionPoolSize) {}
