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

import static java.util.Collections.reverseOrder;

import com.silong.foundation.dj.mixmaster.Identity;
import com.silong.foundation.dj.mixmaster.Partition2NodesMapping;
import com.silong.foundation.dj.mixmaster.utils.LambdaSerializable.SerializableBiPredicate;
import com.silong.foundation.dj.mixmaster.vo.ClusterNodeUUID;
import jakarta.annotation.Nullable;
import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.jgroups.Address;
import org.springframework.stereotype.Component;

/**
 *
 *
 * <pre>
 *   分布式系统为充分利用集群内节点能力，在保存数据时首先需要解决的问题是确定数据该保存至集群内那个节点(如果有副本，则需要考虑保存至那几个节点)。
 *   在思考数据在集群节点中如何分配时，需要考虑以下几个约束条件：
 *   1. 数据在集群节点内均匀分布
 *   2. 集群节点在增加时，集群当前保存数据会从各集群节点部分迁移至新节点，已达到集群内数据均匀分布，但是集群内老节点间不会迁移数据。
 *   3. 集群节点在删除时，原保存在被删除节点中的数据会向集群中仍然存在的节点进行迁移，已达到集群内数据均匀分布，但是集群内老节点间不会迁移数据。
 *   4. 由于集群节点增删导致的数据迁移负载尽量小
 *   5. 分布式系统存在频繁的数据存储或读取，需要稳定，可靠，高性能且实现相对简单。
 *   为达到以上效果，业界一般采取一致性hash算法解决此问题。
 *   此处使用Rendezvous一致性哈希算法解决此问题，具体实现是把数据先映射至分区，在把分区映射至集群节点，通过分层提升算法性能以及灵活性。
 * </pre>
 *
 * @see <a href="https://randorithms.com/2020/12/26/rendezvous-hashing.html">rendezvous-hashing</a>
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-06 22:29
 */
@Slf4j
@Component
class RendezvousPartitionMapping implements Partition2NodesMapping<ClusterNodeUUID>, Serializable {

  @Serial private static final long serialVersionUID = 8_142_384_185_333_518_222L;

  /** 配套优先级队列，取最高权重TopK个节点 */
  private static final Comparator<ClusterNodeUUID> COMPARATOR =
      reverseOrder(
          Comparator.comparingLong(clusterNodeUUID -> clusterNodeUUID.rendezvousWeight().get()));

  private static final ThreadLocal<PriorityQueue<ClusterNodeUUID>> PRIORITY_QUEUE =
      new ThreadLocal<>();

  private static final ThreadLocal<Collection<Identity<Address>>> NEIGHBORS = new ThreadLocal<>();

  /** 备份节点过滤器，第一个参数为Primary节点, 第二个参数为被测试节点. */
  private SerializableBiPredicate<ClusterNodeUUID, ClusterNodeUUID> backupFilter;

  /** 第一个参数为被测试节点，第二个参数为当前partition已经分配的节点列表 (列表中的第一个节点为Primary) */
  private SerializableBiPredicate<ClusterNodeUUID, Collection<ClusterNodeUUID>>
      affinityBackupFilter;

  /**
   * 设置备份节点过滤器
   *
   * @param backupFilter 过滤器
   * @return @{@code this}
   */
  public RendezvousPartitionMapping setBackupFilter(
      SerializableBiPredicate<ClusterNodeUUID, ClusterNodeUUID> backupFilter) {
    this.backupFilter = backupFilter;
    return this;
  }

  /**
   * 设置亲和性节点过滤器
   *
   * @param affinityBackupFilter 过滤器
   * @return @{@code this}
   */
  public RendezvousPartitionMapping setAffinityBackupFilter(
      SerializableBiPredicate<ClusterNodeUUID, Collection<ClusterNodeUUID>> affinityBackupFilter) {
    this.affinityBackupFilter = affinityBackupFilter;
    return this;
  }

  private <T> T getThreadLocalValue(ThreadLocal<T> threadLocal, Supplier<T> supplier) {
    // 协程数量可能很多，不复用，所以直接创建，不缓存
    if (Thread.currentThread().isVirtual()) {
      return supplier.get();
    } else {
      T result;
      if ((result = threadLocal.get()) == null) {
        threadLocal.set(result = supplier.get());
      }
      return result;
    }
  }

