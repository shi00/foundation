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

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
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
@EqualsAndHashCode
@NoArgsConstructor
class DefaultDelayedTask implements DelayedTask, Closeable {

  /** 时钟轮数，由于任务延时时间大于时间轮刻度表示范围，需要引入轮数表示触发时间 */
  long rounds;

  /** 任务触发时间 */
  long deadLine;

  /** 任务逻辑 */
  Callable callable;

  /** 任务名 */
  String name;

  /** 任务执行异常 */
  volatile Exception exception;

  /** 任务执行结果 */
  volatile Future result;

  /** 任务归属定时器 */
  HashedWheelTimer wheelTimer;

  /** 任务状态 */
  AtomicReference<State> stateRef = new AtomicReference<>(State.READY);

  /**
   * 包裹延时任务执行逻辑
   *
   * @return 执行逻辑
   */
  public <R> Callable<R> wrap() {
    return () -> {
      if (stateRef.compareAndSet(State.READY, State.RUNNING)) {
        try {
          log.debug("Start executing DelayTask:{}.", name);
          R call = (R) callable.call();
          log.debug("DelayTask:{} has been successfully executed.", name);
          stateRef.compareAndSet(State.RUNNING, State.FINISH);
          return call;
        } catch (Exception e) {
          log.error("Failed to execute DelayTask:{}.", name, exception = e);
          stateRef.compareAndSet(State.RUNNING, State.EXCEPTION);
          return null;
        }
      } else {
        log.info("DelayTask:{} is not in the {} state and cannot be executed.", name, State.READY);
        return null;
      }
    };
  }

  @Override
  public boolean cancel() {
    return stateRef.compareAndSet(State.READY, State.CANCELLED);
  }

  @Override
  public Exception getException() {
    return exception;
  }

  @Override
  public <R> Future<R> getResult() {
    return result;
  }

  @Override
  public State getState() {
    return stateRef.get();
  }

  @Override
  public String getName() {
    return name;
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
