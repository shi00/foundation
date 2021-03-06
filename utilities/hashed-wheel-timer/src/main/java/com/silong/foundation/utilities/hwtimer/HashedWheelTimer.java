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

import com.silong.foundation.utilities.concurrent.SimpleThreadFactory;
import com.silong.foundation.utilities.pool.SimpleObjectPool;
import lombok.extern.slf4j.Slf4j;
import org.jctools.queues.MpscUnboundedArrayQueue;

import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.silong.foundation.utilities.pool.SimpleObjectPool.buildLinkedListSoftRefObjectPool;
import static com.silong.foundation.utilities.pool.SimpleObjectPool.buildSoftRefObjectPool;

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
      new SimpleThreadFactory(HashedWheelTimer.class.getSimpleName());

  /** 默认时间轮长度 */
  private static final int DEFAULT_WHEEL_SIZE =
      Integer.parseInt(System.getProperty("hwtimer.default.wheel.size", "1024"));

  /** 提交任务队列初始长度 */
  private static final int MPSC_CHUNK_SIZE =
      Integer.parseInt(System.getProperty("hwtimer.default.mpsc.chunk.size", "1024"));

  /** 默认单位时间间隔，毫秒 */
  private static final int DEFAULT_TICK_MS =
      Integer.parseInt(System.getProperty("hwtimer.default.tick.ms", "1"));

  /** 默认定时器内的缓存的最大任务数 */
  private static final int DEFAULT_MAX_POOL_TASK_COUNT =
      Integer.parseInt(System.getProperty("hwtimer.default.max.pool.task.count", "1024"));

  /** 默认定时器内的缓存的最大任务队列数 */
  private static final int DEFAULT_MAX_POOL_TASK_LINKED_LIST_COUNT =
      Integer.parseInt(
          System.getProperty("hwtimer.default.max.pool.task.linked.list.count", "1024"));

  /**
   * The maximum capacity, used if a higher value is implicitly specified by either of the
   * constructors with arguments. MUST be a power of two <= 1<<30.
   */
  private static final int MAXIMUM_CAPACITY = 1 << 30;

  /** 纳秒转毫秒 */
  private static final long MILLI_SCALE = 1000L * 1000L;

  /** 任务对象池 */
  SimpleObjectPool<DefaultDelayedTask> delayedTaskObjectPool;

  /** 任务列表对象池 */
  SimpleObjectPool<TaskLinkedList<DefaultDelayedTask>> taskLinkedListObjectPool;

  /** 时间轮 */
  private WheelBucket[] wheelBuckets;

  /** 时针位置 */
  private long tick;

  /** 时钟格单位间隔，单位：纳秒 */
  private final long tickDurationNs;

  /** 起始时间，纳秒 */
  private long startedTime;

  /** 定时器线程 */
  private final Thread clockThread = CLOCK_THREAD_FACTORY.newThread(this);

  /** 任务提交队列 */
  private MpscUnboundedArrayQueue<DefaultDelayedTask> taskQueue =
      new MpscUnboundedArrayQueue<>(MPSC_CHUNK_SIZE);

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
        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
  }

  /**
   * 构造方法
   *
   * @param wheelSize 时间轮长度
   * @param tickInterval 时间轮最小刻度
   * @param tickIntervalUnit 时间轮最小刻度时间单位
   * @param executor 任务执行器
   */
  public HashedWheelTimer(
      int wheelSize, long tickInterval, TimeUnit tickIntervalUnit, ExecutorService executor) {
    if (wheelSize <= 0) {
      throw new IllegalArgumentException("wheelSize must be greater than 0.");
    }
    if (tickInterval < 1) {
      throw new IllegalArgumentException("tickInterval must be greater than or equals to 1.");
    }
    if (tickIntervalUnit == null) {
      throw new IllegalArgumentException("tickIntervalUnit must not be null.");
    }
    if (executor == null) {
      throw new IllegalArgumentException("executor must not be null.");
    }

    this.tick = 0;
    this.tickDurationNs = tickIntervalUnit.toNanos(tickInterval);
    this.executor = executor;
    this.wheelBuckets = new WheelBucket[powerOf2(wheelSize)];
    for (int i = 0; i < wheelBuckets.length; i++) {
      wheelBuckets[i] = new WheelBucket(this);
    }
    this.mark = wheelBuckets.length - 1;
    // 存在并发因此使用线程安全的对象池
    this.delayedTaskObjectPool =
        buildSoftRefObjectPool(DEFAULT_MAX_POOL_TASK_COUNT, DefaultDelayedTask::new);
    // 单线程访问使用基于LinkedList构建的对象池
    this.taskLinkedListObjectPool =
        buildLinkedListSoftRefObjectPool(
            DEFAULT_MAX_POOL_TASK_LINKED_LIST_COUNT, TaskLinkedList::new);
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
  public DelayedTask submit(String name, Runnable runnable, long delay, TimeUnit timeUnit) {
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
  public <R> DelayedTask submit(String name, Callable<R> callable, long delay, TimeUnit timeUnit) {
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
    long deadLine = currentTime() + timeUnit.toNanos(delay);
    DefaultDelayedTask defaultDelayedTask = delayedTaskObjectPool.obtain();
    defaultDelayedTask.deadLine = deadLine < 0 ? Long.MAX_VALUE : deadLine;
    defaultDelayedTask.name = name;
    defaultDelayedTask.callable = callable;
    defaultDelayedTask.wheelTimer = this;
    taskQueue.offer(defaultDelayedTask);
    return defaultDelayedTask;
  }

  private long currentTime() {
    return System.nanoTime() - startedTime;
  }

  @Override
  public void run() {
    startedTime = System.nanoTime();
    startedFlag.countDown();
    log.info("{} has been started.", clockThread.getName());

    Consumer<DefaultDelayedTask> scheduler = task -> executor.submit(task.wrap());

    do {
      // 确保当前时间与下一个tick时间对齐，即当前时间>=下一个tick时间
      long deadLine;
      long nextTick = tickDurationNs * (tick + 1);
      while (true) {
        if (nextTick > (deadLine = currentTime())) {
          long sleepTime = nextTick - deadLine;
          // 如果休眠时间超过1ms则直接休眠，否则使用onSpinWait()休眠
          if (sleepTime >= MILLI_SCALE) {
            try {
              Thread.sleep(TimeUnit.NANOSECONDS.toMillis(sleepTime));
            } catch (InterruptedException e) {
              throw new IllegalStateException(e);
            }
          } else {
            Thread.onSpinWait();
          }
        } else {
          break;
        }
      }

      // 延迟任务插入时间轮
      int index;
      long rounds = tick / wheelBuckets.length;
      DefaultDelayedTask defaultDelayedTask;
      while ((defaultDelayedTask = taskQueue.poll()) != null) {
        long tickCount = defaultDelayedTask.deadLine / tickDurationNs;
        long roundsTask = tickCount / wheelBuckets.length;
        if (roundsTask <= rounds) {
          // 确保已经过去时间的任务可以被触发
          index = (int) (Math.max(tick, tickCount) & mark);
          defaultDelayedTask.rounds = rounds;
        } else {
          index = (int) (tickCount & mark);
          defaultDelayedTask.rounds = roundsTask;
        }
        wheelBuckets[index].add(defaultDelayedTask);
      }

      // 计算当前tick对应的bucket索引
      index = (int) (tick++ & mark);
      wheelBuckets[index].trigger(rounds, deadLine, scheduler);
    } while (isClockRunning.get());
    stoppedFlag.countDown();
    log.info("{} has been stopped.", clockThread.getName());
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
      delayedTaskObjectPool = null;
    }

    if (taskLinkedListObjectPool != null) {
      taskLinkedListObjectPool.close();
      taskLinkedListObjectPool = null;
    }
  }

  /** Returns a power of two size for the given target size. */
  private static int powerOf2(int size) {
    int n = -1 >>> Integer.numberOfLeadingZeros(size - 1);
    return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
  }
}
