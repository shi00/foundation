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

import com.google.protobuf.ByteString;
import com.silong.foundation.devastator.ClusterNode;
import com.silong.foundation.devastator.model.Devastator.ClusterNodeInfo;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.SneakyThrows;
import org.jgroups.Address;

import java.io.Serial;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * 默认集群节点实现
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-07 22:49
 */
public class DefaultClusterNode implements ClusterNode, Serializable {

  @Serial private static final long serialVersionUID = 0L;

  /** 集群节点信息 */
  private final ClusterNodeUUID clusterNode;

  /** 本地地址 */
  private final Address localAddress;

  /**
   * 构造方法
   *
   * @param clusterNode 节点信息
   * @param localAddress 节点地址
   */
  public DefaultClusterNode(ClusterNodeUUID clusterNode, Address localAddress) {
    if (clusterNode == null) {
      throw new IllegalArgumentException("clusterNode must not be null.");
    }
    if (localAddress == null) {
      throw new IllegalArgumentException("localAddress must not be null.");
    }
    this.clusterNode = clusterNode;
    this.localAddress = localAddress;
  }

  private ClusterNodeInfo getClusterNodeInfo() {
    return clusterNode.clusterNodeInfo();
  }

  @Override
  public ClusterNodeRole role() {
    return ClusterNodeRole.find(getClusterNodeInfo().getRole());
  }

  @Override
  public String version() {
    return getClusterNodeInfo().getVersion();
  }

  @Override
  public String hostName() {
    return getClusterNodeInfo().getHostName();
  }

  @Override
  public Collection<String> addresses() {
    return getClusterNodeInfo().getAddressesList().stream().map(this::buildIpAddress).toList();
  }

  @SneakyThrows
  private String buildIpAddress(ByteString ipAddress) {
    return InetAddress.getByAddress(ipAddress.toByteArray()).getHostAddress();
  }

  @Override
  public boolean isLocal() {
    return localAddress.equals(clusterNode);
  }

  @Override
  @NonNull
  public <T extends Comparable<T>> T uuid() {
    return (T) clusterNode;
  }

  @Nullable
  @Override
  public String attribute(String attributeName) {
    return attributes().get(attributeName);
  }

  @Override
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
    if (obj instanceof DefaultClusterNode) {
      return Objects.equals(clusterNode, ((DefaultClusterNode) obj).clusterNode);
    }
    return false;
  }

  @Override
  public String toString() {
    return clusterNode.toString();
  }
}
