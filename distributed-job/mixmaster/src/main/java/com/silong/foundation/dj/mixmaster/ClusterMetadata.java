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

package com.silong.foundation.dj.mixmaster;

import org.jgroups.View;

/**
 * 集群元数据
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-12 19:46
 * @param <T> 地址类型
 */
public interface ClusterMetadata<T> {

  /**
   * 更新元数据
   *
   * @param oldView 旧的集群视图
   * @param newView 新的集群视图
   */
  void update(View oldView, View newView);

  /**
   * 查询分区对应的节点列表
   *
   * @param partition 分区编号
   * @return 主备节点列表，第一位为primary
   */
  T[] getPrimaryAndBackupNodes(int partition);
}
