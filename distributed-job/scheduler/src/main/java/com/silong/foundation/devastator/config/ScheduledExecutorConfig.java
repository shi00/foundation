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
package com.silong.foundation.devastator.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 任务调度器配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-15 22:12
 */
@Data
@Accessors(fluent = true)
public class ScheduledExecutorConfig implements Serializable {

  @Serial private static final long serialVersionUID = 9169316777157006138L;

  /** 默认线程池线程名前缀 */
  public static final String DEFAULT_THREAD_PREFIX = "devastator-scheduler-";

  /** 默认调度器名 */
  public static final String DEFAULT_NAME = "devastator-scheduler";

  /** 任务调度器名字，默认： devastator-scheduler */
  @NotEmpty private String name = DEFAULT_NAME;

  /** 核心线程数，默认：10 */
  @Positive private int threadCoreSize = Runtime.getRuntime().availableProcessors();

  /** 线程池线程名字前缀，默认： devastator-scheduler- */
  @NotEmpty private String threadNamePrefix = DEFAULT_THREAD_PREFIX;
}
