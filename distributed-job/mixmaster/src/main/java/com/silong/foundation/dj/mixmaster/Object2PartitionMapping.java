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
package com.silong.foundation.dj.mixmaster;

/**
 * 对象到分区映射器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-05-01 10:39
 */
public interface Object2PartitionMapping {

  /**
   * 集群内的分区总数量
   *
   * @return 分区数量
   */
  int partitions();

  /**
   * 重置分区数量
   *
   * @param partitions 分区数
   */
  void resetPartitions(int partitions);

  /**
   * 数据key映射到分区
   *
   * @param key 数据key
   * @return 分区编号
   */
  int partition(Object key);
}
