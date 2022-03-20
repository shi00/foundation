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
package com.silong.foundation.springboot.starter.cjob.runtime;

import com.hazelcast.durableexecutor.DurableExecutorService;

/**
 * job任务运行时
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-03-19 16:29
 */
public final class ComplexJobRuntime {
  /** 线程池核心数量 */
  private static final int THREAD_CORE_SIZE =
      Integer.parseInt(
          System.getProperty(
              "complex.job.scheduler.thread.count",
              String.valueOf(Runtime.getRuntime().availableProcessors())));

  /** 分布式线程池 */
  private DurableExecutorService executor;

  public ComplexJobRuntime(int capcity, int poolSize, int backupCount) {}
}
