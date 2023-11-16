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
import org.springframework.context.ApplicationEvent;

/**
 * 同步分区数据事件
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-16 9:05
 */
@Getter
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
public class SyncPartitionEvent extends ApplicationEvent {

  @Serial private static final long serialVersionUID = -6_617_291_978_925_234_769L;

  /** 待同步的分区编号 */
  private final int partitionNo;

  /**
   * 构造方法
   *
   * @param primaryPartitionOwner 主分区节点地址
   * @param partitionNo 待同步分区编号
   */
  public SyncPartitionEvent(@NonNull Address primaryPartitionOwner, int partitionNo) {
    super(primaryPartitionOwner);
    if (partitionNo < 0) {
      throw new IllegalArgumentException("partitionNo must be greater than or equals to 0.");
    }
    this.partitionNo = partitionNo;
  }

  /**
   * 获取分区主节点地址
   *
   * @return 地址
   */
  public Address getPrimaryPartitionOwner() {
    return (Address) getSource();
  }
}
