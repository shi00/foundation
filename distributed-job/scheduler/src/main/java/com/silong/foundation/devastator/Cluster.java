/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.silong.foundation.devastator;

import java.io.Serializable;
import java.util.Collection;

/**
 * 集群
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-10 09:28
 */
public interface Cluster extends Serializable {

  /**
   * 集群名称
   *
   * @return 集群名
   */
  String name();

  /**
   * 集群唯一标识
   *
   * @param <T> uuid类型
   * @return uuid
   */
  <T extends Comparable<T>> T uuid();

  /**
   * 集群视图版本
   *
   * @return 视图版本
   */
  long viewVersion();

  /**
   * 获取指定版本集群视图
   *
   * @param version 版本
   * @return 集群视图
   */
  Collection<ClusterNode> view(long version);

  /**
   * 获取本地节点
   *
   * @return 本地节点
   */
  ClusterNode localNode();
}
