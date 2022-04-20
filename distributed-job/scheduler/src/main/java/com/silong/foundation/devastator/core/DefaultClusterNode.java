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
import com.silong.foundation.devastator.ClusterNodeRole;
import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;
import org.jgroups.Version;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

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
  private final ClusterNodeUUID clusterNodeUuid;

  /**
   * 构造方法
   *
   * @param clusterNodeUuid 节点信息
   */
  public DefaultClusterNode(@NonNull ClusterNodeUUID clusterNodeUuid) {
    this.clusterNodeUuid = clusterNodeUuid;
  }

  @Override
  public ClusterNodeRole role() {
    return ClusterNodeRole.find(clusterNodeUuid.clusterNodeInfo().getRole());
  }

  @Override
  public String version() {
    return Version.print((short) clusterNodeUuid.clusterNodeInfo().getVersion());
  }

  @Override
  public String hostName() {
    return clusterNodeUuid.clusterNodeInfo().getHostName();
  }

  @Override
  public String address() {
    return null;
  }

  @Override
  public boolean isLocal() {
    return false;
  }

  @Override
  public ClusterNodeUUID uuid() {
    return clusterNodeUuid;
  }

  @Nullable
  @Override
  public <T> T attribute(String attributeName) {
    return (T) clusterNodeUuid.clusterNodeInfo().getAttributesMap().get(attributeName);
  }

  @Override
  public Map<String, Object> attributes() {
    return null;
  }
}
