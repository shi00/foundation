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
import lombok.Getter;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * 集群节点
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-07 21:33
 */
public interface ClusterNode extends Serializable {

  /**
   * 集群节点角色
   *
   * @author louis sin
   * @version 1.0.0
   * @since 2022-04-15 22:33
   */
  enum ClusterNodeRole implements Serializable {
    /** 工作节点 */
    WORKER(1),

    /** 客户端节点 */
    CLIENT(2);

    /** 角色值 */
    @Getter final int value;

    /**
     * 构造方法
     *
     * @param value 角色值
     */
    ClusterNodeRole(int value) {
      this.value = value;
    }

    /**
     * 查找角色
     *
     * @param value 角色值
     * @return 角色
     * @throws IllegalArgumentException 未知角色值
     */
    public static ClusterNodeRole find(int value) {
      return Arrays.stream(ClusterNodeRole.values())
          .filter(r -> r.value == value)
          .findAny()
          .orElseThrow(
              () ->
                  new IllegalArgumentException(String.format("Unknown value[%d] of role.", value)));
    }
  }

  /**
   * 节点角色列表
   *
   * @return 角色列表
   */
  ClusterNodeRole role();

  /**
   * 节点版本，同一个集群内可能会存在不同版本的集群节点，可以通过版本号进行兼容性校验
   *
   * @return 版本描述
   */
  String version();

  /**
   * 获取节点主机名
   *
   * @return 节点主机名
   */
  String hostName();

  /**
   * 节点地址列表，
   *
   * @return 节点地址列表
   */
  Collection<String> addresses();

  /**
   * 是否本地节点
   *
   * @return true or false
   */
  boolean isLocal();

  /**
   * 获取节点在集群内全局唯一的id
   *
   * @param <T> uuid类型
   * @return id
   */
  @NonNull
  <T extends Comparable<T>> T uuid();

  /**
   * 获取节点属性
   *
   * @param attributeName 属性名
   * @return 属性值
   */
  @Nullable
  String attribute(String attributeName);

  /**
   * 获取节点属性集合
   *
   * @return 属性集合
   */
  Map<String, String> attributes();
}