  @Override
  public SequencedCollection<ClusterNodeUUID> allocatePartition(
      int partitionNo,
      int backupNum,
      SequencedCollection<ClusterNodeUUID> clusterNodes,
      @Nullable Map<ClusterNodeUUID, SequencedCollection<ClusterNodeUUID>> neighborhood) {
    if (partitionNo < 0) {
      throw new IllegalArgumentException("partitionNo must be greater than or equal to 0.");
    }

    if (backupNum < 0) {
      throw new IllegalArgumentException("backupNum must be greater than or equal to 0");
    }

    if (clusterNodes == null || clusterNodes.isEmpty()) {
      throw new IllegalArgumentException("clusterNodes must not be null or empty.");
    }

    int nodesSize = clusterNodes.size();
    if (log.isDebugEnabled() && backupNum >= nodesSize) {
      log.debug(
          "partitionNo: {} ---> backupNum({}) greater than or equals to clusterNodes({}).",
          partitionNo,
          backupNum,
          nodesSize);
    }

    // 主备数量大于集群节点数量则按节点数量保存
    int primaryAndBackups =
        backupNum == Integer.MAX_VALUE ? nodesSize : Math.min(backupNum + 1, nodesSize);

    // 是否排除邻居节点
    boolean exclNeighbors = neighborhood != null && !neighborhood.isEmpty();
    Collection<Identity<Address>> allNeighbors =
        exclNeighbors ? getThreadLocalValue(NEIGHBORS, HashSet::new) : null;
    PriorityQueue<ClusterNodeUUID> priorityQueue =
        getThreadLocalValue(PRIORITY_QUEUE, () -> new PriorityQueue<>(nodesSize, COMPARATOR));

    try {
      // 使用优先级队列，解决TopK问题，权重最大的k个元素
      for (ClusterNodeUUID node : clusterNodes) {
        node.rendezvousWeight().set(mixHash(node.uuid().hashCode(), partitionNo));
        priorityQueue.offer(node);
      }

      // 先添加主(最高随机权重)
      ClusterNodeUUID primary = priorityQueue.remove();
      List<ClusterNodeUUID> res = new ArrayList<>(primaryAndBackups);
      res.add(primary);

      // 选取备份节点
      if (backupNum > 0) {
        ClusterNodeUUID node;
        while ((node = priorityQueue.poll()) != null && res.size() < primaryAndBackups) {
          // 启用备份节点过滤器，亲和性过滤器
          if ((backupFilter != null && backupFilter.test(primary, node))
              || (affinityBackupFilter != null && affinityBackupFilter.test(node, res))
              || (affinityBackupFilter == null && backupFilter == null)) {
            // 开启邻居过滤
            if (exclNeighbors) {
              if (!allNeighbors.contains(node)) {
                res.add(node);
                allNeighbors.addAll(neighborhood.get(node));
              }
            } else {
              res.add(node);
            }
          }
        }
      }

      // 如果分配的主备节点数量不足，则可能是集群节点数量不足，或者是使用了节点排他条件过滤掉了可用节点
      if (res.size() < primaryAndBackups) {
        log.error(
            "There are not enough nodes in the cluster to assign to the backup partition. enableBackupFilter:{}, enableAffinityBackupFilter:{}, excludeNeighbor:{}.",
            backupFilter != null,
            affinityBackupFilter != null,
            exclNeighbors);
      }

      return res;
    } finally {
      if (allNeighbors != null) {
        allNeighbors.clear();
      }
      clusterNodes.forEach(node -> node.rendezvousWeight().remove());
      priorityQueue.clear();
    }
  }

  /**
   * 把两个int类型hash值组合成一个long型hash。<br>
   * 基于Wang/Jenkins hash
   *
   * @param val1 Hash1
   * @param val2 Hash2
   * @see <a href="https://gist.github.com/badboy/6267743#64-bit-mix-functions">64 bit mix
   *     functions</a>
   * @return long hash
   */
  static long mixHash(int val1, int val2) {
    long key = (val1 & 0xFFFFFFFFL) | ((val2 & 0xFFFFFFFFL) << 32);
    key = (~key) + (key << 21);
    key ^= (key >>> 24);
    key += (key << 3) + (key << 8);
    key ^= (key >>> 14);
    key += (key << 2) + (key << 4);
    key ^= (key >>> 28);
    key += (key << 31);
    return key;
  }
}
