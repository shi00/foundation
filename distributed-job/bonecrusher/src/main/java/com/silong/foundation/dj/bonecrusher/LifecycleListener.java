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
 * Bonecrusher服务器生命周期监听器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-07 22:08
 */
public interface LifecycleListener {

  /**
   * 创建完毕，new状态
   *
   * @param syncServer 服务器
   */
  void createCompletion(DataSyncServer syncServer);

  /**
   * shutdown完毕后回调，shutdown状态
   *
   * @param syncServer 服务器
   */
  void shutdownCompletion(DataSyncServer syncServer);

  /**
   * initialize完毕后回调，initialized状态
   *
   * @param syncServer 服务器
   */
  void initializeCompletion(DataSyncServer syncServer);

  /**
   * 启动完毕，running状态后回调
   *
   * @param syncServer 服务器
   */
  void startCompletion(DataSyncServer syncServer);

  /**
   * 发生异常，abnormal状态后回调
   *
   * @param syncServer 服务器
   */
  void occurException(DataSyncServer syncServer, Exception e);
}
