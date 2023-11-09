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

import com.silong.foundation.dj.mixmaster.Object2PartitionMapping;
import java.io.Serial;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 对象分区映射器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-05-01 10:41
 */
@Component
@ToString
@EqualsAndHashCode
class DefaultObject2PartitionMapping implements Object2PartitionMapping, Serializable {

  @Serial private static final long serialVersionUID = -4_230_565_279_234_269_084L;

  /** 分区数量 */
  private int partitions;

  /** 标识分区数值是否为2的指数，-1表示非2的指数 */
  @ToString.Exclude private int mask;

  /**
   * 构造方法
   *
   * @param partitions 分区数，此分区数应远大于集群节点数，但是必须小于等于{@code 8192}，大于等于{@code 1}
   */
  public DefaultObject2PartitionMapping(@Value("${mixmaster.partitions}") int partitions) {
    resetPartitions(partitions);
  }

  /**
   * 设置分区数量，其中partitions值必须大于0，小于等于{@code 8192}<br>
   * 推荐分区为2的指数值，提升计算性能
   *
   * @param partitions 分区数
   */
  @Override
  public void resetPartitions(int partitions) {
    if (partitions <= 8192 && partitions >= 1) {
      this.partitions = partitions;
      this.mask = calculateMask(partitions);
      return;
    }
    throw new IllegalArgumentException(
        String.format(
            "partitions must be greater than or equal to %d less than or equal to %d", 1, 8192));
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
}
