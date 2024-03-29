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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.silong.foundation.dj.mixmaster.vo.ClusterNodeUUID;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jgroups.Address;
import org.jgroups.stack.MembershipChangePolicy;
import org.springframework.stereotype.Component;

/**
 * 集群成员变更策略
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-25 23:30
 */
@Component
@Slf4j
class DefaultMembershipChangePolicy implements MembershipChangePolicy, Serializable {

  @Serial private static final long serialVersionUID = 8_463_246_729_470_241_243L;

  /** 节点处理能力级别属性key， */
  public static final String CLUSTER_NODE_PERFORMANCE_RANK_ATTRIBUTE_KEY =
      "cluster.node.performance.rank";

  private double getNodePowerWeight(Address address, String key) {
    ClusterNodeUUID uuid = (ClusterNodeUUID) address;
    String weight = uuid.clusterNodeInfo().getAttributesMap().get(key).toString(UTF_8);
    return weight != null ? Double.parseDouble(weight) : 0;
  }

  @Override
  public List<Address> getNewMembership(
      Collection<Address> currentMembers,
      Collection<Address> joiners,
      Collection<Address> leavers,
      Collection<Address> suspects) {
    if (log.isDebugEnabled()) {
      log.debug(
          "currentMembers: {}, joiners: {}, leavers: {}, suspects: {}",
          currentMembers,
          joiners,
          leavers,
          suspects);
    }

    LinkedList<Address> members =
        currentMembers.isEmpty() ? new LinkedList<>() : new LinkedList<>(currentMembers);
    Address oldCoordinator = members.peekFirst();

    // 删除离开集群的节点
    if (!leavers.isEmpty()) {
      members.removeAll(leavers);
    }

    // 删除疑似无响应节点
    if (!suspects.isEmpty()) {
      members.removeAll(suspects);
    }

    // 追加新加入集群节点
    if (!joiners.isEmpty()) {
      members.addAll(joiners);
    }

    // 如果当前coordinator仍然存在则保证当前coordinator不变，但是后续节点按权重，角色排序
    if (members.getFirst().equals(oldCoordinator)) {
      members.remove(oldCoordinator);
      members.sort(this::compare);
      members.addFirst(oldCoordinator);
    } else {
      // 如果当前coordinator离群，则按权重，角色排序决定新coordinator
      members.sort(this::compare);
    }

    if (log.isDebugEnabled()) {
      log.debug("newMembership: {}", members);
    }
    return members;
  }

  private int compare(Address m1, Address m2) {
    // 选取性能级别最高的节点优先作为coordinator
    int compare =
        Double.compare(
            getNodePowerWeight(m2, CLUSTER_NODE_PERFORMANCE_RANK_ATTRIBUTE_KEY),
            getNodePowerWeight(m1, CLUSTER_NODE_PERFORMANCE_RANK_ATTRIBUTE_KEY));
    if (compare != 0) {
      return compare;
    }

    // 按照uuid排序
    return m1.compareTo(m2);
  }

  @Override
  public List<Address> getNewMembership(Collection<Collection<Address>> subviews) {
    if (log.isDebugEnabled()) {
      log.debug("subviews: {}", subviews);
    }

    // 从各子视图中挑选当前作为coordinator的节点进行排序，选择新coordinator
    Address coordinator =
        subviews.stream()
            .filter(subView -> !subView.isEmpty())
            .map(subView -> subView.iterator().next())
            .min(this::compare)
            .orElseThrow(IllegalStateException::new);

    LinkedList<Address> addresses =
        subviews.stream()
            .filter(subView -> !subView.isEmpty())
            .flatMap(Collection::stream)
            .filter(address -> !address.equals(coordinator))
            .sorted(this::compare)
            .collect(Collectors.toCollection(LinkedList::new));
    addresses.addFirst(coordinator);

    if (log.isDebugEnabled()) {
      log.debug("mergedView: {}", addresses);
    }
    return addresses;
  }
}
