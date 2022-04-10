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
package com.silong.foundation.djs.cluster;

import edu.umd.cs.findbugs.annotations.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

/**
 *
 *
 * <pre>
 * 分布式任务调度器使用KV存储方式保存数据，使用KV存储需要解决数据到集群节点的映射问题，同时还要考虑集群节点的动态变化。
 * 为了解决此问题，分布式调度器采用以下数据映射模型：
 *
 *        节点数取模           一致性hash
 * 数据key ------->  partition ------->  cluster-node
 *
 * </pre>
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-09 19:01
 */
public interface ClusterDataAllocator extends Serializable {

  /** 默认分区数. */
  int DEFAULT_PARTITION_SIZE = 512;

  /** 最大分区数 */
  int MAX_PARTITIONS_COUNT = 8192;

  /** 最小分区数 */
  int MIN_PARTITIONS_COUNT = 1;

  /**
   * 数据在集群内的分区数量
   *
   * @return 分区数量
   */
  int partitions();

  /**
   * 数据key映射到分区
   *
   * @param key 数据key
   * @return 分区编号
   */
  int partition(Object key);

  /**
   * 分区到集群节点映射
   *
   * @param partitionNo 分区编号，大于等于0
   * @param backupNum 副本数量(不含主)，副本数大于等于0
   * @param clusterNodes 集群节点列表，节点列表大于等于1
   * @param neighborhood 各节点邻居关系表，如果此参数不为null，则避免把主备数据映射到互为邻居的节点，确保数据可靠性
   * @return 保存分区的节点列表，其中第一个节点为primary partition节点，后续为backup partition
   */
  Collection<ClusterNode> allocatePartition(
      int partitionNo,
      int backupNum,
      Collection<ClusterNode> clusterNodes,
      @Nullable Map<ClusterNode, Collection<ClusterNode>> neighborhood);
}
