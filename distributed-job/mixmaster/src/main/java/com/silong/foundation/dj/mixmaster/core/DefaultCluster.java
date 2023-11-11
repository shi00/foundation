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
package com.silong.foundation.dj.mixmaster.core;

import com.silong.foundation.dj.mixmaster.Cluster;
import com.silong.foundation.dj.mixmaster.ClusterNode;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import lombok.NonNull;
import org.jgroups.Address;

/**
 * 默认集群
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-25 22:50
 */
class DefaultCluster implements Cluster, Serializable {

  @Serial private static final long serialVersionUID = -601_860_985_854_551_451L;

  /** 分布式引擎 */
  private final DefaultDistributedEngine engine;

  /**
   * 构造方法
   *
   * @param engine 分布式引擎
   */
  public DefaultCluster(@NonNull DefaultDistributedEngine engine) {
    this.engine = engine;
  }

  @Override
  @NonNull
  public String name() {
    return engine.clusterName();
  }

  @Override
  @NonNull
  @SuppressWarnings("unchecked")
  public Collection<DefaultClusterNode> clusterNodes() {
    return engine.getClusterNodes(engine.currentView());
  }

  @Override
  @NonNull
  @SuppressWarnings("unchecked")
  public ClusterNode<Address> localNode() {
    return engine.getLocalNode();
  }
}
