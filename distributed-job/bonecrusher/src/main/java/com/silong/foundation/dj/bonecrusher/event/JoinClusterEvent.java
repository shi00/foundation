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

package com.silong.foundation.dj.bonecrusher.event;

import java.io.Serial;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import org.jgroups.Address;
import org.springframework.context.ApplicationEvent;

/**
 * 加入集群通知事件
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-28 15:33
 */
@Getter
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
public class JoinClusterEvent extends ApplicationEvent {

  @Serial private static final long serialVersionUID = 4_285_425_499_689_811_118L;

  /** 本地节点地址 */
  private final Address localAddress;

  /** 集群名 */
  private final String cluster;

  /**
   * 构造方法
   *
   * @param source 事件源
   * @param cluster 集群
   * @param localAddress 本地地址
   */
  public JoinClusterEvent(
      @NonNull Object source, @NonNull String cluster, @NonNull Address localAddress) {
    super(source);
    this.localAddress = localAddress;
    this.cluster = cluster;
  }

  /**
   * 旧视图
   *
   * @return 旧视图
   */
  public String cluster() {
    return cluster;
  }

  @Override
  public String toString() {
    return String.format("JoinClusterEvent{cluster:%s, localAddress:%s}", cluster, localAddress);
  }
}
