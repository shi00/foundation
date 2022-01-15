package com.silong.foundation.duuid.generator;

import java.io.Closeable;

/**
 * 分布式ID生成器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2021-12-28 22:25
 */
public interface DuuidGenerator extends Closeable {

  /** 释放资源 */
  @Override
  default void close() {}

  /**
   * 生成uuid
   *
   * @return 生成id
   */
  Long nextId();
}
