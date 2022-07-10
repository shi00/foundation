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

import edu.umd.cs.findbugs.annotations.NonNull;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分布式数据元数据
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-07-10 16:40
 */
class DistributedDataMetadata implements AutoCloseable, Serializable {

  /** 分区到节点映射表 */
  private final Map<Integer, List<DefaultClusterNode>> partition2Nodes;

  /** 节点到分区映射表 */
  private final Map<DefaultClusterNode, List<Integer>> node2Partitions;

  /** 分布式引擎 */
  private final DefaultDistributedEngine engine;

  /**
   * 构造方法
   *
   * @param engine 分布式引擎
   */
  public DistributedDataMetadata(@NonNull DefaultDistributedEngine engine) {
    this.engine = engine;
    // 此处设置初始容量大于最大容量，配合负载因子为1，避免rehash
    this.partition2Nodes = new HashMap<>(engine.config.partitionCount() + 1, 1.0f);
    this.node2Partitions = new HashMap<>();
  }





  @Override
  public void close() {
    partition2Nodes.clear();
    node2Partitions.clear();
  }
}
