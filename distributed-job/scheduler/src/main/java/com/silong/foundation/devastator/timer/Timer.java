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
package com.silong.foundation.devastator.timer;

import javax.annotation.concurrent.ThreadSafe;
import java.io.Closeable;
import java.util.concurrent.TimeUnit;

/**
 * 定时器接口
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-05-21 08:31
 */
@ThreadSafe
public interface Timer extends Closeable {

  /** 启动定时器 */
  void start();

  /** 停止定时器 */
  void stop();

  /**
   * 提交任务至定时器，并指定执行时延和单位
   *
   * @param task 待执行任务
   * @param delay 任务执行时延
   * @param timeUnit 时延单位
   * @return 定时任务
   */
  TimerTask submit(Runnable task, long delay, TimeUnit timeUnit);
}
