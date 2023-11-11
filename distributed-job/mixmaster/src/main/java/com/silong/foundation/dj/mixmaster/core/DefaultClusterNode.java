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

import static com.silong.foundation.dj.mixmaster.core.DefaultAddressGenerator.HOST_NAME;
import static com.silong.foundation.dj.mixmaster.enu.ClusterNodeRole.LEADER;
import static com.silong.foundation.dj.mixmaster.enu.ClusterNodeRole.WORKER;

import com.google.protobuf.ByteString;
import com.silong.foundation.dj.mixmaster.ClusterNode;
import com.silong.foundation.dj.mixmaster.enu.ClusterNodeRole;
import com.silong.foundation.dj.mixmaster.vo.AttributionKey;
import com.silong.foundation.dj.mixmaster.vo.ClusterNodeUUID;
import jakarta.annotation.Nullable;
import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import lombok.NonNull;
import org.jgroups.Address;

/**
 * 默认集群节点实现
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-07 22:49
 */
class DefaultClusterNode implements ClusterNode<Address>, Serializable {

  @Serial private static final long serialVersionUID = 8_229_558_903_854_171_711L;

  /** 集群节点信息 */
  private final ClusterNodeUUID clusterNode;

  /** 本地地址 */
  private final DefaultDistributedEngine engine;

  /**
   * 构造方法
   *
   * @param clusterNode 节点信息
   * @param engine 分布式引擎
   */
  public DefaultClusterNode(
      @NonNull ClusterNodeUUID clusterNode, @NonNull DefaultDistributedEngine engine) {
    this.clusterNode = clusterNode;
    this.engine = engine;
  }

  @Override
  public ClusterNodeRole role() {
    return engine.isCoordinator(clusterNode) ? LEADER : WORKER;
  }

  @Override
  public String clusterName() {
    return engine.clusterName();
  }

  @Override
  public String hostName() {
    return HOST_NAME;
  }

  @Override
  public String name() {
    return engine.name();
  }

  @Override
  public ClusterNodeUUID uuid() {
    return clusterNode;
  }

  public Map<String, ByteString> attributes() {
    return clusterNode.clusterNodeInfo().getAttributesMap();
  }

  @Nullable
  @Override
  public <R> R attribute(@NonNull AttributionKey<R> attributeKey) {
    return attributeKey.getConverter().to(attributes().get(attributeKey.getKey()));
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(clusterNode);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof DefaultClusterNode node) {
      return Objects.equals(clusterNode, node.clusterNode);
    }
    return false;
  }

  @Override
  public String toString() {
    return clusterNode.toString();
  }
}
