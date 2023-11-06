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

package com.silong.foundation.dj.bonecrusher.vo;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import lombok.*;
import org.jgroups.Address;
import org.jgroups.View;

/**
 * 集群信息
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-06 15:40
 */
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class ClusterInfo {
  private static final AtomicReferenceFieldUpdater<ClusterInfo, Address> LOCAL_ADDRESS_UPDATER =
      AtomicReferenceFieldUpdater.newUpdater(ClusterInfo.class, Address.class, "localAddress");

  private static final AtomicReferenceFieldUpdater<ClusterInfo, String> CLUSTER_NAME_UPDATER =
      AtomicReferenceFieldUpdater.newUpdater(ClusterInfo.class, String.class, "name");

  private static final AtomicReferenceFieldUpdater<ClusterInfo, View> CLUSTER_VIEW_UPDATER =
      AtomicReferenceFieldUpdater.newUpdater(ClusterInfo.class, View.class, "view");

  /** 本地节点地址 */
  private volatile Address localAddress;

  /** 集群名称 */
  private volatile String name;

  /** 当前集群视图 */
  private volatile View view;

  /** 清空 */
  public void clear() {
    CLUSTER_VIEW_UPDATER.set(this, null);
    CLUSTER_NAME_UPDATER.set(this, null);
    LOCAL_ADDRESS_UPDATER.set(this, null);
  }

  /**
   * 原子更新集群名称
   *
   * @param expected 预期的集群名称
   * @param update 更新后的集群名称
   * @return true or false
   */
  public boolean compareAndSetClusterName(String expected, String update) {
    return CLUSTER_NAME_UPDATER.compareAndSet(this, expected, update);
  }

  /**
   * 原子更新集群视图
   *
   * @param expected 预期的集群视图
   * @param update 更新后的集群视图
   * @return true or false
   */
  public boolean compareAndSetClusterView(View expected, View update) {
    return CLUSTER_VIEW_UPDATER.compareAndSet(this, expected, update);
  }

  /**
   * 原子更新节点本地地址
   *
   * @param expected 预期的本地地址
   * @param update 更新后的本地地址
   * @return true or false
   */
  public boolean compareAndSetLocalAddress(Address expected, Address update) {
    return LOCAL_ADDRESS_UPDATER.compareAndSet(this, expected, update);
  }

  public ClusterInfo localAddress(Address localAddress) {
    LOCAL_ADDRESS_UPDATER.set(this, localAddress);
    return this;
  }

  public ClusterInfo clusterName(String name) {
    CLUSTER_NAME_UPDATER.set(this, name);
    return this;
  }

  public ClusterInfo view(View view) {
    CLUSTER_VIEW_UPDATER.set(this, view);
    return this;
  }

  public Address localAddress() {
    return LOCAL_ADDRESS_UPDATER.get(this);
  }

  public String clusterName() {
    return CLUSTER_NAME_UPDATER.get(this);
  }

  public View view() {
    return CLUSTER_VIEW_UPDATER.get(this);
  }
}
