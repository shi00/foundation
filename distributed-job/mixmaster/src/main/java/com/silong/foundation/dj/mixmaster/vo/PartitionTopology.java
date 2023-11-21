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

package com.silong.foundation.dj.mixmaster.vo;

import jakarta.annotation.Nullable;
import java.io.Serial;
import java.io.Serializable;
import java.util.Iterator;
import java.util.SequencedCollection;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

/**
 * 数据分区在集群节点上的拓扑图，拓扑图中第一位为primary
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-13 15:23
 * @param <T> 节点类型
 */
@Data
@Builder
public class PartitionTopology<T>
    implements Iterable<T>, Comparable<PartitionTopology<T>>, Serializable {

  @Serial private static final long serialVersionUID = -9_040_144_143_663_603_204L;

  /** 分区存储的节点列表 */
  @NonNull private SequencedCollection<T> primaryAndBackups;

  /** 逻辑时钟，版本号，单调递增 */
  private long version;

  /**
   * 分区布局是否包含给定节点
   *
   * @param node 节点
   * @return true or false
   */
  public boolean contains(@NonNull T node) {
    return primaryAndBackups.contains(node);
  }

  /**
   * 给定节点是否为分区主节点
   *
   * @param node 节点
   * @return true or false
   */
  public boolean isPrimary(@NonNull T node) {
    return node.equals(primary());
  }

  /**
   * 给定节点是否为分区备份节点
   *
   * @param node 节点
   * @return true or false
   */
  public boolean isBackup(@NonNull T node) {
    // 过滤分区主节点后查找
    return primaryAndBackups.stream().skip(1).anyMatch(node::equals);
  }

  /**
   * 返回分区对应的主节点
   *
   * @return 主节点
   */
  @Nullable
  public T primary() {
    return primaryAndBackups.isEmpty() ? null : primaryAndBackups.getFirst();
  }

  @Override
  @NonNull
  public Iterator<T> iterator() {
    return primaryAndBackups.iterator();
  }

  @Override
  public int compareTo(@NonNull PartitionTopology<T> nodes) {
    return Long.compare(nodes.version, version);
  }
}
