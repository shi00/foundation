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

package com.silong.foundation.dj.bonecrusher;

import java.util.function.Consumer;

/**
 * 数据同步客户端接口
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-28 17:54
 */
public interface DataSyncClient extends AutoCloseable {

  /**
   * 连接服务端
   *
   * @param remoteAddress 服务器地址
   * @param remotePort 服务器端口
   * @throws Exception 异常
   */
  DataSyncClient connect(String remoteAddress, int remotePort) throws Exception;

  /**
   * 发送同步消息
   *
   * @param req 请求
   * @return 响应
   * @param <T> 请求类型
   * @param <R> 响应结果
   * @throws Exception 异常
   */
  <T, R> R sendSync(T req) throws Exception;

  /**
   * 异步发送消息
   *
   * @param req 请求
   * @param resultConsumer 响应处理器
   * @param <T> 请求类型
   * @param <R> 响应类型
   * @throws Exception 异常
   */
  <T, R> void sendAsync(T req, Consumer<R> resultConsumer) throws Exception;
}
