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
import com.silong.foundation.dj.mixmaster.Object2PartitionMapping;
import com.silong.foundation.dj.mixmaster.Partition2NodesMapping;
import com.silong.foundation.dj.mixmaster.configure.config.MixmasterProperties;
import com.silong.foundation.dj.mixmaster.vo.ClusterNodeUUID;
import com.silong.foundation.dj.mixmaster.vo.Partition;
import com.silong.foundation.dj.mixmaster.vo.StoreNodes;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.IntStream;
import lombok.Data;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Component
class DefaultClusterMetadata implements ClusterMetadata<ClusterNodeUUID> {

  /** 分区节点映射器 */
  @ToString.Exclude private Partition2NodesMapping<ClusterNodeUUID> partition2NodesMapping;

  /** 对象分区映射器 */
  @ToString.Exclude private Object2PartitionMapping objectPartitionMapping;

  /** 同步锁 */
  @ToString.Exclude private final StampedLock lock = new StampedLock();

  /** 事件发送器 */
  @ToString.Exclude private ApplicationEventPublisher eventPublisher;

  /** 分布式引擎 */
  @ToString.Exclude private DefaultDistributedEngine engine;

  /** 备份数量，不含主 */
  private int backupNum;

  /** 分区总数 */
  private int totalPartition;

  /** 分区映射表 */
  private NonBlockingHashMap<Integer, Partition<StoreNodes<ClusterNodeUUID>>> partitionsMap;

  @SneakyThrows
  private <T> T doWithWriteLock(Callable<T> callable) {
    long stamp = lock.writeLock();
    try {
      return callable.call();
    } finally {
      lock.unlockWrite(stamp);
    }
  }

  private void updatePartitions(View view) {
    IntStream.range(0, totalPartition)
        .parallel()
        .forEach(
            partitionNo -> {
              // 获取存储分区
              Partition<StoreNodes<ClusterNodeUUID>> partition =
                  partitionsMap.computeIfAbsent(
                      partitionNo, key -> new Partition<>(partitionNo, VIEW_CHANGED_RECORDS));

              // 如果分区首次创建，并且当前节点不是coordinator，则计算历史视图分布
              if (partition.isEmpty() && !engine.isCoordinator()) {
                View before = engine.clusterView.before(view);
                if (log.isDebugEnabled()) {
                  log.debug("view:{}, beforeView:{}", view, before);
                }
                if (before != null) {
                  partition.record(calculatePartitionDistribution(partitionNo, before));
                } else {
                  throw new IllegalStateException(
                      String.format(
                          "The previous view of View cannot be found. %s, view:%s",
                          engine.clusterView, view));
                }
              }

              StoreNodes<ClusterNodeUUID> historyNodes = partition.currentRecord();

              // 计算分区映射的集群节点列表
              StoreNodes<ClusterNodeUUID> newNodes =
                  calculatePartitionDistribution(partitionNo, view);

              // 记录当前分区对应的存储节点列表
              partition.record(newNodes);

              ClusterNodeUUID local = engine.localAddress();

              // 无历史视图表明本地节点是coordinator
              if (historyNodes == null) {
                // TODO coordinator初始化分区数据
              } else {
                // 如果分区历史布局包含本地节点，但是新分区布局不包含则表明分区已迁走到别的节点
                if (historyNodes.contains(local) && !newNodes.contains(local)) {}
              }
            });
  }

  private StoreNodes<ClusterNodeUUID> calculatePartitionDistribution(
      int partitionNo, View newView) {
    return StoreNodes.<ClusterNodeUUID>builder()
        .version(newView.getViewId().getId())
        .primaryAndBackups(
            partition2NodesMapping.allocatePartition(
                partitionNo, backupNum, toClusterNodes(newView.getMembersRaw()), null))
        .build();
  }

  /**
   * 更新元数据
   *
   * @param newView 新视图
   */
  void update(@NonNull View newView) {
    this.<Void>doWithWriteLock(
        () -> {
          updatePartitions(newView);
          return null;
        });
  }

  @Override
  public StoreNodes<ClusterNodeUUID> mapPartition2Nodes(int partition) {
    if (partition < 0 || partition >= totalPartition) {
      throw new IllegalArgumentException(
          String.format("partition(%d) exceeds boundary[%d, %d).", partition, 0, totalPartition));
    }
    long stamp = lock.tryOptimisticRead();
    StoreNodes<ClusterNodeUUID> nodes = partitionsMap.get(partition).currentRecord();
    if (!lock.validate(stamp)) {
      stamp = lock.readLock();
      try {
        nodes = partitionsMap.get(partition).currentRecord();
      } finally {
        lock.unlockRead(stamp);
      }
    }
    return nodes;
  }

  @Override
  public StoreNodes<ClusterNodeUUID> mapObj2Nodes(Object obj) {
    return mapPartition2Nodes(mapObj2Partition(obj));
  }

  @Override
  public int mapObj2Partition(Object obj) {
    return objectPartitionMapping.partition(obj);
  }

  private List<ClusterNodeUUID> toClusterNodes(Address[] addresses) {
    return Arrays.stream(addresses).map(ClusterNodeUUID.class::cast).toList();
  }

  public void setEngine(DefaultDistributedEngine engine) {
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
