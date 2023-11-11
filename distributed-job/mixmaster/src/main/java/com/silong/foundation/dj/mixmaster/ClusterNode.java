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

import com.silong.foundation.dj.mixmaster.enu.ClusterNodeRole;
import com.silong.foundation.dj.mixmaster.vo.AttributionKey;

/**
 * 集群节点
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-07 21:33
 * @param <T> 唯一标识类型
 */
public interface ClusterNode<T extends Comparable<T>> extends Identity<T> {

  /**
   * 角色
   *
   * @return 角色
   */
  ClusterNodeRole role();

  /**
   * 集群名
   *
   * @return 集群名称
   */
  String clusterName();

  /**
   * 获取节点主机名
   *
   * @return 节点主机名
   */
  String hostName();

  /**
   * 集群节点实例名
   *
   * @return 实例名
   */
  String name();

  /**
   * 获取节点在集群内全局唯一的id
   *
   * @return id
   */
  T uuid();

  /**
   * 获取节点属性
   *
   * @param attributeKey 属性名
   * @return 属性值
   */
  <R> R attribute(AttributionKey<R> attributeKey);
}
