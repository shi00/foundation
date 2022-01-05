package com.silong.fundation.duuid.generator.impl;

import com.silong.fundation.duuid.generator.DuuidGenerator;

/**
 * Twitter SnowFlake ID
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-03 08:58
 */
public class TwitterSnowFlakeIdGenerator implements DuuidGenerator {
  /** 起始的时间戳 */
  private static final long START_STMP = 1480166465631L;

  /** 序列号占用的位数 */
  private static final long SEQUENCE_BIT = 12;

  /** 机器标识占用的位数 */
  private static final long MACHINE_BIT = 5;

  /** 数据中心占用的位数 */
  private static final long DATACENTER_BIT = 5;

  /** 每一部分的最大值 */
  private static final long MAX_DATACENTER_NUM = ~(-1L << DATACENTER_BIT);

  private static final long MAX_MACHINE_NUM = ~(-1L << MACHINE_BIT);
  private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BIT);

  /** 每一部分向左的位移 */
  private static final long MACHINE_LEFT = SEQUENCE_BIT;

  private static final long DATACENTER_LEFT = SEQUENCE_BIT + MACHINE_BIT;
  private static final long TIMESTMP_LEFT = DATACENTER_LEFT + DATACENTER_BIT;

  /** 数据中心 */
  private final long datacenterId;
  /** 机器标识 */
  private final long machineId;
  /** 序列号 */
  private long sequence = 0L;
  /** 上一次时间戳 */
  private long lastStmp = -1L;

  /**
   * 构造方法
   *
   * @param datacenterId 数据中心id
   * @param machineId 服务器id
   */
  public TwitterSnowFlakeIdGenerator(long datacenterId, long machineId) {
    if (datacenterId > MAX_DATACENTER_NUM || datacenterId < 0) {
      throw new IllegalArgumentException(
          "datacenterId can't be greater than MAX_DATACENTER_NUM or less than 0");
    }
    if (machineId > MAX_MACHINE_NUM || machineId < 0) {
      throw new IllegalArgumentException(
          "machineId can't be greater than MAX_MACHINE_NUM or less than 0");
    }
    this.datacenterId = datacenterId;
    this.machineId = machineId;
  }

  /**
   * 产生下一个ID
   *
   * @return id
   */
  @Override
  public synchronized long nextId() {
    long currStmp = getNewstmp();
    if (currStmp < lastStmp) {
      throw new RuntimeException("Clock moved backwards.  Refusing to generate id");
    }

    if (currStmp == lastStmp) {
      // 相同毫秒内，序列号自增
      sequence = (sequence + 1) & MAX_SEQUENCE;
      // 同一毫秒的序列数已经达到最大
      if (sequence == 0L) {
        currStmp = getNextMill();
      }
    } else {
      // 不同毫秒内，序列号置为0
      sequence = 0L;
    }

    lastStmp = currStmp;

    return (currStmp - START_STMP) << TIMESTMP_LEFT // 时间戳部分
        | datacenterId << DATACENTER_LEFT // 数据中心部分
        | machineId << MACHINE_LEFT // 机器标识部分
        | sequence; // 序列号部分
  }

  private long getNextMill() {
    long mill = getNewstmp();
    while (mill <= lastStmp) {
      mill = getNewstmp();
    }
    return mill;
  }

  private long getNewstmp() {
    return System.currentTimeMillis();
  }
}