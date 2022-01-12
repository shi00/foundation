package com.silong.foundation.duuid.generator;

/**
 * 分布式ID生成器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2021-12-28 22:25
 */
public interface DuuidGenerator {

  /**
   * 生成uuid
   *
   * @return 生成id
   */
  long nextId();
}
