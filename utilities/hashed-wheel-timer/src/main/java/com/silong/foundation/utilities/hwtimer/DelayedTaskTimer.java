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
package com.silong.foundation.utilities.hwtimer;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * 延时任务定时器接口
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-06-24 23:18
 */
public interface DelayedTaskTimer {

  /**
   * 启动定时器
   *
   * @return true or false
   */
  boolean start();

  /**
   * 停止定时器
   *
   * @return true or false
   */
  boolean stop();

  /**
   * 提交延时任务
   *
   * @param name 唯一任务名
   * @param runnable 任务逻辑
   * @param delay 延时时长
   * @param timeUnit 延时单位
   * @return 延时任务
   * @throws Exception 异常
   */
  DelayedTask submit(String name, Runnable runnable, long delay, TimeUnit timeUnit)
      throws Exception;

  /**
   * 提交延时任务
   *
   * @param name 唯一任务名
   * @param callable 任务逻辑
   * @param delay 延时时长
   * @param timeUnit 延时单位
   * @return 延时任务
   * @param <R> 任务结果类型
   * @throws Exception 异常
   */
  <R> DelayedTask submit(String name, Callable<R> callable, long delay, TimeUnit timeUnit)
      throws Exception;
}
