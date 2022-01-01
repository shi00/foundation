package com.silong.fundation.duuid.generator.utils;

import lombok.Data;

import static com.silong.fundation.duuid.generator.utils.Constants.SIGN_BIT;

/**
 * long型比特位计算器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-01 19:15
 */
@Data
public final class LongBitsCalculator {

  /** workerId占用比特位数 */
  private final int workerIdBits;

  /** deltaDays占用比特位数 */
  private final int deltaDaysBits;

  /** 序号占用比特位数 */
  private final int sequenceBits;

  /** 最大可用workerId值，workerId取值范围：[0, maxWorkId - 1] */
  private final long maxWorkerId;

  /** 最大可用delta-days值，delta-days取值范围：[0, maxDeltaDays - 1] */
  private final long maxDeltaDays;

  /** 最大可用sequence值，sequence取值范围：[0, maxSequence - 1] */
  private final long maxSequence;

  /** deltaDays左移位数 */
  private final int deltaDaysLeftShiftBits;

  /** workId左移位数 */
  private final int workerIdLeftShiftBits;

  /**
   * 构造方法
   *
   * @param workerIdBits workerId占用比特位数量
   * @param deltaDaysBits 时间差占用比特位数量
   * @param sequenceBits 序列号占用比特位数量
   */
  public LongBitsCalculator(int workerIdBits, int deltaDaysBits, int sequenceBits) {
    if (workerIdBits <= 0) {
      throw new IllegalArgumentException("workerIdBits must be greater than 0.");
    }
    if (deltaDaysBits <= 0) {
      throw new IllegalArgumentException("deltaDaysBits must be greater than 0.");
    }
    if (sequenceBits <= 0) {
      throw new IllegalArgumentException("sequenceBits must be greater than 0.");
    }
    if (workerIdBits + deltaDaysBits + sequenceBits + SIGN_BIT != Long.SIZE) {
      throw new IllegalArgumentException(
          String.format(
              "The equation [workIdBits + deltaDaysBits + sequenceBits == %d] must hold.",
              Long.SIZE - SIGN_BIT));
    }
    this.workerIdBits = workerIdBits;
    this.deltaDaysBits = deltaDaysBits;
    this.sequenceBits = sequenceBits;
    this.maxWorkerId = maxValues(workerIdBits);
    this.maxDeltaDays = maxValues(deltaDaysBits);
    this.maxSequence = maxValues(sequenceBits);
    this.deltaDaysLeftShiftBits = sequenceBits;
    this.workerIdLeftShiftBits = deltaDaysBits + sequenceBits;
  }

  /**
   * 按照比特位计算最大值
   *
   * @param bits 比特位
   * @return 最大值
   */
  public long maxValues(int bits) {
    assert bits > 0 : "Invalid bits: " + bits;
    return ~(-1L << bits);
  }

  /**
   * 组装ID
   *
   * @param workerId worker id
   * @param deltaDays 时间差
   * @param sequence 序列号
   * @return id
   */
  public long combine(long workerId, long deltaDays, long sequence) {
    assert workerId >= 0 && deltaDays >= 0 && sequence >= 0
        : String.format(
            "Invalid params: [workerId=%d, deltaDays=%d, sequence=%d]",
            workerId, deltaDays, sequence);
    return (workerId << workerIdLeftShiftBits) | (deltaDays << deltaDaysLeftShiftBits) | sequence;
  }
}
