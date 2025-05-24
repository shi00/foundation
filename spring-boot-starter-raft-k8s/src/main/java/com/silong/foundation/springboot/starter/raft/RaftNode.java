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

package com.silong.foundation.springboot.starter.raft;

import jakarta.validation.constraints.NotNull;

/**
 * raft协议接点
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-10-20 10:28
 */
public interface RaftNode {

  /**
   * 服务器已知最新的任期（在服务器首次启动时初始化为0，单调递增）
   *
   * @return 任期
   */
  long currentTerm();

  /**
   * 当前任期内收到选票的 candidateId，如果没有投给任何候选人 则为空
   *
   * @return candidateId or null
   */
  String votedFor();

  /**
   * 集群成员角色
   *
   * @return 角色
   */
  MemberRole role();

  /**
   * 对于每一台服务器，发送到该服务器的下一个日志条目的索引（初始值为领导人最后的日志条目的索引+1）
   *
   * @param nodeId 集群节点Id
   * @return 发送到该服务器的下一个日志条目的索引
   */
  long nextIndex(@NotNull String nodeId);

  /**
   * 对于每一台服务器，已知的已经复制到该服务器的最高日志条目的索引（初始值为0，单调递增）
   *
   * @param nodeId 集群节点Id
   * @return 已知的已经复制到该服务器的最高日志条目的索引
   */
  long matchIndex(@NotNull String nodeId);

  /**
   * 已知已提交的最高的日志条目的索引（初始值为0，单调递增）
   *
   * @return 索引
   */
  long commitIndex();

  /**
   * 已经被应用到状态机的最高的日志条目的索引（初始值为0，单调递增）
   *
   * @return 索引
   */
  long lastApplied();
}
