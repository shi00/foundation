package com.silong.foundation.ctask.scheduler;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 任务执行线程池
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-03-03 14:48
 */
@Slf4j
public sealed class ComplexTaskExecutors permits ComplexTaskScheduler {

  private static final AtomicInteger COUNT = new AtomicInteger(0);

  private static final int THREAD_CORE_SIZE =
      Integer.parseInt(
          System.getProperty(
              "task.scheduler.thread.count", "" + Runtime.getRuntime().availableProcessors()));

  private static final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE;

  static {
    SCHEDULED_EXECUTOR_SERVICE =
        new ScheduledThreadPoolExecutor(THREAD_CORE_SIZE, r -> new Thread(r, ComplexTaskExecutors.class.getSimpleName()+COUNT.incrementAndGet()), new ThreadPoolExecutor.AbortPolicy());
  }

  /**
   * 获取执行线程池
   *
   * @return 线程池
   */
  public ScheduledExecutorService getExecutor() {
    return SCHEDULED_EXECUTOR_SERVICE;
  }
}
