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

import com.silong.foundation.devastator.ObjectPartitionMapping;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import static com.silong.foundation.devastator.config.DevastatorConfig.*;

/**
 * 对象分区映射器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-05-01 10:41
 */
@ToString
@EqualsAndHashCode
public class DefaultObjectPartitionMapping implements ObjectPartitionMapping {

  /** 分区数量 */
  private int partitions;

  /** 标识分区数值是否为2的指数，-1表示非2的指数 */
  private int mask;

  /** 默认构造方法 */
  public DefaultObjectPartitionMapping() {
    this(DEFAULT_PARTITION_SIZE);
  }

  /**
   * 构造方法
   *
   * @param partitions 分区数，此分区数应远大于集群节点数，但是必须小于等于{@code
   *     ClusterDataAllocator.MAX_PARTITIONS_COUNT}，大于等于{@code
   *     ClusterDataAllocator.MIN_PARTITIONS_COUNT}
   */
  public DefaultObjectPartitionMapping(int partitions) {
    partitions(partitions);
  }

  /**
   * 设置分区数量，其中partitions值必须大于0，小于等于{@code MAX_PARTITIONS_COUNT}<br>
   * 推荐分区为2的指数值，提升计算性能
   *
   * @param partitions 分区数
   */
  public void partitions(int partitions) {
    if (partitions <= MAX_PARTITIONS_COUNT && partitions >= MIN_PARTITIONS_COUNT) {
      this.partitions = partitions;
      this.mask = calculateMask(partitions);
      return;
    }
    throw new IllegalArgumentException(
        String.format(
            "partitions must be greater than or equal to %d less than or equal to %d",
            MIN_PARTITIONS_COUNT, MAX_PARTITIONS_COUNT));
  }

  /**
   * 计算key映射到的partitions编号
   *
   * @param key – Key 建对象.
   * @return 给定key映射到的分区编号.
   */
  @Override
  public int partition(Object key) {
    if (key == null) {
      throw new IllegalArgumentException("key must not be null.");
    }
    if (mask >= 0) {
      int h;
      return ((h = key.hashCode()) ^ (h >>> 16)) & mask;
    }
    return Math.max(Math.abs(key.hashCode() % partitions), 0);
  }

  /**
   * 计算分区是否2的指数值
   *
   * @param partCount 分区数量
   * @return -1表示非2的指数值，否则其他值.
   */
  public static int calculateMask(int partCount) {
    if (partCount <= 0) {
      throw new IllegalArgumentException("partCount must be greater than 0.");
    }
    return (partCount & (partCount - 1)) == 0 ? partCount - 1 : -1;
  }

  /**
   * 获取分区数量。
   *
   * @return 分区数量
   */
  @Override
  public int partitions() {
    return partitions;
  }

  @Override
  public void close() {}
}
