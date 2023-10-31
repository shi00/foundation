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

/**
 * 数据同步服务器接口
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-28 17:54
 */
public interface DataSyncServer {

  /**
   * 启动服务
   *
   * @throws Exception 异常
   */
  void start() throws Exception;

  /**
   * 关闭服务器
   *
   * @throws Exception 异常
   */
  void shutdown() throws Exception;

  /**
   * 数据同步服务客户端
   *
   * @return 客户端
   */
  DataSyncClient client();
}