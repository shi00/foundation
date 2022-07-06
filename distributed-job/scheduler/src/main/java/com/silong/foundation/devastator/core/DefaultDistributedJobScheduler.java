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

import com.silong.foundation.devastator.DistributedJobScheduler;
import com.silong.foundation.devastator.utils.LambdaSerializable.SerializableRunnable;

import java.util.concurrent.ScheduledExecutorService;

/**
 * 分布式任务调度器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-07-02 22:31
 */
class DefaultDistributedJobScheduler implements DistributedJobScheduler, AutoCloseable {

  /** 调度器 */
  private ScheduledExecutorService executorService;

  /** 引擎 */
  private DefaultDistributedEngine engine;

  /**
   * 构造方法
   *
   * @param engine 分布式引擎
   * @param executorService 调度器
   */
  public DefaultDistributedJobScheduler(
      DefaultDistributedEngine engine, ScheduledExecutorService executorService) {
    if (executorService == null) {
      throw new IllegalArgumentException("executorService must not be null.");
    }
    if (engine == null) {
      throw new IllegalArgumentException("engine must not be null.");
    }
    this.engine = engine;
    this.executorService = executorService;
  }

  @Override
  public void close() {
    if (executorService != null) {
      this.executorService.shutdown();
      this.executorService = null;
    }
    this.engine = null;
  }

  @Override
  public void execute(Runnable command) {
    if (command == null) {
      throw new IllegalArgumentException("command must not be null.");
    }
    SerializableRunnable runnable = command::run;
    executorService.execute(runnable);
  }
}
