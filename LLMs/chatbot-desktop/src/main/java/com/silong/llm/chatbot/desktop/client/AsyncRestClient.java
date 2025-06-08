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

import java.io.Closeable;
import java.util.concurrent.Future;

/**
 * 异步客户端
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-19 20:19
 */
public interface AsyncRestClient extends Closeable {

  /**
   * 创建客户端
   *
   * @param host 主机地址
   * @param port 端口
   * @param credential 用户凭证
   * @return 客户端
   */
  static AsyncRestClient create(String host, int port, String credential) {
    return new DefaultAsyncRestClient(host, port, credential);
  }

  /**
   * 发送用户输入的问题，与LLM交互
   *
   * @param query 用户输入
   * @param conversationId 会话id
   * @return 异步任务
   */
  Future<?> ask(String query, String conversationId);

  /** 关闭，释放资源 */
  @Override
  void close();

  /**
   * 注册消息回调
   *
   * @param callback 回调
   */
  void register(ResponseCallback callback);
}
