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

import com.silong.foundation.devastator.exception.GeneralException;
import com.silong.foundation.devastator.exception.InitializationException;
import com.silong.foundation.devastator.timer.Timer;
import lombok.extern.slf4j.Slf4j;
import org.jctools.queues.MpscArrayQueue;

import java.util.LinkedHashSet;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.silong.foundation.devastator.core.DefaultObjectPartitionMapping.calculateMask;
import static com.silong.foundation.devastator.timer.TimerTask.State.CANCELLED;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;

/**
 * 定时器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-05-08 22:17
 */
@Slf4j
public class HashedWheelTimer implements Timer, Runnable {

  /** 单例 */
  public static final HashedWheelTimer INSTANCE = new HashedWheelTimer();

  /** 定时轮时间间隔配置 */
  public static final String TIMER_TICK_DURATION = "timer.tick.duration";

  /** 定时轮slot数量配置 */
  public static final String TIMER_WHEEL_SIZE = "timer.wheel.size";

  /** 任务执行线程数量 */
  public static final String TIMER_TASK_EXECUTORS_COUNT = "timer.task.executors.count";

  /** 定时任务队列 */
  public static final String TIMER_TASK_QUEUE_SIZE = "timer.task.queue.size";

  /** 定时器间隔时间单位 */
  private static final TimeUnit TICK_DURATION_UNIT = MILLISECONDS;

  private static final int MAX_SIZE = 1 << 30;

  /** 定时器间隔时间 */
  private static final long TICK_DURATION =
      Integer.parseInt(System.getProperty(TIMER_TICK_DURATION, "1"));

  /** 定时器间隔时间 */
  private static final int EXECUTORS_COUNT =
      Integer.parseInt(
          System.getProperty(
              TIMER_TASK_EXECUTORS_COUNT,
              String.valueOf(Runtime.getRuntime().availableProcessors())));

  /** 定时轮slot数量 */
  private static final int TICK_PER_WHEEL =
      capacitySizeFor(Integer.parseInt(System.getProperty(TIMER_WHEEL_SIZE, "256")));

  /** 任务队列长度 */
  private static final int TASK_QUEUE_SIZE =
      capacitySizeFor(Integer.parseInt(System.getProperty(TIMER_TASK_QUEUE_SIZE, "256")));

  /** 定时器启动标识 */
  private final AtomicBoolean TIMER_STARTED_FLAG = new AtomicBoolean(false);

  /** 定时器启动latch */
  private final CountDownLatch TIMER_STARTED_LATCH = new CountDownLatch(1);

  /** 定时器停止latch */
  private final CountDownLatch TIMER_STOPPED_LATCH = new CountDownLatch(1);

  /** timer wheel */
  private final LinkedHashSet<HashedTimerTask>[] wheel;

  /** 定时任务提交队列 */
  private final MpscArrayQueue<HashedTimerTask> taskQueue;

  /** 任务执行线程池 */
  private final ExecutorService timerExecutors;

  /** 定时器线程 */
  private final Thread timerThread;

  /** mask */
  private final int mask;

  /** 定时器线程运行标识 */
  private volatile boolean timerThreadRunning;

  /** 启动时间 */
  private long startedTime;

  /** 计时ticker */
  long tick;

  /** 禁止实例化 */
  @SuppressWarnings("PMD.AvoidManuallyCreateThreadRule")
  private HashedWheelTimer() {
    // 初始化定时轮buckets
    wheel = initializeWheelBuckets();

    // 初始化定时任务线程池
    timerExecutors = initializeTimerExecutors();

    // 初始化任务提交队列
    taskQueue = initializeTaskQueue();

    // 初始化定时器线程池
    timerThread = new Thread(this, "hashed-wheel-timer-trigger");
    mask = calculateMask(wheel.length);
  }

  private MpscArrayQueue<HashedTimerTask> initializeTaskQueue() {
    if (TASK_QUEUE_SIZE <= 0) {
      throw new InitializationException("timer.task.queue.size must be greater than 0");
    }
    return new MpscArrayQueue<>(TASK_QUEUE_SIZE);
  }

  private ExecutorService initializeTimerExecutors() {
    if (EXECUTORS_COUNT <= 0) {
      throw new InitializationException("timer.task.executors.count must be greater than 0");
    }
    AtomicInteger count = new AtomicInteger(0);
    return new ThreadPoolExecutor(
        1,
        EXECUTORS_COUNT,
        0L,
        TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>(),
        r -> new Thread(r, "hashed-wheel-timer-executor" + count.getAndIncrement()));
  }

  private LinkedHashSet<HashedTimerTask>[] initializeWheelBuckets() {
    if (TICK_PER_WHEEL > MAX_SIZE || TICK_PER_WHEEL <= 0) {
      throw new InitializationException(
          "timer.wheel.size must be greater than 0 less than or equal to " + MAX_SIZE);
    }
    LinkedHashSet<HashedTimerTask>[] wheel = new LinkedHashSet[TICK_PER_WHEEL];
    for (int i = 0; i < wheel.length; i++) {
      wheel[i] = new LinkedHashSet<>();
    }
    return wheel;
  }

