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

import com.silong.foundation.devastator.ClusterDataAllocator;
import com.silong.foundation.devastator.ClusterNode;
import com.silong.foundation.devastator.utils.SerializableBiPredicate;
import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.extern.slf4j.Slf4j;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.stream.StreamSupport;

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
public class RendezvousAllocator implements ClusterDataAllocator, Serializable {

  @Serial private static final long serialVersionUID = 0L;

  /** 备份节点过滤器，第一个参数为Primary节点, 第二个参数为被测试节点. */
  private SerializableBiPredicate<ClusterNode, ClusterNode> backupFilter;

  /** 第一个参数为被测试节点，第二个参数为当前partition已经分配的节点列表 (列表中的第一个节点为Primary) */
  private SerializableBiPredicate<ClusterNode, Collection<ClusterNode>> affinityBackupFilter;

  /** 分区数量 */
  private int partitions;

  /** 标识分区数值是否为2的指数，-1表示非2的指数 */
  private int mask;

  /** 默认构造方法 */
  public RendezvousAllocator() {
    this(DEFAULT_PARTITION_SIZE);
  }

  /**
   * 构造方法
   *
   * @param partitions 分区数，此分区数应远大于集群节点数，但是必须小于等于{@code
   *     ClusterDataAllocator.MAX_PARTITIONS_COUNT}，大于等于{@code
   *     ClusterDataAllocator.MIN_PARTITIONS_COUNT}
   */
  public RendezvousAllocator(int partitions) {
    setPartitions(partitions);
  }

  /**
   * 获取分区数量。
   *
   * @return 分区数量
   */
  public int getPartitions() {
    return partitions;
  }

  /**
   * 设置分区数量，其中partitions值必须大于0，小于等于{@code MAX_PARTITIONS_COUNT}<br>
   * 推荐分区为2的指数值，提升计算性能
   *
   * @param partitions 分区数
   * @return @{@code this}
   */
  public RendezvousAllocator setPartitions(int partitions) {
    if (partitions <= MAX_PARTITIONS_COUNT && partitions >= MIN_PARTITIONS_COUNT) {
      this.partitions = partitions;
      this.mask = calculateMask(partitions);
      return this;
    }
    throw new IllegalArgumentException(
        String.format(
            "partitions must be greater than or equal to %d less than or equal to %d",
            MIN_PARTITIONS_COUNT, MAX_PARTITIONS_COUNT));
  }

  /**
   * 设置备份节点过滤器
   *
   * @param backupFilter 过滤器
   * @return @{@code this}
   */
  public RendezvousAllocator setBackupFilter(
      SerializableBiPredicate<ClusterNode, ClusterNode> backupFilter) {
    this.backupFilter = backupFilter;
    return this;
  }

  /**
   * 设置亲和性节点过滤器
   *
   * @param affinityBackupFilter 过滤器
   * @return @{@code this}
   */
  public RendezvousAllocator setAffinityBackupFilter(
      SerializableBiPredicate<ClusterNode, Collection<ClusterNode>> affinityBackupFilter) {
    this.affinityBackupFilter = affinityBackupFilter;
    return this;
  }

  @Override
  public int partitions() {
    return partitions;
  }

  @Override
  public int partition(Object key) {
    return calculatePartition(key);
  }

  private static class WeightNodeTupleComparator
      implements Comparator<WeightNodeTuple>, Serializable {
    @Serial private static final long serialVersionUID = 0L;

    public static final WeightNodeTupleComparator COMPARATOR = new WeightNodeTupleComparator();

    /** forbidden */
    private WeightNodeTupleComparator() {}

    /** {@inheritDoc} */
    @Override
    public int compare(WeightNodeTuple o1, WeightNodeTuple o2) {
      return o1.weight < o2.weight
          ? -1
          : o1.weight > o2.weight ? 1 : o1.node.uuid().compareTo(o2.node.uuid());
    }
  }

  private record WeightNodeTuple(long weight, ClusterNode node) implements Comparable<WeightNodeTuple>, Serializable {
    @Serial
    private static final long serialVersionUID = 0L;

