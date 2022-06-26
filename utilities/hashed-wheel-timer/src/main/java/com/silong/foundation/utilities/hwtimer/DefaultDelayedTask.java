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

import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 延时任务默认实现
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-06-24 16:57
 */
@Slf4j
@ToString
@NoArgsConstructor
class DefaultDelayedTask implements DelayedTask, Closeable {

  /** 时钟轮数，由于任务延时时间大于时间轮刻度表示范围，需要引入轮数表示触发时间 */
  long rounds;

  /** 任务触发时间 */
  long deadLine;

  /** 任务名 */
  String name;

  /** 任务状态 */
  AtomicReference<State> stateRef = new AtomicReference<>(State.READY);

  /** 任务执行结束信号 */
  @ToString.Exclude CountDownLatch2 signal = new CountDownLatch2(1);

  /** 任务逻辑 */
  @ToString.Exclude Callable<?> callable;

  /** 任务执行异常 */
  @ToString.Exclude Exception exception;

  /** 任务执行结果 */
  @ToString.Exclude Object result;

  /** 任务归属定时器 */
  @ToString.Exclude HashedWheelTimer wheelTimer;

  /**
   * 包裹延时任务执行逻辑
   *
   * @return 执行逻辑
   */
  public Runnable wrap() {
    return () -> {
      try {
        if (stateRef.compareAndSet(State.READY, State.RUNNING)) {
          try {
            log.debug("Start executing DelayTask:{}.", name);
            result = callable.call();
            log.debug("DelayTask:{} has been successfully executed.", name);
            stateRef.compareAndSet(State.RUNNING, State.FINISH);
          } catch (Exception e) {
            stateRef.compareAndSet(State.RUNNING, State.EXCEPTION);
            log.error("Failed to execute DelayTask:{}.", name, exception = e);
          }
        } else {
          log.debug(
              "DelayTask:{} is ignored, because its {} is not {}.",
              name,
              getState(),
              DelayedTask.State.READY);
        }
      } finally {
        signal.countDown();
      }
    };
  }

  @Override
  public boolean cancel() {
    boolean result = stateRef.compareAndSet(State.READY, State.CANCELLED);
    // 如果任务被取消执行，需要考虑等待执行结果或者异常的阻塞线程可以正常被唤醒
    if (result) {
      signal.countDown();
    }
    return result;
  }

  @Override
  public Exception getException() throws InterruptedException {
    signal.await();
    return exception;
  }

  @Override
  public <R> R getResult() throws InterruptedException {
    signal.await();
    return (R) result;
  }

  @Override
  public State getState() {
    return stateRef.get();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof DefaultDelayedTask task) {
      return Objects.equals(name, task.name)
          && signal == task.signal
          && stateRef == task.stateRef
          && rounds == task.rounds
          && deadLine == task.deadLine;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        rounds, deadLine, name, stateRef, signal, callable, exception, result, wheelTimer);
  }

  /** 归还对象至对象池 */
  @Override
  public void close() {
    try {
      wheelTimer.delayedTaskObjectPool.returnObject(this);
    } catch (Exception e) {
      log.error("Failed to return {} to delayedTaskObjectPool.", this, e);
    }
  }
}
