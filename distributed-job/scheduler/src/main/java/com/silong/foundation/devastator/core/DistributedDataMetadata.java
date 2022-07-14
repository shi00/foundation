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

import com.silong.foundation.devastator.model.ClusterNodeUUID;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.IntStream;

/**
 * 分布式数据元数据
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-07-10 16:40
 */
class DistributedDataMetadata implements AutoCloseable, Serializable {

  @Serial private static final long serialVersionUID = 4228081108547447817L;

  /** 分区到节点映射表 */
  private final RecycleConcurrentMap<Integer, List<ClusterNodeUUID>> partition2Nodes;

  /** 节点到分区映射表，value为0标识primary，其他值为队列中的位置 */
  private final RecycleConcurrentMap<Integer, Integer> node2Partitions;

  /** 节点本地地址 */
  private final DefaultDistributedEngine engine;

  /** 同步锁，读锁 */
  private final ReentrantReadWriteLock.ReadLock readLock;

  /** 同步锁，读锁 */
  private final ReentrantReadWriteLock.WriteLock writeLock;

  /**
   * 构造方法
   *
   * @param engine 分布式引擎
   */
  public DistributedDataMetadata(DefaultDistributedEngine engine) {
    this.engine = engine;

    // 此处设置初始容量大于最大容量，配合负载因子为1，避免rehash
    int initialCapacity = engine.config.partitionCount() + 1;
    this.partition2Nodes = new RecycleConcurrentMap<>(initialCapacity, 1.0f);
    this.node2Partitions = new RecycleConcurrentMap<>(initialCapacity, 1.0f);
    ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    this.readLock = readWriteLock.readLock();
    this.writeLock = readWriteLock.writeLock();
  }

  /**
   * 根据当前集群节点，初始化本地分区
   *
   * @param nodes 集群节点列表
   */
  public void computePartition2Nodes(List<ClusterNodeUUID> nodes) {
    if (nodes == null || nodes.isEmpty()) {
      throw new IllegalArgumentException("nodes must not be null or empty.");
    }
    int partitionCount = engine.config.partitionCount();
    int backupNum = engine.config.backupNums();
    RendezvousPartitionMapping partitionMapping = engine.partitionMapping;
    ClusterNodeUUID localAddress = engine.getLocalAddress();

    writeLock.lock();
    try {
      // 滚动map
      partition2Nodes.scroll();
      node2Partitions.scroll();

      // 获取上一次视图对应的分区表
      Map<Integer, List<ClusterNodeUUID>> history = partition2Nodes.history();
      IntStream.range(0, partitionCount)
          .parallel()
          .forEach(
              partition -> {
                List<ClusterNodeUUID> newNodes =
                    partitionMapping.allocatePartition(partition, backupNum, nodes, null);
                List<ClusterNodeUUID> historyNodes = history.get(partition);
                int oldIndex = binarySearch(historyNodes, localAddress);
                int newIndex = binarySearch(newNodes, localAddress);

                boolean oldContains = oldIndex >= 0;
                boolean newContains = newIndex >= 0;
                if (oldContains && newContains) {

                } else if (oldContains) {
                  // 如果本地节点原来是主分区，而新的视图中不在本地节点保存，则同步分区数据至新节点
                  if (oldIndex == 0) {
                    engine.partitionSyncExecutor.execute(
                        () -> {
                          String partitionCf = engine.getPartitionCf(partition);
                          //                      engine.persistStorage.

                        });
                  }

                } else if (newContains) {

                } else {

                }

                // 保存本地节点对应的副本节点列表中的索引
                if (newContains) {
                  node2Partitions.put(partition, newIndex);
                }

                // 保存新的分区表
                partition2Nodes.put(partition, newNodes);
              });
    } finally {
      writeLock.unlock();
    }
  }

