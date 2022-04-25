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
package com.silong.foundation.devastator.core;

import com.silong.foundation.devastator.Cluster;
import com.silong.foundation.devastator.ClusterNode;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;

/**
 * 集群
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-25 22:50
 */
public class DefaultCluster implements Cluster, Serializable {

  @Serial private static final long serialVersionUID = 0L;

  /** 分布式引擎 */
  private final DevastatorEngine engine;

  /**
   * 构造方法
   *
   * @param engine 分布式引擎
   */
  public DefaultCluster(DevastatorEngine engine) {
    if (engine == null) {
      throw new IllegalArgumentException("engine must not be null.");
    }
    this.engine = engine;
  }

  @Override
  public String name() {
    return engine.clusterName();
  }

  @Override
  public Collection<ClusterNode> clusterNodes() {
    return engine.getClusterNodes(engine.currentView());
  }

  @Override
  public ClusterNode localNode() {
    return null;
  }
}
