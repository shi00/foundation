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

import com.silong.foundation.dj.mixmaster.ClusterMetadata;
import com.silong.foundation.dj.mixmaster.Partition2NodesMapping;
import com.silong.foundation.dj.mixmaster.configure.config.MixmasterProperties;
import com.silong.foundation.dj.mixmaster.vo.ClusterNodeUUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.IntStream;
import lombok.Data;
import lombok.NonNull;
import lombok.ToString;
import org.jctools.maps.NonBlockingHashMap;
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

  @ToString.Exclude private Partition2NodesMapping<ClusterNodeUUID> partition2NodesMapping;

  /** 备份数量，不含主 */
  private int backupNum;

  /** 分区总数 */
  private int totalPartition;

  /** 分区映射至集群节点映射表 */
  private NonBlockingHashMap<Integer, ClusterNodeUUID[]> partition2NodesMap;

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
  public void update(@NonNull View oldView, @NonNull View newView) {
    writeLock.lock();
    try {
      IntStream.range(0, totalPartition)
          .parallel()
          .forEach(
              partition ->
                  partition2NodesMap.put(
                      partition,
                      partition2NodesMapping.allocatePartition(
                          partition,
                          backupNum,
                          (ClusterNodeUUID[]) newView.getMembersRaw(),
                          null)));
    } finally {
      writeLock.unlock();
    }
  }

  @Autowired
  public void setPartition2NodesMapping(
      Partition2NodesMapping<ClusterNodeUUID> partition2NodesMapping) {
    this.partition2NodesMapping = partition2NodesMapping;
  }

  @Autowired
  public void initialize(MixmasterProperties properties) {
    this.backupNum = properties.getBackupNum();
    this.totalPartition = properties.getPartitions();
    this.partition2NodesMap = new NonBlockingHashMap<>(totalPartition);
  }

  @Override
  public ClusterNodeUUID[] getPrimaryAndBackupNodes(int partition) {
    if (partition < 0 || partition >= totalPartition) {
      throw new IllegalArgumentException(
          String.format("partition(%d) exceeds boundary[%d, %d).", partition, 0, totalPartition));
    }
    readLock.lock();
    try {
      return partition2NodesMap.get(partition);
    } finally {
      readLock.unlock();
    }
  }
}
