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
import com.silong.foundation.utilities.concurrent.McsSpinLock;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
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

  /** 节点到分区映射表，value为true标识primary否则为backup节点 */
  private final RecycleConcurrentMap<Integer, Boolean> node2Partitions;

  /** 节点本地地址 */
  private final DefaultDistributedEngine engine;

  /** MCS自旋锁 */
  private final Lock spinLock = new McsSpinLock();

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

    spinLock.lock();
    try {
      // 滚动map
      partition2Nodes.scroll();
      node2Partitions.scroll();
      IntStream.range(0, partitionCount)
          .parallel()
          .forEach(
              partition -> {
                List<ClusterNodeUUID> mappingNodes =
                    partitionMapping.allocatePartition(partition, backupNum, nodes, null);
                int index = mappingNodes.indexOf(localAddress);
                if (index >= 0) {
                  node2Partitions.put(partition, index == 0 ? Boolean.TRUE : Boolean.FALSE);
                }
                partition2Nodes.put(partition, mappingNodes);
              });
    } finally {
      spinLock.unlock();
    }
  }

  /**
   * 根据分区号查询对应的集群节点列表
   *
   * @param partition 分区号
   * @return 节点列表
   */
  public List<ClusterNodeUUID> getClusterNodes(int partition) {
    spinLock.lock();
    try {
      List<ClusterNodeUUID> nodes = partition2Nodes.get(partition);
      return nodes == null ? List.of() : nodes;
    } finally {
      spinLock.unlock();
    }
  }

  /**
   * 分区是否存储在本地节点
   *
   * @param partition 分区
   * @return true or false
   */
  public boolean isLocalContains(int partition) {
    spinLock.lock();
    try {
      return node2Partitions.containsKey(partition);
    } finally {
      spinLock.unlock();
    }
  }

  /**
   * 本地节点是否为primary分区
   *
   * @param partition 分区
   * @return true or false
   */
  public boolean isLocalPrimary(int partition) {
    spinLock.lock();
    try {
      return node2Partitions.get(partition) == Boolean.TRUE;
    } finally {
      spinLock.unlock();
    }
  }

  @Override
  public void close() {
    partition2Nodes.clear();
    node2Partitions.release();
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
