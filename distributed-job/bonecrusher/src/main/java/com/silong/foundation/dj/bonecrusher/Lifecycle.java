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
 * 生命周期接口
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-07 22:08
 */
public interface Lifecycle {

  /**
   * 初始化
   *
   * @throws Exception 异常
   */
  void initialize() throws Exception;

  /**
   * 启动服务
   *
   * @param block 是否阻塞当前线程，等待服务器关闭
   * @throws Exception 异常
   */
  void start(boolean block) throws Exception;

  /**
   * 关闭服务器
   *
   * @throws Exception 异常
   */
  void shutdown() throws Exception;
}
