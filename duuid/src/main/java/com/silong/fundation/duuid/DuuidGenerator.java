package com.silong.fundation.duuid;

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
   * @return id
   * @throws Exception id生成异常
   */
  long generate() throws Exception;
}
