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
package com.silong.foundation.dts.scheduler.allocator;

import java.io.Serial;
import java.io.Serializable;

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
public class RendezvousAllocator implements Serializable {

  @Serial private static final long serialVersionUID = 0L;

  /** 默认分区数. */
  public static final int DEFAULT_PARTITION_SIZE = 1024;

  /** 最大分区数 */
  public static final int MAX_PARTITIONS_COUNT = 65000;

  /** 分区数量 */
  private int partitions;

  /** 标识分区数值是否为2的指数，-1表示非2的指数 */
  private int mask = -1;

  /** 默认构造方法 */
  public RendezvousAllocator() {
    this(DEFAULT_PARTITION_SIZE);
  }

  /**
   * 构造方法
   *
   * @param partitions 分区数，此分区数应远大于集群节点数，但是必须小于{@code MAX_PARTITIONS_COUNT}
   */
  public RendezvousAllocator(int partitions) {
    this.partitions = partitions;
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
    if (partitions <= MAX_PARTITIONS_COUNT && partitions > 0) {
      this.partitions = partitions;
      mask = isPow2(partitions);
      return this;
    }
    throw new IllegalArgumentException(
        String.format(
            "partitions must be greater than 0 less than or equal to %d", MAX_PARTITIONS_COUNT));
  }

  /**
   * 计算给定值是否为2的指数值
   *
   * @param val int value.
   * @return -1表示非2的指数值，否则其他值.
   */
  public static int isPow2(int val) {
    return (val & (val - 1)) == 0 ? val - 1 : -1;
  }

  /**
   * 计算key映射到的partitions编号
   *
   * @param key – Key 建对象.
   * @param mask Mask to use in calculation when partitions count is power of 2.
   * @param partitions 分区数.
   * @return 给定key映射到的分区编号.
   */
  public static int calculatePartition(Object key, int mask, int partitions) {
    if (mask >= 0) {
      int h;
      return ((h = key.hashCode()) ^ (h >>> 16)) & mask;
    }
    return abs(key.hashCode() % partitions);
  }

  /**
   * Gets absolute value for integer. If integer is {@link Integer#MIN_VALUE}, then {@code 0} is
   * returned.
   *
   * @param i Integer.
   * @return Absolute value.
   */
  private static int abs(int i) {
    return Math.max(Math.abs(i), 0);
  }
}
