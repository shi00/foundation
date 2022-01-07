package com.silong.fundation.duuid.generator.utils;

/**
 * 常量
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2021-12-31 22:39
 */
public interface Constants {

  /** 默认环状队列容量，为确保队列性能必须为2的次方，默认值：2的9次方 */
  int DEFAULT_QUEUE_CAPACITY = 512;

  /** id生成器期望QPS：默认：100000 */
  long DEFAULT_EXPECTED_QPS = 100000;

  /** 最小Id生成器预期QPS */
  long MIN_EXPECTED_QPS = 10000;

  /** 以2020-01-01 00:00:00为起始时间距1970-01-01 00:00:00的天数差，默认值：18261 */
  long EPOCH = 18261;

  /** 最高位为符号位，为了确保生成id为正数，符号位固定为0 */
  int SIGN_BIT = 1;

  /** 节点id占用的bits */
  int DEFAULT_WORK_ID_BITS = 23;

  /** 启动时间与基准时间的差值，单位：天 */
  int DEFAULT_DELTA_DAYS_BITS = 15;

  /** 序号占用bit位 */
  int DEFAULT_SEQUENCE_BITS = 25;

  /** 默认最大随机增量32 */
  int DEFAULT_MAX_RANDOM_INCREMENT = 32;
}
