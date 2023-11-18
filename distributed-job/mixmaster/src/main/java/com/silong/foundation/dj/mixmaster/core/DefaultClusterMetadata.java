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

import static com.silong.foundation.dj.mixmaster.core.DefaultDistributedEngine.VIEW_CHANGED_RECORDS;

import com.silong.foundation.dj.hook.event.SyncPartitionEvent;
import com.silong.foundation.dj.mixmaster.ClusterMetadata;
import com.silong.foundation.dj.mixmaster.DistributedEngine;
import com.silong.foundation.dj.mixmaster.Object2PartitionMapping;
import com.silong.foundation.dj.mixmaster.Partition2NodesMapping;
import com.silong.foundation.dj.mixmaster.configure.config.MixmasterProperties;
import com.silong.foundation.dj.mixmaster.vo.ClusterNodeUUID;
import com.silong.foundation.dj.mixmaster.vo.Partition;
import com.silong.foundation.dj.mixmaster.vo.StoreNodes;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import lombok.Data;
import lombok.NonNull;
import lombok.ToString;
import org.jctools.maps.NonBlockingHashMap;
import org.jgroups.Address;
import org.jgroups.View;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
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

  /** 事件发送器 */
  private ApplicationEventPublisher eventPublisher;

  /** 分布式引擎 */
  private DistributedEngine engine;

  /** 构造方法 */
  public DefaultClusterMetadata() {
    ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    this.writeLock = readWriteLock.writeLock();
    this.readLock = readWriteLock.readLock();
  }

  private List<ClusterNodeUUID> toClusterNodes(Address[] addresses) {
    return Arrays.stream(addresses).map(ClusterNodeUUID.class::cast).toList();
  }

  /**
   * 初始化元素据
   *
   * @param view 初始视图
   */
  void initialize(@NonNull View view) {
    writeLock.lock();
    try {
      IntStream.range(0, totalPartition)
          .parallel()
          .mapToObj(
              partitionNo ->
                  partitionsMap.computeIfAbsent(
                      partitionNo, key -> new Partition<>(partitionNo, VIEW_CHANGED_RECORDS)))
          .forEach(
              partition -> {
                // 计算分区映射的集群节点列表
                // 记录当前分区对应的存储节点列表
                partition.record(
                    StoreNodes.<ClusterNodeUUID>builder()
                        .version(view.getViewId().getId())
                        .primaryAndBackups(
                            partition2NodesMapping.allocatePartition(
                                partition.getPartitionNo(),
                                backupNum,
                                toClusterNodes(view.getMembersRaw()),
                                null))
                        .build());
              });
    } finally {
      writeLock.unlock();
    }
  }

  /**
   * 更新元数据
   *
   * @param oldView 旧视图
   * @param newView 新视图
   */
  void update(@Nullable View oldView, @NonNull View newView) {
    writeLock.lock();
    try {
      IntStream.range(0, totalPartition)
          .parallel()
          .forEach(
              partitionNo -> {
                // 获取存储分区
                Partition<StoreNodes<ClusterNodeUUID>> partition =
                    partitionsMap.computeIfAbsent(
                        partitionNo, key -> new Partition<>(partitionNo, VIEW_CHANGED_RECORDS));

                // 计算分区映射的集群节点列表
                StoreNodes<ClusterNodeUUID> newNodes =
                    StoreNodes.<ClusterNodeUUID>builder()
                        .version(newView.getViewId().getId())
                        .primaryAndBackups(
                            partition2NodesMapping.allocatePartition(
                                partitionNo,
                                backupNum,
                                toClusterNodes(newView.getMembersRaw()),
                                null))
                        .build();

                // 获取分区当前的节点分布记录
                StoreNodes<ClusterNodeUUID> historyNodes = partition.currentRecord();

                // 记录当前分区对应的存储节点列表
                partition.record(newNodes);

                ClusterNodeUUID local = (ClusterNodeUUID) engine.localAddress();

                // 无历史视图表明本地节点是新加入集群
                if (historyNodes == null) {
                  // 新加入集群如果做为备分区，则向主分区请求同步数据
                  if (newNodes.isBackup(local)) {
                    Thread.ofVirtual()
                        .start(
                            () ->
                                eventPublisher.publishEvent(
                                    new SyncPartitionEvent(newNodes.primary(), partitionNo)));
                  } else if (newNodes.isPrimary(local)) {

                  }
                }
                // 如果旧的分区分布不包含本地节点，新分区分布包含本地节点
                else if (!historyNodes.contains(local)) {

                  if (newNodes.contains(local)) {

                  } else {

                  }
                }
              });
    } finally {
      writeLock.unlock();
    }
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

  public void setEngine(DistributedEngine engine) {
    this.engine = engine;
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
  public void setEventPublisher(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  @Autowired
  public void setObjectPartitionMapping(Object2PartitionMapping objectPartitionMapping) {
    this.objectPartitionMapping = objectPartitionMapping;
  }
}
