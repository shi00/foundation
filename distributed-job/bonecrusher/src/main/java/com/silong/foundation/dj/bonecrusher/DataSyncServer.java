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

import com.silong.foundation.dj.bonecrusher.enu.NodeClusterState;
import com.silong.foundation.dj.bonecrusher.enu.ServerState;

/**
 * 数据同步服务器接口
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-28 17:54
 */
public interface DataSyncServer extends Lifecycle {

  /**
   * 注册生命周期监听器
   *
   * @param lifecycleListeners 监听器
   */
  void registerListener(LifecycleListener... lifecycleListeners);

  /**
   * 删除生命周期监听器
   *
   * @param lifecycleListener 监听器
   */
  void removeListener(LifecycleListener lifecycleListener);

  /**
   * 获取服务器状态
   *
   * @return 状态
   */
  ServerState state();

  /**
   * 获取节点集群状态
   *
   * @return 集群状态
   */
  NodeClusterState clusterState();

  /**
   * 新建并返回数据同步服务客户端
   *
   * @return 客户端
   */
  DataSyncClient newClient();
}
