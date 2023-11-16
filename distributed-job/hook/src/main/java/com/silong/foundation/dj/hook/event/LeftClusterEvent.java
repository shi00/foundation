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

package com.silong.foundation.dj.hook.event;

import java.io.Serial;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import org.jgroups.Address;
import org.jgroups.View;

/**
 * 离开集群通知事件
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-28 15:33
 */
@Getter
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
public class LeftClusterEvent extends JoinClusterEvent {

  @Serial private static final long serialVersionUID = -5_577_213_471_548_732_899L;

  /**
   * 构造方法
   *
   * @param view 集群视图
   * @param cluster 集群
   * @param localAddress 本地地址
   */
  public LeftClusterEvent(
      @NonNull View view, @NonNull String cluster, @NonNull Address localAddress) {
    super(view, cluster, localAddress);
  }

  @Override
  public String toString() {
    return String.format(
        "LeftClusterEvent{cluster:%s, localAddress:%s}", cluster(), localAddress());
  }
}
