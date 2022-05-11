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
package com.silong.foundation.devastator.utils;

import lombok.SneakyThrows;
import org.jctools.queues.MpscArrayQueue;

import java.io.Closeable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.silong.foundation.devastator.core.DefaultObjectPartitionMapping.calculateMask;
import static java.lang.Runtime.getRuntime;

/**
 * 定时器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-05-08 22:17
 */
public class HashedWheelTimer implements Closeable, Runnable {

  /** 定时轮时间间隔配置 */
  public static final String TIMER_TICK_DURATION = "timer.tick.duration";

  /** 定时轮slot数量配置 */
  public static final String TIMER_WHEEL_SIZE = "timer.wheel.size";

  /** 定时器间隔时间单位 */
  private static final TimeUnit TICK_DURATION_UNIT = TimeUnit.MILLISECONDS;

  /** 单例 */
  public static final HashedWheelTimer INSTANCE = new HashedWheelTimer();

  /** 定时器间隔时间 */
  private static final long TICK_DURATION =
      Integer.parseInt(System.getProperty(TIMER_TICK_DURATION, "1"));

  /** 定时轮slot数量 */
  private static final int TICK_PER_WHEEL =
      tableSizeFor(Integer.parseInt(System.getProperty(TIMER_WHEEL_SIZE, "256")));

  /** MARK */
  private final int mark;

  /** 任务执行线程池 */
  private final ExecutorService timerExecutors;

  /** 定时器启动标识 */
  private final AtomicBoolean TIMER_STARTED_FLAG = new AtomicBoolean(false);

  /** 定时器启动latch */
  private final CountDownLatch TIMER_STARTED_LATCH = new CountDownLatch(1);

  /** 定时器线程 */
  private Thread timerThread;

  /** 启动时间 */
  private long startedTime;

  private volatile boolean timerThreadRunning;

  /** timer wheel */
  private final HashedWheelBucket[] wheel = new HashedWheelBucket[TICK_PER_WHEEL];

  /** 定时任务提交队列 */
  private final MpscArrayQueue<Runnable> taskQueue = new MpscArrayQueue<>(1024);

  /** 禁止实例化 */
  private HashedWheelTimer() {
    AtomicInteger count = new AtomicInteger(0);
    timerExecutors =
        Executors.newFixedThreadPool(
            getRuntime().availableProcessors() * 2,
            r -> new Thread(r, "hashed-wheel-timer-executor" + count.getAndIncrement()));

    ThreadFactory timerThreadFactory = r -> new Thread(r, "hashed-wheel-timer-trigger");
    timerThread = timerThreadFactory.newThread(this);
    mark = calculateMask(TICK_PER_WHEEL);
  }

  /** 启动定时器 */
  @SneakyThrows
  public void start() {
    if (TIMER_STARTED_FLAG.compareAndSet(false, true)) {
      timerThreadRunning = true;
      timerThread.start();
      TIMER_STARTED_LATCH.countDown();
    } else {
      TIMER_STARTED_LATCH.await();
    }
  }

  /** 停止定时器 */
  public void stop() {
    List<Runnable> unRunning = timerExecutors.shutdownNow();
  }

  @Override
  public void close() {
    stop();
  }

  @Override
  public void run() {
    startedTime = System.nanoTime();
    while (timerThreadRunning) {}
  }

  /** 定时任务 */
  public interface TimerTask extends Runnable {

    /**
     * 是否超期，可以被执行
     *
     * @return {@code true} or {@code false}
     */
    boolean isExpired();
  }

  /** 定时轮bucket */
  private static class HashedWheelBucket {
    /** 任务列表 */
    private final LinkedList<TimerTask> timerTasks;

    /** 构造方法 */
    public HashedWheelBucket() {
      timerTasks = new LinkedList<>();
    }

    /**
     * 添加任务
     *
     * @param timerTask 定时任务
     */
    public void add(TimerTask timerTask) {
      assert timerTask != null;
      timerTasks.add(timerTask);
    }
  }

  /** Returns a power of two size for the given target capacity. */
  private static int tableSizeFor(int cap) {
    int max = 1 << 30;
    int n = -1 >>> Integer.numberOfLeadingZeros(cap - 1);
    return (n < 0) ? 1 : (n >= max) ? max : n + 1;
  }
}
