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

import org.jgroups.Address;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

/**
 * 分布式数据元数据
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-07-10 16:40
 */
class DistributedDataMetadata implements AutoCloseable, Serializable {

  @Serial private static final long serialVersionUID = 8690858620029609276L;

  /** 分区到节点映射表 */
  private final Map<Integer, List<DefaultClusterNode>> partition2Nodes;

  /** 节点到分区映射表 */
  private final Set<Integer> node2Partitions;

  /** 节点本地地址 */
  private final DefaultDistributedEngine engine;

  /**
   * 构造方法
   *
   * @param engine 分布式引擎
   */
  public DistributedDataMetadata(DefaultDistributedEngine engine) {
    // 此处设置初始容量大于最大容量，配合负载因子为1，避免rehash
    this.engine = engine;
    this.partition2Nodes = new ConcurrentHashMap<>(engine.config.partitionCount() + 1, 1.0f);
    this.node2Partitions = ConcurrentHashMap.newKeySet();
  }

  /**
   * 根据当前集群节点，初始化本地分区
   *
   * @param nodes 集群节点列表
   */
  public synchronized void initialize(List<DefaultClusterNode> nodes) {
    // 并行计算分区节点映射关系
    computePartition2Nodes(nodes);

    // 提取本地节点包含的分区列表
    computeNodePartitions();
  }

  private void computeNodePartitions() {
    Address local = engine.getLocalAddress();
    partition2Nodes.entrySet().parallelStream()
        .forEach(
            entry -> {
              Integer partition = entry.getKey();
              List<DefaultClusterNode> nodeList = entry.getValue();
              if (nodeList.stream().anyMatch(node -> node.uuid().equals(local))) {
                node2Partitions.add(partition);
              }
            });
  }

  private void computePartition2Nodes(List<DefaultClusterNode> nodes) {
    int partitionCount = engine.config.partitionCount();
    int backupNum = engine.config.backupNums();
    RendezvousPartitionMapping partitionMapping = engine.partitionMapping;
    IntStream.range(0, partitionCount)
        .parallel()
        .forEach(
            partition ->
                partition2Nodes.put(
                    partition,
                    partitionMapping.allocatePartition(partition, backupNum, nodes, null)));
  }

  /**
   * 根据分区号查询对应的集群节点列表
   *
   * @param partition 分区号
   * @return 节点列表
   */
  public synchronized List<DefaultClusterNode> getClusterNodes(int partition) {
    List<DefaultClusterNode> nodes = partition2Nodes.get(partition);
    return nodes == null ? List.of() : nodes;
  }

  @Override
  public void close() {
    partition2Nodes.clear();
    node2Partitions.clear();
  }
}