    @Override
    public int compareTo(RendezvousAllocator.WeightNodeTuple o) {
      return WeightNodeTupleComparator.COMPARATOR.compare(this, o);
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
  private long mixHash(int val1, int val2) {
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

  private WeightNodeTuple[] calculateNodeWeight(
      int partitionNum, Collection<ClusterNode> clusterNodes) {
    return clusterNodes.stream()
        .map(node -> new WeightNodeTuple(mixHash(node.uuid().hashCode(), partitionNum), node))
        .toArray(WeightNodeTuple[]::new);
  }

  @Override
  public Collection<ClusterNode> allocatePartition(
      int partitionNo,
      int backupNum,
      Collection<ClusterNode> clusterNodes,
      @Nullable Map<ClusterNode, Collection<ClusterNode>> neighborhood) {
    validate(partitionNo, backupNum, clusterNodes);

    WeightNodeTuple[] weightNodeTuples = calculateNodeWeight(partitionNo, clusterNodes);

    // 计算集群中真实保存的数据份数，含主
    final int primaryAndBackups =
        backupNum == Integer.MAX_VALUE
            ? clusterNodes.size()
            : Math.min(backupNum + 1, clusterNodes.size());

    Iterable<ClusterNode> sortedNodes =
        new LazyLinearSortedContainer(weightNodeTuples, primaryAndBackups);

    // 如果是同步复制到所有节点，则直接按照节点权重排序顺序返回
    if (backupNum == Integer.MAX_VALUE) {
      return StreamSupport.stream(sortedNodes.spliterator(), false).toList();
    }

    // 先添加主
    Iterator<ClusterNode> it = sortedNodes.iterator();
    ClusterNode primary = it.next();
    List<ClusterNode> res = new ArrayList<>(primaryAndBackups);
    res.add(primary);

    boolean exclNeighbors;
    Collection<ClusterNode> allNeighbors =
        (exclNeighbors = (neighborhood != null && !neighborhood.isEmpty()))
            ? new HashSet<>()
            : null;

    if (backupNum > 0) {
      while (it.hasNext() && res.size() < primaryAndBackups) {
        ClusterNode node = it.next();
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
        ClusterNode node = it.next();
        if (!res.contains(node)) {
          res.add(node);
        }
      }
      log.warn(
          "ClusterDataAllocator excludeNeighbors is ignored because topology has no enough nodes to assign backups.");
    }

    assert res.size() <= primaryAndBackups;
    return res;
  }

  private void validate(int partitionNo, int backupNum, Collection<ClusterNode> clusterNodes) {
    if (partitionNo < 0 || partitionNo >= partitions) {
      throw new IllegalArgumentException(
          String.format("partitionNo must be greater than or equal to 0 less than %d", partitions));
    }

    if (backupNum < 0) {
      throw new IllegalArgumentException("backupNum must be greater than or equal to 0");
    }

    if (clusterNodes == null || clusterNodes.isEmpty()) {
      throw new IllegalArgumentException("clusterNodes must not be null or empty.");
    }
  }

  private boolean isPassedAffinityBackupNodeFilter(ClusterNode node, List<ClusterNode> res) {
    return affinityBackupFilter != null && affinityBackupFilter.test(node, res);
  }

  private boolean isPassedBackupNodeFilter(ClusterNode primary, ClusterNode node) {
    return backupFilter != null && backupFilter.test(primary, node);
  }

  private boolean disableBackupNodeFilter() {
    return affinityBackupFilter == null && backupFilter == null;
  }

  /** Sorts the initial array with linear sort algorithm array */
  private static class LazyLinearSortedContainer implements Iterable<ClusterNode> {
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
    public Iterator<ClusterNode> iterator() {
      return new LazyLinearSortedContainer.SortIterator();
    }

    /** */
    private class SortIterator implements Iterator<ClusterNode> {
      /** Index of the first unsorted element. */
      private int cur;

      /** {@inheritDoc} */
      @Override
      public boolean hasNext() {
        return cur < arr.length;
      }

      /** {@inheritDoc} */
      @Override
      public ClusterNode next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }

        if (cur < sorted) {
          return arr[cur++].node;
        }

        WeightNodeTuple min = arr[cur];
        int minIdx = cur;
        for (int i = cur + 1; i < arr.length; i++) {
          if (WeightNodeTupleComparator.COMPARATOR.compare(arr[i], min) < 0) {
            minIdx = i;
            min = arr[i];
          }
        }

        if (minIdx != cur) {
          arr[minIdx] = arr[cur];
          arr[cur] = min;
        }

        sorted = cur++;
        return min.node;
      }

      /** {@inheritDoc} */
      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    }
  }

  /**
   * 计算分区是否2的指数值
   *
   * @param partCount 分区数量
   * @return -1表示非2的指数值，否则其他值.
   */
  private int calculateMask(int partCount) {
    return (partCount & (partCount - 1)) == 0 ? partCount - 1 : -1;
  }

  /**
   * 计算key映射到的partitions编号
   *
   * @param key – Key 建对象.
   * @return 给定key映射到的分区编号.
   */
  private int calculatePartition(Object key) {
    if (mask >= 0) {
      int h;
      return ((h = key.hashCode()) ^ (h >>> 16)) & mask;
    }
    return Math.max(Math.abs(key.hashCode() % partitions), 0);
  }
}
