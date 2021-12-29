package com.silong.fundation.duuid.impl;

import java.text.ParseException;

/**
 * Long Bits计算器，64比特位进行如下分配：
 *
 * <pre>
 * ==========================================
 * |  1  |    23    |     15     |    25    |
 * ==========================================
 *  sign    work-id   delta-days    sequence
 * </pre>
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2021-12-28 22:32
 */
public class LongBits {

  /** 以2020-01-01 00:00:00为起始时间 */
  private static final int EPOCH = 18261;

  /** 最高位为符号位，为了确保生成id为正数，符号位固定为0 */
  private static final int SIGN_BIT = 1;

  /** 节点id占用的bits */
  private static final int DEFAULT_WORK_ID_BITS = 23;

  /** 启动时间与基准时间的差值，单位：天 */
  private static final int DEFAULT_DELTA_DAYS_BITS = 15;

  /** 序号占用bit位 */
  private static final int DEFAULT_SEQUENCE_BITS = 25;

  /** workId占用比特位数 */
  private final int workIdBits;

  /** deltaDays占用比特位数 */
  private final int deltaDaysBits;

  /** 序号占用比特位数 */
  private final int sequenceBits;

  /** 最大可用workid值，workid取值范围：[0, maxWorkId - 1] */
  private final long maxWorkId;

  /** 最大可用delta-days值，delta-days取值范围：[0, maxDeltaDays - 1] */
  private final long maxDeltaDays;

  /** 最大可用sequence值，sequence取值范围：[0, maxSequence - 1] */
  private final long maxSequence;

  /** deltaDays左移位数 */
  private final int deltaDaysLeftShiftBits;

  /** workId左移位数 */
  private final int workIdLeftShiftBits;

  /**
   * 默认构造方法：<br>
   * workIdBits = 23<br>
   * deltaDaysBits = 15<br>
   * sequenceBits = 25<br>
   */
  public LongBits() {
    this.workIdBits = DEFAULT_WORK_ID_BITS;
    this.deltaDaysBits = DEFAULT_DELTA_DAYS_BITS;
    this.sequenceBits = DEFAULT_SEQUENCE_BITS;
    this.maxWorkId = max(workIdBits);
    this.maxDeltaDays = max(deltaDaysBits);
    this.maxSequence = max(sequenceBits);
    this.deltaDaysLeftShiftBits = this.sequenceBits;
    this.workIdLeftShiftBits = this.deltaDaysBits + this.sequenceBits;
  }

  /**
   * 构造方法，必须确保workIdBits + deltaDaysBits + sequenceBits + 1 == 64
   *
   * @param workIdBits workId占据比特位数量
   * @param deltaDaysBits 当前时间与基准时间差占据比特位数量
   * @param sequenceBits 序号占据比特位数量
   */
  public LongBits(int workIdBits, int deltaDaysBits, int sequenceBits) {
    if (workIdBits + deltaDaysBits + sequenceBits + SIGN_BIT != Long.SIZE) {
      throw new IllegalArgumentException(
          String.format(
              "The equation [workIdBits + deltaDaysBits + sequenceBits == %d] must hold.",
              Long.SIZE - SIGN_BIT));
    }

    this.workIdBits = workIdBits;
    this.deltaDaysBits = deltaDaysBits;
    this.sequenceBits = sequenceBits;
    this.maxWorkId = max(workIdBits);
    this.maxDeltaDays = max(deltaDaysBits);
    this.maxSequence = max(sequenceBits);
    this.deltaDaysLeftShiftBits = this.sequenceBits;
    this.workIdLeftShiftBits = this.deltaDaysBits + this.sequenceBits;
  }

  private static long max(int bits) {
    return ~(1L << bits);
  }


  public static void main(String... args) throws ParseException {

    double pow = Math.pow(2, 15);

    System.out.println(pow);
  }
}
