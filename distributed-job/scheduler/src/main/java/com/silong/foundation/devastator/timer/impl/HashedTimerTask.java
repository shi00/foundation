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
package com.silong.foundation.devastator.timer.impl;

import com.silong.foundation.devastator.timer.TimerTask;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 定时任务
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-05-14 08:37
 */
@Slf4j
class HashedTimerTask implements TimerTask {

  /** 任务关联定时器 */
  final HashedWheelTimer timer;

  /** 定时任务 */
  final Runnable runnable;

  /** 任务执行时间，以添加任务至定时器时间点为基准，单位：ns */
  final long deadLine;

  /** 任务轮次 */
  long remainingRounds;

  /** 任务执行异常 */
  volatile Throwable throwable;

  /** 任务状态 */
  private final AtomicReference<State> state = new AtomicReference<>(State.INIT);

  /**
   * 构造方法
   *
   * @param timer 任务关联定时器
   * @param runnable 定时任务
   * @param deadLine 执行时间
   */
  public HashedTimerTask(HashedWheelTimer timer, Runnable runnable, long deadLine) {
    assert timer != null;
    assert runnable != null;
    assert deadLine >= 0;
    this.timer = timer;
    this.runnable =
        runnable instanceof HashedTimerTask ? ((HashedTimerTask) runnable).runnable : runnable;
    this.deadLine = deadLine;
  }

  @Override
  public void run() {
    if (state.compareAndSet(State.INIT, State.EXECUTING)) {
      try {
        runnable.run();
        state.compareAndSet(State.EXECUTING, State.FINISH);
      } catch (Throwable t) {
        state.compareAndSet(State.EXECUTING, State.EXCEPTION);
        throwable = t;
        log.error("Failed to execute {}.", runnable, t);
      }
    } else {
      log.info(
          "Because the {} state is {} and not {}, it is not executed.",
          runnable,
          state.get(),
          State.INIT);
    }
  }

  @Override
  public Throwable getException() {
    return throwable;
  }

  @Override
  public State getState() {
    return state.get();
  }

  /**
   * 是否初始状态
   *
   * @return {@code true} or {@code false}
   */
  public boolean isInit() {
    return getState() == State.INIT;
  }

  /**
   * 取消任务
   *
   * @return 取消成功 {@code true}，否则 {@code false}
   */
  public boolean cancel() {
    return state.compareAndSet(State.INIT, State.CANCELLED);
  }

  /**
   * 任务是否超期
   *
   * @param now 给定时间点
   * @return {@code true} or {@code false}
   */
  public boolean expired(long now) {
    return this.deadLine <= now;
  }

  @Override
  public boolean equals(Object o) {
    return this == o;
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(this);
  }
}
