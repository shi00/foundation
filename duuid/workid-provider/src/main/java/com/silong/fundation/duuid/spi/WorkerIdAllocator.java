package com.silong.fundation.duuid.spi;

/**
 * workid分配器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2021-12-30 21:49
 */
public interface WorkerIdAllocator {
  /**
   * 根据worker信息分配worker id
   *
   * @param info worker信息
   * @return workerid
   */
  int allocate(WorkerInfo info);
}
