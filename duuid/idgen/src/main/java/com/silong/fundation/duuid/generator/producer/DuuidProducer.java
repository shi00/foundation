package com.silong.fundation.duuid.generator.producer;

import lombok.extern.slf4j.Slf4j;
import org.jctools.queues.MessagePassingQueue.Supplier;
import org.jctools.queues.SpmcArrayQueue;

/**
 * id生成器，负责往环状队列内填充id，供消费者取用
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2021-12-30 22:06
 */
@Slf4j
public class DuuidProducer extends Thread {

  /** single-producer-multiple-consumers circular queue */
  private final SpmcArrayQueue<Long> queue;

  /** 环状队列填充率，即需要保证环状队列内的填充的id和容量的占比 */
  private final double paddingFactor;

  private final Supplier<Long> idSupplier;

  /**
   * 构造方法，启动生产者线程
   *
   * @param queue 环状队列
   * @param idSupplier id供应商
   * @param paddingFactor 队列填充率
   */
  public DuuidProducer(
      SpmcArrayQueue<Long> queue, Supplier<Long> idSupplier, double paddingFactor) {
    if (queue == null) {
      throw new IllegalArgumentException("queue must not be null.");
    }
    if (idSupplier == null) {
      throw new IllegalArgumentException("idSupplier must not be null.");
    }
    if (paddingFactor <= 0 || paddingFactor > 1.0) {
      throw new IllegalArgumentException("paddingFactor value range (0, 1.0].");
    }
    setName("duuid-producer");
    this.queue = queue;
    this.idSupplier = idSupplier;
    this.paddingFactor = paddingFactor;
    start();
  }

  /** 循环生成id，填充环状队列 */
  @Override
  public void run() {
    queue.fill(
        idSupplier,
        idleCounter -> {
          // 计算当前队列填充率，如果大于等于阈值则放弃CPU时间片，马上重新竞争时间片
          while ((queue.size() * 1.0 / queue.capacity()) >= paddingFactor) {
            try {
              Thread.sleep(0);
            } catch (InterruptedException e) {
              log.error("Fatal error.", e);
            }
          }
          return idleCounter;
        },
        () -> true);
  }
}
