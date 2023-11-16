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

import static com.silong.foundation.dj.mixmaster.configure.config.MixmasterProperties.MAX_PARTITIONS_COUNT;

import java.io.Serial;
import lombok.*;

/**
 * 数据分区对象
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-13 10:41
 * @param <T> 节点类型
 */
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
public class Partition<T> extends MultipleVersionObj<T> {

  @Serial private static final long serialVersionUID = 7_579_895_742_844_967_261L;

  /** 分区编号 */
  private final int partitionNo;

  /**
   * 构造方法
   *
   * @param partitionNo 分区编号
   * @param shiftRecords 移动记录上限
   */
  public Partition(int partitionNo, int shiftRecords) {
    super(shiftRecords);
    if (partitionNo < 0 || partitionNo > MAX_PARTITIONS_COUNT) {
      throw new IllegalArgumentException(
          String.format(
              "partitionNo(%d) must be greater than or equals to 0 and less than or equals to %d.",
              partitionNo, MAX_PARTITIONS_COUNT));
    }
    this.partitionNo = partitionNo;
    clear();
  }
}
