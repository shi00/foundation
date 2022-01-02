package com.silong.fundation.duuid.generator.utils;

/**
 * 常量
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2021-12-31 22:39
 */
public interface Constants {

  /** 默认：生产者线程名前缀 */
  String DUUID_PRODUCER_NAME_PREFIX = "Duuid-Producer-";

  /** 默认环状队列容量，为确保队列性能必须为2的次方，默认值：2的16次方 */
  int DEFAULT_QUEUE_CAPACITY = 65536;

  /** 环状队列填充率，默认：0.7 */
  double DEFAULT_PADDING_FACTOR = 0.7;

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
}
