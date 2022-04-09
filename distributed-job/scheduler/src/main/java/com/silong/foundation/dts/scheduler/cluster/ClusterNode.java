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
package com.silong.foundation.dts.scheduler.cluster;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

/**
 * 集群节点
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-07 21:33
 */
public interface ClusterNode {

  /** 节点角色 */
  enum Role {
    /** 工作节点 */
    WORKER,

    /** 集群管理者，负责集群管理业务 */
    LEADER,

    /** 客户端节点，不负责处理业务，仅连接集群，访问集群数据 */
    CLIENT
  }

  /**
   * 节点角色列表
   *
   * @return 角色列表
   */
  Collection<Role> roles();

  /**
   * 节点版本，同一个集群内可能会存在不同版本的集群节点，可以通过版本号进行兼容性校验
   *
   * @return 版本描述
   */
  @NonNull
  String version();

  /**
   * 获取节点主机名
   *
   * @return 节点主机名
   */
  @NonNull
  String hostName();

  /**
   * 节点地址列表
   *
   * @return 地址列表
   */
  @NonNull
  Collection<String> addresses();

  /**
   * 是否本地节点
   *
   * @return true or false
   */
  boolean isLocalNode();

  /**
   * 获取节点在集群内全局唯一的id
   *
   * @param <T> uuid类型
   * @return id
   */
  @NonNull
  <T extends Comparable<T>> T uuid();

  /**
   * 根据属性名获取属性值
   *
   * @param attributeName 属性名
   * @param <T> 属性值类型
   * @return 属性值
   */
  @Nullable
  <T> T attribute(String attributeName);

  /**
   * 获取节点属性集合
   *
   * @return 属性集合
   */
  @NonNull
  Map<String, Object> attributes();
}
