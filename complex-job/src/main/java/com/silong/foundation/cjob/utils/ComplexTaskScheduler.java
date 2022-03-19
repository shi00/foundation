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
package com.silong.foundation.cjob.utils;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 复杂任务调度器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-03-03 14:44
 */
public final class ComplexTaskScheduler {

  private static final AtomicInteger COUNT = new AtomicInteger(0);

  private static final int THREAD_CORE_SIZE =
      Integer.parseInt(
          System.getProperty(
              "task.scheduler.thread.count",
              String.valueOf(Runtime.getRuntime().availableProcessors())));

  private static final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE;

  static {
    SCHEDULED_EXECUTOR_SERVICE =
        new ScheduledThreadPoolExecutor(
            THREAD_CORE_SIZE,
            r ->
                new Thread(r, ComplexTaskScheduler.class.getSimpleName() + COUNT.incrementAndGet()),
            new ThreadPoolExecutor.AbortPolicy());
  }

  /** 禁止实例化 */
  private ComplexTaskScheduler() {}

  /**
   * 获取执行线程池
   *
   * @return 线程池
   */
  public ScheduledExecutorService getExecutor() {
    return SCHEDULED_EXECUTOR_SERVICE;
  }
}
