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

import com.silong.foundation.dj.bonecrusher.enu.ClientState;
import com.silong.foundation.dj.bonecrusher.message.Messages.DataBlockMetadata;
import io.netty.buffer.ByteBuf;
import io.netty.util.concurrent.Future;
import java.util.function.BiConsumer;

/**
 * 数据同步客户端接口
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-28 17:54
 */
public interface DataSyncClient extends AutoCloseable {

  /**
   * 获取当前客户端状态
   *
   * @return 状态
   */
  ClientState state();

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
   * @param <R> 响应类型
   * @throws Exception 异常
   */
  <T, R> R sendSync(T req) throws Exception;

  /**
   * 异步发送消息，如果数据量过大，通过Future获取所有结果可能会导致OOM异常
   *
   * @param req 请求
   * @return 异步任务Future
   * @param <T> 请求类型
   * @param <R> 响应类型
   * @throws Exception 异常
   */
  <T, R> Future<R> sendAsync(T req) throws Exception;

  /**
   * 发送请求，异步处理返回数据，处理大数据量请求，会分批返回数据块，所有返回的数据块都会调用consumer
   *
   * @param req 请求
   * @param consumer 数据块处理器，输入参数为数据块，总数据块数量，当前数据块编号
   * @return future
   * @param <T> 请求类型
   * @throws Exception 异常
   */
  <T> Future<Void> sendAsync(T req, BiConsumer<ByteBuf, DataBlockMetadata> consumer)
      throws Exception;
}
