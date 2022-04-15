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
package com.silong.foundation.devastator;

import lombok.Getter;

import java.io.Serializable;

/**
 * 集群节点角色
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-15 22:33
 */
public enum ClusterNodeRole implements Serializable {
  /** 工作节点 */
  WORKER(1),

  /** 客户端节点 */
  CLIENT(2);

  /** 角色值 */
  @Getter final int value;

  /**
   * 构造方法
   *
   * @param value 角色值
   */
  ClusterNodeRole(int value) {
    this.value = value;
  }
}
