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

import com.silong.foundation.dj.mixmaster.Identity;
import com.silong.foundation.dj.mixmaster.Partition2NodesMapping;
import com.silong.foundation.dj.mixmaster.utils.LambdaSerializable.SerializableBiPredicate;
import com.silong.foundation.dj.mixmaster.vo.ClusterNodeUUID;
import com.silong.foundation.dj.mixmaster.vo.WeightNodeTuple;
import jakarta.annotation.Nullable;
import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import lombok.NonNull;
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

  /** 最高随机权重分区节点映射器 */
  public static final RendezvousPartitionMapping INSTANCE = new RendezvousPartitionMapping();

  /** 备份节点过滤器，第一个参数为Primary节点, 第二个参数为被测试节点. */
  private SerializableBiPredicate<ClusterNodeUUID, ClusterNodeUUID> backupFilter;

  /** 第一个参数为被测试节点，第二个参数为当前partition已经分配的节点列表 (列表中的第一个节点为Primary) */
  private SerializableBiPredicate<ClusterNodeUUID, Collection<ClusterNodeUUID>>
      affinityBackupFilter;

  /** 默认构造方法 */
  private RendezvousPartitionMapping() {}

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

  private WeightNodeTuple[] calculateNodeWeight(
      int partitionNum, Collection<ClusterNodeUUID> clusterNodes) {
    int i = 0;
    WeightNodeTuple[] array = new WeightNodeTuple[clusterNodes.size()];
    for (ClusterNodeUUID node : clusterNodes) {
      array[i++] = new WeightNodeTuple(mixHash(node.uuid().hashCode(), partitionNum), node);
    }
    return array;
  }

  @Override
  public List<ClusterNodeUUID> allocatePartition(
      int partitionNo,
      int backupNum,
      Collection<ClusterNodeUUID> clusterNodes,
      @Nullable Map<ClusterNodeUUID, Collection<ClusterNodeUUID>> neighborhood) {
    if (partitionNo < 0) {
      throw new IllegalArgumentException("partitionNo must be greater than or equal to 0.");
    }

    if (backupNum < 0) {
      throw new IllegalArgumentException("backupNum must be greater than or equal to 0");
    }

    if (clusterNodes == null || clusterNodes.isEmpty()) {
      throw new IllegalArgumentException("clusterNodes must not be null or empty.");
    }

    // 计算集群中真实保存的数据份数，含主
    final int primaryAndBackups =
        backupNum == Integer.MAX_VALUE
            ? clusterNodes.size()
            : Math.min(backupNum + 1, clusterNodes.size());

    // 如果是同步复制到所有节点，则直接返回所有节点
    if (primaryAndBackups == clusterNodes.size()) {
      return new ArrayList<>(clusterNodes);
    }

    // 延迟排序优化
    WeightNodeTuple[] weightNodeTuples = calculateNodeWeight(partitionNo, clusterNodes);
    Iterable<ClusterNodeUUID> sortedNodes =
        new LazyLinearSortedContainer(weightNodeTuples, primaryAndBackups);

    // 先添加主(最高随机权重)
    Iterator<ClusterNodeUUID> it = sortedNodes.iterator();
    ClusterNodeUUID primary = it.next();
    List<ClusterNodeUUID> res = new ArrayList<>(primaryAndBackups);
    res.add(primary);

    // 是否排除邻居节点
    boolean exclNeighbors = neighborhood != null && !neighborhood.isEmpty();
    Collection<Identity<Address>> allNeighbors = exclNeighbors ? new HashSet<>() : null;

    // 选取备份节点
    if (backupNum > 0) {
      while (it.hasNext() && res.size() < primaryAndBackups) {
        ClusterNodeUUID node = it.next();
        if (disableBackupNodeFilter()
            || isPassedBackupNodeFilter(primary, node)
            || isPassedAffinityBackupNodeFilter(node, res)) {
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

    // Need to iterate again in case if there are no nodes which pass exclude neighbors backups
    // criteria.
    if (res.size() < primaryAndBackups
        && clusterNodes.size() >= primaryAndBackups
        && exclNeighbors) {
      // 剔除primary
      it = sortedNodes.iterator();
      it.next();

      while (it.hasNext() && res.size() < primaryAndBackups) {
        ClusterNodeUUID node = it.next();
        if (!res.contains(node)) {
          res.add(node);
        }
      }
      log.warn(
          "RendezvousPartitionMapping excludeNeighbors is ignored because topology has no enough nodes to assign backups.");
    }

    if (res.size() > primaryAndBackups) {
      throw new IllegalStateException(
          "The number of primary and backup nodes must be greater than or equal to the number of mapping nodes.");
    }
    return res;
  }

  private boolean isPassedAffinityBackupNodeFilter(
      ClusterNodeUUID node, List<ClusterNodeUUID> res) {
    return affinityBackupFilter != null && affinityBackupFilter.test(node, res);
  }

  private boolean isPassedBackupNodeFilter(ClusterNodeUUID primary, ClusterNodeUUID node) {
    return backupFilter != null && backupFilter.test(primary, node);
  }

  private boolean disableBackupNodeFilter() {
    return affinityBackupFilter == null && backupFilter == null;
  }

  /** Sorts the initial array with linear sort algorithm array */
  private static class LazyLinearSortedContainer implements Iterable<ClusterNodeUUID> {
    /** Initial node-hash array. */
    private final WeightNodeTuple[] arr;

    /** Count of the sorted elements */
    private int sorted;

    /**
     * @param arr Node / partition hash list.
     * @param needFirstSortedCnt Estimate count of elements to return by iterator.
     */
    LazyLinearSortedContainer(WeightNodeTuple[] arr, int needFirstSortedCnt) {
      this.arr = arr;
      if (needFirstSortedCnt > (int) Math.log(arr.length)) {
        Arrays.sort(arr);
        sorted = arr.length;
      }
    }

    /** {@inheritDoc} */
    @Override
    @NonNull
    public Iterator<ClusterNodeUUID> iterator() {
      return new SortIterator();
    }

    /** */
    private class SortIterator implements Iterator<ClusterNodeUUID> {
      /** Index of the first unsorted element. */
      private int cur;

      /** {@inheritDoc} */
      @Override
      public boolean hasNext() {
        return cur < arr.length;
      }

      /** {@inheritDoc} */
      @Override
      public ClusterNodeUUID next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }

        if (cur < sorted) {
          return arr[cur++].node();
        }

        WeightNodeTuple min = arr[cur];
        int minIdx = cur;
        for (int i = cur + 1; i < arr.length; i++) {
          if (WeightNodeTuple.WeightNodeTupleComparator.COMPARATOR.compare(arr[i], min) < 0) {
            minIdx = i;
            min = arr[i];
          }
        }

        if (minIdx != cur) {
          arr[minIdx] = arr[cur];
          arr[cur] = min;
        }

        sorted = cur++;
        return min.node();
      }

      /** {@inheritDoc} */
      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
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
