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
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.jctools.queues.MpscUnboundedArrayQueue;

import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 时间轮定时器实现
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-06-24 17:51
 */
@Slf4j
public class HashedWheelTimer implements DelayedTaskTimer, Runnable {

  private static final ThreadFactory CLOCK_THREAD_FACTORY =
      new ThreadFactory() {

        private static final AtomicInteger COUNT = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
          return new Thread(
              r, HashedWheelTimer.class.getSimpleName() + "-" + COUNT.getAndIncrement());
        }
      };

  /** 默认时间轮长度 */
  private static final int DEFAULT_WHEEL_SIZE = 64;

  /** 默认单位时间间隔，毫秒 */
  private static final int DEFAULT_TICK_MS = 1;

  /** 默认定时器内的最大任务数 */
  private static final int DEFAULT_MAX_TASK_COUNT = 1024 * 100;

  /**
   * The maximum capacity, used if a higher value is implicitly specified by either of the
   * constructors with arguments. MUST be a power of two <= 1<<30.
   */
  private static final int MAXIMUM_CAPACITY = 1 << 30;

  final ObjectPool<DefaultDelayedTask> delayedTaskObjectPool;

  /** 时间轮 */
  private WheelBucket[] wheelBuckets;

  /** 时针位置 */
  private long tick;

  /** 时钟格单位间隔，单位：纳秒 */
  private final long tickNs;

  /** 起始时间，纳秒 */
  private long startedTime;

  /** 定时器线程 */
  private final Thread clockThread = CLOCK_THREAD_FACTORY.newThread(this);

  /** 任务提交队列 */
  private MpscUnboundedArrayQueue<DefaultDelayedTask> taskQueue =
      new MpscUnboundedArrayQueue<>(DEFAULT_WHEEL_SIZE);

  /** 定时器启动标识 */
  private final CountDownLatch startedFlag = new CountDownLatch(1);

  /** 定时器停止标识 */
  private final CountDownLatch stoppedFlag = new CountDownLatch(1);

  /** 线程运行标识 */
  private final AtomicBoolean isClockRunning = new AtomicBoolean(false);

  /** 任务执行器 */
  private ExecutorService executor;

  /** 快速计算时针位置辅助 */
  private final int mark;

  /** 默认构造方法 */
  public HashedWheelTimer() {
    this(
        DEFAULT_WHEEL_SIZE,
        DEFAULT_TICK_MS,
        TimeUnit.MILLISECONDS,
        DEFAULT_MAX_TASK_COUNT,
        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
  }

  /**
   * 构造方法
   *
   * @param wheelSize 时间轮长度
   * @param tickInterval 时间轮最小刻度
   * @param tickIntervalUnit 时间轮最小刻度时间单位
   * @param maxTaskCount 定时器可接受的最大任务数
   * @param executor 任务执行器
   */
  public HashedWheelTimer(
      int wheelSize,
      long tickInterval,
      TimeUnit tickIntervalUnit,
      int maxTaskCount,
      ExecutorService executor) {
    if (wheelSize <= 0) {
      throw new IllegalArgumentException("wheelSize must be greater than 0.");
    }
    if (tickInterval < 1) {
      throw new IllegalArgumentException("tickInterval must be greater than or equals to 1.");
    }
    if (tickIntervalUnit == null) {
      throw new IllegalArgumentException("tickIntervalUnit must not be null.");
    }
    if (maxTaskCount <= 0) {
      throw new IllegalArgumentException("maxTaskCount must be greater than 0.");
    }
    if (executor == null) {
      throw new IllegalArgumentException("executor must not be null.");
    }

    this.tick = 0;
    this.tickNs = tickIntervalUnit.toNanos(tickInterval);
    this.executor = executor;
    this.wheelBuckets = new WheelBucket[powerOf2(wheelSize)];
    for (int i = 0; i < wheelBuckets.length; i++) {
      wheelBuckets[i] = new WheelBucket();
    }
    this.mark = calculateMask(wheelBuckets.length);
    GenericObjectPoolConfig<DefaultDelayedTask> objectPoolConfig = new GenericObjectPoolConfig<>();
    objectPoolConfig.setMaxIdle(32);
    objectPoolConfig.setMinIdle(8);
    objectPoolConfig.setMaxTotal(maxTaskCount);
    this.delayedTaskObjectPool =
        new GenericObjectPool<>(new DelayedTaskFactory(), objectPoolConfig);
  }

  @Override
  public boolean start() {
    boolean result = true;
    if (isClockRunning.compareAndSet(false, true)) {
      clockThread.start();
    }
    try {
      startedFlag.await();
    } catch (InterruptedException e) {
      result = false;
      log.error("Failed to start {}.", clockThread.getName(), e);
    }
    return result;
  }

  @Override
  public boolean stop() {
    isClockRunning.compareAndSet(true, false);
    try {
      stoppedFlag.await();
    } catch (InterruptedException e) {
      log.error("Failed to stop {}.", clockThread.getName(), e);
      return false;
    }
    close();
    return true;
  }

  @Override
  public DelayedTask submit(String name, Runnable runnable, long delay, TimeUnit timeUnit)
      throws Exception {
    return submit(
        name,
        () -> {
          runnable.run();
          return null;
        },
        delay,
        timeUnit);
  }

  @Override
  public <R> DelayedTask submit(String name, Callable<R> callable, long delay, TimeUnit timeUnit)
      throws Exception {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("name must not be null or empty.");
    }
    if (callable == null) {
      throw new IllegalArgumentException("callable must not be null.");
    }
    if (delay < 0) {
      throw new IllegalArgumentException("delay must be greater than or equal to 0.");
    }
    if (timeUnit == null) {
      throw new IllegalArgumentException("timeUnit must not be null.");
    }
    if (stoppedFlag.getCount() == 0) {
      throw new IllegalStateException(
          String.format(
              "%s has been stopped and the task cannot be submitted.", clockThread.getName()));
    }

    // 启动定时器
    if (!start()) {
      throw new IllegalStateException(String.format("Failed to start %s.", clockThread.getName()));
    }

    // 计算任务触发时间，可能出现负数，表示当前定时器启动时间到任务提交时间之差超过了最大值
    // 需要处理这种特殊情况，出现负值则按最大值处理
    long deadLine = System.nanoTime() - startedTime + timeUnit.toNanos(delay);
    DefaultDelayedTask defaultDelayedTask = delayedTaskObjectPool.borrowObject();
    defaultDelayedTask.deadLine = deadLine < 0 ? Long.MAX_VALUE : deadLine;
    defaultDelayedTask.name = name;
    defaultDelayedTask.callable = callable;
    defaultDelayedTask.wheelTimer = this;
    taskQueue.offer(defaultDelayedTask);
    return defaultDelayedTask;
  }

  private long waitForNextTick() {
    long nextTick = tickNs * (tick + 1);
    while (true) {
      long currentTime = System.nanoTime() - startedTime;
      if (nextTick > currentTime) {
        Thread.onSpinWait();
      } else {
        return currentTime;
      }
    }
  }

  @Override
  public void run() {
    startedTime = System.nanoTime();
    startedFlag.countDown();
    log.info("{} has been started.", clockThread.getName());
    do {
      // 获取下一个触发时间，如果需要会sleep
      long deadLine = waitForNextTick();

      // 延迟任务插入时间轮
      appendTasks();

      int index = (int) (tick & mark);
      wheelBuckets[index].trigger(
          tick / wheelBuckets.length, deadLine, task -> task.result = executor.submit(task.wrap()));

      tick++;
    } while (isClockRunning.get());
    stoppedFlag.countDown();
    log.info("{} has been stopped.", clockThread.getName());
  }

  private void appendTasks() {
    DefaultDelayedTask defaultDelayedTask;
    while ((defaultDelayedTask = taskQueue.poll()) != null) {
      if (defaultDelayedTask.getState() == DelayedTask.State.READY) {
        long tickCount = defaultDelayedTask.deadLine / tickNs;
        long maxTick = Math.max(tick, tickCount);
        defaultDelayedTask.rounds = maxTick / wheelBuckets.length;
        int index = (int) (maxTick & mark);
        wheelBuckets[index].add(defaultDelayedTask);
      } else {
        log.debug(
            "DelayTask:{} is ignored because its status is not {}",
            defaultDelayedTask.getName(),
            DelayedTask.State.READY);
      }
    }
  }

  /** 释放定时器资源 */
  private void close() {
    if (wheelBuckets != null) {
      Arrays.stream(wheelBuckets).forEach(WheelBucket::close);
      wheelBuckets = null;
    }

    if (taskQueue != null) {
      taskQueue.clear();
      taskQueue = null;
    }

    if (executor != null) {
      executor.shutdown();
      executor = null;
    }

    if (delayedTaskObjectPool != null) {
      delayedTaskObjectPool.close();
    }
  }

  /** Returns a power of two size for the given target capacity. */
  private static int powerOf2(int cap) {
    int n = -1 >>> Integer.numberOfLeadingZeros(cap - 1);
    return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
  }

  private static int calculateMask(int wheels) {
    return (wheels & (wheels - 1)) == 0 ? wheels - 1 : -1;
  }
}
