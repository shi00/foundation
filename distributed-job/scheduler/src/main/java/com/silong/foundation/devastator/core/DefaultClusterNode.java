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

import com.silong.foundation.devastator.ClusterNode;
import com.silong.foundation.devastator.config.DevastatorProperties.Version;
import com.silong.foundation.devastator.model.ClusterNodeUUID;
import com.silong.foundation.devastator.model.Devastator.ClusterNodeInfo;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.jgroups.Address;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import static com.silong.foundation.devastator.ClusterNode.ClusterNodeRole.LEADER;
import static com.silong.foundation.devastator.ClusterNode.ClusterNodeRole.WORKER;

/**
 * 默认集群节点实现
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-07 22:49
 */
class DefaultClusterNode implements ClusterNode<Address>, Serializable {

  @Serial private static final long serialVersionUID = 4756251708772289039L;

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
  public DefaultClusterNode(ClusterNodeUUID clusterNode, DefaultDistributedEngine engine) {
    if (clusterNode == null) {
      throw new IllegalArgumentException("clusterNode must not be null.");
    }
    if (engine == null) {
      throw new IllegalArgumentException("engine must not be null.");
    }
    this.clusterNode = clusterNode;
    this.engine = engine;
  }

  private ClusterNodeInfo getClusterNodeInfo() {
    return clusterNode.clusterNodeInfo();
  }

  @Override
  @NonNull
  public ClusterNodeRole role() {
    return engine.isCoordinator(clusterNode) ? LEADER : WORKER;
  }

  @Override
  @NonNull
  public String version() {
    return Version.parse((short) getClusterNodeInfo().getDevastatorVersion()).toString();
  }

  @Override
  @NonNull
  public String hostName() {
    return getClusterNodeInfo().getHostName();
  }

  @Override
  @NonNull
  public Collection<String> addresses() {
    return getClusterNodeInfo().getIpAddressesList().stream().toList();
  }

  @Override
  public boolean isLocal() {
    return engine.getLocalAddress().equals(clusterNode);
  }

  @Override
  @NonNull
  public ClusterNodeUUID uuid() {
    return clusterNode;
  }

  @Nullable
  @Override
  public String attribute(String attributeName) {
    return attributes().get(attributeName);
  }

  @Override
  @NonNull
  public Map<String, String> attributes() {
    return getClusterNodeInfo().getAttributesMap();
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
