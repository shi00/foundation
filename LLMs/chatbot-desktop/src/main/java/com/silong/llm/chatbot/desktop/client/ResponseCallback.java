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

package com.silong.llm.chatbot.desktop.client;

import jakarta.annotation.Nullable;

/**
 * 异步响应处理器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-19 20:19
 */
public interface ResponseCallback {

  /**
   * 获取消息
   *
   * @param msgKey key
   * @return 消息
   */
  String getMessage(String msgKey);

  /**
   * 响应消息回调
   *
   * @param responseText 响应消息
   * @param conversationId 会话id
   */
  void callback(@Nullable String responseText, @Nullable String conversationId);
}
