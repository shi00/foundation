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

import com.silong.foundation.devastator.config.DevastatorConfig;
import com.silong.foundation.devastator.config.ScheduledExecutorConfig;

import java.io.Closeable;
import java.io.Serializable;

/**
 * Devastator分布式引擎
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-10 09:01
 */
public interface Devastator extends Serializable, Closeable {

  /**
   * 实例名
   *
   * @return 实例名
   */
  String name();

  /**
   * 获取集群配置
   *
   * @return 配置
   */
  DevastatorConfig config();

  /**
   * 获取集群
   *
   * @return 集群
   */
  Cluster cluster();

  /**
   * 获取分布式调度器
   *
   * @param name 调度器名
   * @return 分布式调度器
   */
  DistributedJobScheduler scheduler(String name);
}
