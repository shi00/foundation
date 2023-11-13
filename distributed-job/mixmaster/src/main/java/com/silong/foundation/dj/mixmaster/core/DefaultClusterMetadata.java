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

import static com.silong.foundation.dj.mixmaster.vo.EmptyView.EMPTY_VIEW;

import com.silong.foundation.dj.mixmaster.ClusterMetadata;
import com.silong.foundation.dj.mixmaster.Object2PartitionMapping;
import com.silong.foundation.dj.mixmaster.Partition2NodesMapping;
import com.silong.foundation.dj.mixmaster.configure.config.MixmasterProperties;
import com.silong.foundation.dj.mixmaster.vo.ClusterNodeUUID;
import com.silong.foundation.dj.mixmaster.vo.Partition;
import com.silong.foundation.dj.mixmaster.vo.StoreNodes;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.IntStream;
import lombok.Data;
import lombok.NonNull;
import lombok.ToString;
import org.jctools.maps.NonBlockingHashMap;
import org.jgroups.Address;
import org.jgroups.View;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 集群元数据
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-12 14:33
 */
@Data
@Component
class DefaultClusterMetadata implements ClusterMetadata<ClusterNodeUUID> {

  @ToString.Exclude private final Lock writeLock;

  @ToString.Exclude private final Lock readLock;

  /** 分区节点映射器 */
  @ToString.Exclude private Partition2NodesMapping<ClusterNodeUUID> partition2NodesMapping;

  /** 对象分区映射器 */
  @ToString.Exclude private Object2PartitionMapping objectPartitionMapping;

  /** 备份数量，不含主 */
  private int backupNum;

  /** 分区总数 */
  private int totalPartition;

  /** 分区映射表 */
  private NonBlockingHashMap<Integer, Partition<StoreNodes<ClusterNodeUUID>>> partitionsMap;

  /** 构造方法 */
  public DefaultClusterMetadata() {
    ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    this.writeLock = readWriteLock.writeLock();
    this.readLock = readWriteLock.readLock();
  }

  /**
   * 更新元数据
   *
   * @param oldView 旧的集群视图
   * @param newView 新的集群视图
   */
  @Override
  public void update(@NonNull View oldView, @NonNull View newView) {
    writeLock.lock();
    try {
      // 如果是首次加入集群，则完全重新计算分区映射关系
      if (oldView == EMPTY_VIEW) {
        IntStream.range(0, totalPartition)
            .parallel()
            .forEach(
                partition -> {
                  // 获取存储分区
                  Partition<StoreNodes<ClusterNodeUUID>> part =
                      partitionsMap.computeIfAbsent(
                          partition, key -> new Partition<>(partition, 3));

                  StoreNodes<ClusterNodeUUID> nodes =
                      StoreNodes.<ClusterNodeUUID>builder()
                          .primaryAndBackups(
                              partition2NodesMapping.allocatePartition(
                                  partition,
                                  backupNum,
                                  Arrays.stream(newView.getMembersRaw())
                                      .map(ClusterNodeUUID.class::cast)
                                      .toList(),
                                  null))
                          .build();

                  // 记录当前分区对应的存储节点列表
                  part.record(nodes);
                });
      } else {
        Address[][] diff = View.diff(oldView, newView);
        Address[] join = diff[0];
        Address[] left = diff[1];
      }
    } finally {
      writeLock.unlock();
    }
  }

  @Autowired
  public void initialize(MixmasterProperties properties) {
    this.backupNum = properties.getBackupNum();
    this.totalPartition = properties.getPartitions();
    this.partitionsMap = new NonBlockingHashMap<>(totalPartition);
  }

  @Autowired
  public void setPartition2NodesMapping(
      Partition2NodesMapping<ClusterNodeUUID> partition2NodesMapping) {
    this.partition2NodesMapping = partition2NodesMapping;
  }

  @Autowired
  public void setObjectPartitionMapping(Object2PartitionMapping objectPartitionMapping) {
    this.objectPartitionMapping = objectPartitionMapping;
  }

  @Override
  public StoreNodes<ClusterNodeUUID> mapPartition2Nodes(int partition) {
    if (partition < 0 || partition >= totalPartition) {
      throw new IllegalArgumentException(
          String.format("partition(%d) exceeds boundary[%d, %d).", partition, 0, totalPartition));
    }
    readLock.lock();
    try {
      return partitionsMap.get(partition).currentRecord();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public StoreNodes<ClusterNodeUUID> mapObj2Nodes(Object obj) {
    return mapPartition2Nodes(mapObj2Partition(obj));
  }

  @Override
  public int mapObj2Partition(Object obj) {
    return objectPartitionMapping.partition(obj);
  }
}
