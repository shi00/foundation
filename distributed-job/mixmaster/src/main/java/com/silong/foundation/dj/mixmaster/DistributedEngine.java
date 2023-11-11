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

import com.google.protobuf.MessageLite;
import jakarta.annotation.Nullable;
import org.jgroups.Address;

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
   * @param msg 消息
   * @param offset 偏移量
   * @param length 长度
   * @param dest 目标地址，如果null则标识集群内发送
   * @return this
   * @throws Exception 异常
   */
  DistributedEngine send(byte[] msg, int offset, int length, @Nullable ClusterNode<?> dest)
      throws Exception;

  /**
   * 异步发送集群消息
   *
   * @param msg 消息
   * @param dest 目标地址，如果null则标识集群内发送
   * @return this
   * @throws Exception 异常
   */
  DistributedEngine send(byte[] msg, @Nullable ClusterNode<?> dest) throws Exception;

  /**
   * 异步发送集群消息
   *
   * @param msg 消息
   * @param dest 目标地址，如果null则标识集群内发送
   * @return this
   * @param <T> 消息类型
   * @throws Exception 异常
   */
  <T extends MessageLite> DistributedEngine send(T msg, @Nullable ClusterNode<?> dest)
      throws Exception;

  /**
   * 加入集群后返回集群名，否则null
   *
   * @return 集群名
   */
  String clusterName();

  /**
   * 获取本地节点地址
   *
   * @return 地址
   */
  Address localAddress();

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
