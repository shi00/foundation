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

import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 定时轮单元格
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-06-25 12:14
 */
@Slf4j
class WheelBucket implements Closeable {
  /** 分层任务列表 */
  Map<Long, LinkedList<DefaultDelayedTask>> roundTasks;

  /** 构造方法 */
  public WheelBucket() {
    roundTasks = new HashMap<>();
  }

  /**
   * 添加任务
   *
   * @param task 任务
   */
  public void add(DefaultDelayedTask task) {
    if (task == null) {
      throw new IllegalArgumentException("task must not be null.");
    }
    roundTasks.computeIfAbsent(task.rounds, k -> new LinkedList<>()).add(task);
  }

  /**
   * 根据当前时间，轮次触发任务执行
   *
   * @param rounds 轮次
   * @param currentTime 当前时间
   * @param executor 执行器
   */
  public void trigger(long rounds, long currentTime, Consumer<DefaultDelayedTask> executor) {
    if (rounds < 0) {
      throw new IllegalArgumentException("rounds must be greater and equals 0.");
    }
    if (executor == null) {
      throw new IllegalArgumentException("executor must not be null.");
    }

    LinkedList<DefaultDelayedTask> defaultDelayedTasks = roundTasks.get(rounds);
    if (defaultDelayedTasks == null || defaultDelayedTasks.isEmpty()) {
      removeLastRounds(rounds);
      return;
    }

    DefaultDelayedTask task;
    while ((task = defaultDelayedTasks.poll()) != null) {
      if (task.getState() == DelayedTask.State.READY) {
        if (task.deadLine <= currentTime) {
          executor.accept(task);
        } else {
          // The task was placed into a wrong slot. This should never happen.
          throw new IllegalStateException(
              String.format("task.deadline (%d) > currentTime (%d)", task.deadLine, currentTime));
        }
      } else {
        log.debug(
            "DelayTask:{} is ignored, because its {} is not {}.",
            task.getName(),
            task.getState(),
            DelayedTask.State.READY);
      }
    }

    removeLastRounds(rounds);
  }

  private void removeLastRounds(long rounds) {
    long lastRounds = rounds - 1;
    if (lastRounds >= 0 && roundTasks.containsKey(lastRounds)) {
      LinkedList<DefaultDelayedTask> deleted = roundTasks.remove(lastRounds);
      if (deleted != null && !deleted.isEmpty()) {
        throw new IllegalStateException(
            String.format("Discover %s that were not triggered for execution.", deleted));
      }
    }
  }

  @Override
  public void close() {
    if (roundTasks != null) {
      roundTasks.clear();
      roundTasks = null;
    }
  }
}