  /**
   * 降序二分查找
   *
   * @param nodes 降序节点列表
   * @param targetNode 目标节点
   * @return 节点在列表中的位置，否则返回值<0
   */
  private static int binarySearch(List<ClusterNodeUUID> nodes, ClusterNodeUUID targetNode) {
    int low = 0;
    int high = nodes.size() - 1;
    while (low <= high) {
      // 取中值
      int mid = (low + high) >>> 1;
      ClusterNodeUUID midVal = nodes.get(mid);

      // 目标与中值比较大小
      int cmp = midVal.compareTo(targetNode);

      if (cmp > 0) {
        low = mid + 1;
      } else if (cmp < 0) {
        high = mid - 1;
      } else {
        // key found
        return mid;
      }
    }
    // key not found
    return -(low + 1);
  }

  /**
   * 根据分区号查询对应的集群节点列表
   *
   * @param partition 分区号
   * @return 节点列表
   */
  @NonNull
  public List<ClusterNodeUUID> getClusterNodes(int partition) {
    readLock.lock();
    try {
      return partition2Nodes.getOrDefault(partition, List.of());
    } finally {
      readLock.unlock();
    }
  }

  /**
   * 分区是否存储在本地节点
   *
   * @param partition 分区
   * @return true or false
   */
  public boolean isLocalContains(int partition) {
    readLock.lock();
    try {
      return node2Partitions.containsKey(partition);
    } finally {
      readLock.unlock();
    }
  }

  /**
   * 本地节点是否为primary分区
   *
   * @param partition 分区
   * @return true or false
   */
  public boolean isLocalPrimary(int partition) {
    return isLocalIndexFor(partition, 0);
  }

  /**
   * 本地节点是否包含指定分区且本地节点在副本节点列表中的位置是否为给定值
   *
   * @param partition 分区号
   * @param x 本地节点在副本队列中的位置
   * @return true or false
   */
  public boolean isLocalIndexFor(int partition, int x) {
    readLock.lock();
    try {
      return node2Partitions.containsKey(partition) && node2Partitions.get(partition) == x;
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void close() {
    writeLock.lock();
    try {
      partition2Nodes.clear();
      node2Partitions.release();
    } finally {
      writeLock.unlock();
    }
  }

  private static class RecycleConcurrentMap<K, V> implements Map<K, V> {

    /** 当前正则使用的map */
    private volatile ConcurrentHashMap<K, V> current;

    /** 上一次使用的map */
    private volatile ConcurrentHashMap<K, V> history;

    /**
     * 构造方法
     *
     * @param initialCapacity 初始容量
     * @param loadFactor 负载因子
     */
    public RecycleConcurrentMap(int initialCapacity, float loadFactor) {
      this.current = new ConcurrentHashMap<>(initialCapacity, loadFactor);
      this.history = new ConcurrentHashMap<>(initialCapacity, loadFactor);
    }

    /**
     * 返回历史map数据
     *
     * @return map
     */
    @NonNull
    public Map<K, V> history() {
      return history;
    }

    /** 循环使用map */
    public void scroll() {
      ConcurrentHashMap<K, V> swap = current;
      if (!history.isEmpty()) {
        history.clear();
      }
      current = history;
      history = swap;
    }

    /** 释放资源 */
    public void release() {
      if (current != null) {
        current.clear();
        current = null;
      }
      if (history != null) {
        history.clear();
        history = null;
      }
    }

    @Override
    public int size() {
      return current.size();
    }

    @Override
    public boolean isEmpty() {
      return current.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
      return current.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
      return current.containsValue(value);
    }

    @Override
    public V get(Object key) {
      return current.get(key);
    }

    @Override
    public V put(K key, V value) {
      return current.put(key, value);
    }

    @Override
    public V remove(Object key) {
      return current.remove(key);
    }

    @Override
    public void putAll(@NonNull Map<? extends K, ? extends V> m) {
      current.putAll(m);
    }

    @Override
    public void clear() {
      current.clear();
    }

    @Override
    @NonNull
    public Set<K> keySet() {
      return current.keySet();
    }

    @Override
    @NonNull
    public Collection<V> values() {
      return current.values();
    }

    @Override
    @NonNull
    public Set<Entry<K, V>> entrySet() {
      return current.entrySet();
    }
  }
}
