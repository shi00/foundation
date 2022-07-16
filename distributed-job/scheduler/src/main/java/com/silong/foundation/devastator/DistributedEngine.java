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
package com.silong.foundation.devastator;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Devastator分布式引擎
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-10 09:01
 */
public interface DistributedEngine {

  /**
   * 异步发送集群消息
   *
   * @param msg 消息字节数组
   * @param dest 目标地址
   * @param <T> 地址类型
   * @return this
   * @throws Exception 当前节点为连接集群或已关闭时抛出，参数异常时抛出
   */
  <T extends Comparable<T>> DistributedEngine asyncSend(
      @NonNull byte[] msg, @Nullable ClusterNode<T> dest) throws Exception;

  /**
   * 异步发送集群消息
   *
   * @param msg 消息字节数组
   * @param offset 消息偏移位置
   * @param length 消息长度
   * @param dest 目标地址
   * @return this
   * @param <T> 地址类型
   * @throws Exception 当前节点为连接集群或已关闭时抛出，参数异常时抛出
   */
  <T extends Comparable<T>> DistributedEngine asyncSend(
      @NonNull byte[] msg, int offset, int length, @Nullable ClusterNode<T> dest) throws Exception;

  /**
   * 获取集群
   *
   * @return 集群
   */
  Cluster cluster();

  /**
   * 获取分布式调度器
   *
   * @param name 调度器名
   * @return 分布式调度器
   */
  DistributedJobScheduler scheduler(String name);
}