  /** 启动定时器 */
  public void start() {
    if (Thread.currentThread() == timerThread) {
      throw new IllegalStateException(
          String.format("%s cannot start itself.", timerThread.getName()));
    }

    if (TIMER_STARTED_FLAG.compareAndSet(false, true)) {
      timerThreadRunning = true;
      timerThread.start();
    }

    try {
      TIMER_STARTED_LATCH.await();
    } catch (InterruptedException e) {
      throw new GeneralException("Failed to start HashedWheelTimer.", e);
    }
  }

  /** 停止定时器 */
  public void stop() {
    if (Thread.currentThread() == timerThread) {
      throw new IllegalStateException(
          String.format("%s cannot stop itself.", timerThread.getName()));
    }

    try {
      timerThreadRunning = false;
      TIMER_STOPPED_LATCH.await();
      timerExecutors.shutdown();
    } catch (InterruptedException e) {
      throw new GeneralException("Failed to stop HashedWheelTimer.", e);
    }
  }

  @Override
  public void close() {
    stop();
  }

  /**
   * 提交定时任务
   *
   * @param timerTask 定时任务
   * @return 提交的定时任务
   */
  public HashedTimerTask submit(Runnable timerTask, long delay, TimeUnit timeUnit) {
    if (timerTask == null) {
      throw new IllegalArgumentException("timerTask must not be null.");
    }
    if (delay < 0) {
      throw new IllegalArgumentException("delay must be greater than or equal to 0.");
    }
    if (timeUnit == null) {
      throw new IllegalArgumentException("timeUnit must not be null.");
    }

    // 延时启动定时器线程
    start();

    // 计算任务执行时间
    long deadLine = System.nanoTime() - startedTime + timeUnit.toNanos(delay);

    // 溢出保护
    if (deadLine < 0) {
      deadLine = Long.MAX_VALUE;
    }

    HashedTimerTask task = new HashedTimerTask(this, timerTask, deadLine);
    taskQueue.add(task);
    return task;
  }

  /** 定时器业务流 */
  @Override
  public void run() {
    // 初始化定时器启动时间，通知启动定时器方法，启动完毕
    startedTime = System.nanoTime();
    TIMER_STARTED_LATCH.countDown();

    do {

      long deadLine = waitForNextTick();
      if (deadLine > 0) {

        // 计算当前对应的buckets
        int index = (int) (tick & mask);
        LinkedHashSet<HashedTimerTask> bucket = wheel[index];

        // 去掉已被取消的任务
        bucket.removeIf(next -> next.getState() == CANCELLED);

        // 获取提交的定时任务，填充至相应的bucket
        HashedTimerTask task;
        while ((task = taskQueue.poll()) != null) {
          addTask(task);
        }

        tick++;
      }
    } while (timerThreadRunning);

    TIMER_STOPPED_LATCH.countDown();

    log.info("timer has stopped.");
  }

  private void addTask(HashedTimerTask task) {
    if (task.isInit()) {
      long calculated = task.deadLine / TICK_DURATION;
      task.remainingRounds = (calculated - tick) / wheel.length;

      // Ensure we don't schedule for past.
      long ticks = Math.max(calculated, tick);
      int index = (int) (ticks & mask);

      wheel[index].add(task);
      if (log.isDebugEnabled()) {
        log.debug("Add {} to a bucket with index:{}", task.runnable, index);
      }
    } else {
      log.info(
          "{} is ignored because the task status:{} is not State.INIT.",
          task.runnable,
          task.getState());
    }
  }

  private long waitForNextTick() {
    long deadline = TICK_DURATION * (tick + 1);
    while (true) {
      long currentTime = System.nanoTime() - startedTime;
      long sleepTimeMs = (deadline - currentTime + 999999) / 1000000;

      if (sleepTimeMs <= 0) {
        if (currentTime == Long.MIN_VALUE) {
          return -Long.MAX_VALUE;
        } else {
          return currentTime;
        }
      }

      // Check if we run on windows, as if thats the case we will need
      // to round the sleepTime as workaround for a bug that only affect
      // the JVM if it runs on windows.
      //
      // See https://github.com/netty/netty/issues/356
      if (IS_OS_WINDOWS) {
        sleepTimeMs = sleepTimeMs / 10 * 10;
        if (sleepTimeMs == 0) {
          sleepTimeMs = 1;
        }
      }

      try {
        Thread.sleep(sleepTimeMs);
      } catch (InterruptedException ignored) {
        return Long.MIN_VALUE;
      }
    }
  }

  /** Returns a power of two size for the given target capacity. */
  private static int capacitySizeFor(int cap) {
    int n = -1 >>> Integer.numberOfLeadingZeros(cap - 1);
    return (n < 0) ? 1 : (n >= MAX_SIZE) ? MAX_SIZE : n + 1;
  }
}
