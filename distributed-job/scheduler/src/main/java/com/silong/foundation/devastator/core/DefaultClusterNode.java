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
import com.silong.foundation.devastator.protobuf.Devastator.IpAddressInfo;
import com.silong.foundation.devastator.utils.TypeConverter;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.jgroups.Version;
import org.jgroups.util.UUID;

import java.io.Serial;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import static com.silong.foundation.devastator.utils.TypeConverter.STRING_TO_BYTES;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.SystemUtils.getHostName;

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

  /**
   * 构造方法
   *
   * @param clusterNode 节点信息
   */
  public DefaultClusterNode(ClusterNodeUUID clusterNode) {
    if (clusterNode == null) {
      throw new IllegalArgumentException("clusterNode must not be null.");
    }
    this.clusterNode = clusterNode;
  }

  @Override
  public ClusterNodeRole role() {
    return ClusterNodeRole.find(clusterNode.clusterNodeInfo().getRole());
  }

  @Override
  public String version() {
    return Version.print((short) clusterNode.clusterNodeInfo().getVersion());
  }

  @Override
  public String hostName() {
    return clusterNode.clusterNodeInfo().getHostName();
  }

  @Override
  public Collection<String> addresses() {
    return clusterNode.clusterNodeInfo().getAddressesList().stream()
        .map(this::buildIpAddress)
        .toList();
  }

  @SneakyThrows
  private String buildIpAddress(IpAddressInfo ipAddressInfo) {
    return InetAddress.getByAddress(ipAddressInfo.getIpAddress().toByteArray()).getHostAddress();
  }

  @Override
  public boolean isLocal() {
    return StringUtils.equals(hostName(), getHostName());
  }

  @Override
  @NonNull
  public <T extends UUID> T uuid() {
    return (T) clusterNode;
  }

  @Nullable
  @Override
  @SneakyThrows
  public <T> T attribute(String attributeName, TypeConverter<T, byte[]> converter) {
    if (isEmpty(attributeName)) {
      throw new IllegalArgumentException("attributeName must not be null or empty.");
    }
    if (converter == null) {
      throw new IllegalArgumentException("converter must not be null.");
    }
    return converter.from(
        clusterNode.clusterNodeInfo().getAttributesMap().get(attributeName).toByteArray());
  }

  @Nullable
  @Override
  public String attribute(String attributeName) {
    return attribute(attributeName, STRING_TO_BYTES);
  }

  @Override
  public Map<String, byte[]> attributes() {
    return clusterNode.clusterNodeInfo().getAttributesMap().entrySet().stream()
        .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().toByteArray()));
  }
}
