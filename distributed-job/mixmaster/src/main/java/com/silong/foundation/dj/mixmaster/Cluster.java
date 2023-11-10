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

import java.util.Collection;

/**
 * 集群
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-10 09:28
 */
public interface Cluster {

  /**
   * 集群名称
   *
   * @return 集群名
   */
  String name();

  /**
   * 获取集群节点列表
   *
   * @return 集群节点列表
   * @param <T> 唯一标识类型
   */
  <T extends Comparable<T>> Collection<ClusterNode<T>> clusterNodes();

  /**
   * 获取本地节点
   *
   * @return 本地节点
   * @param <T> 唯一标识类型
   */
  <T extends Comparable<T>> ClusterNode<T> localNode();
}
