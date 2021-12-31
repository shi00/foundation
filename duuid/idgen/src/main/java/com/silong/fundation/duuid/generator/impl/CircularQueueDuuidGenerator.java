package com.silong.fundation.duuid.generator.impl;

import com.silong.fundation.duuid.generator.DuuidGenerator;
import com.silong.fundation.duuid.generator.producer.DuuidProducer;
import org.jctools.queues.SpmcArrayQueue;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 *
 *
 * <pre>
 * Duuid是趋势递增的uuid，可以用于分布式系统中各种需要uuid的场景。
 * DUuid是基于推特开源的SnowFlake ID生成算法改进而来，
 * 其主要解决SnowFlake ID算法固有的以下两个问题：
 * 1. 时钟回拨导致无法生成id；
 * 2. 需要手动指定DataCenterId和WorkerId，在大规模部署时操作复杂，易错；
 *
 * Duuid解决上述两个问题的方案是重新设计和分配long型ID的比特位，如下：
 * ==========================================
 * |  1  |    23    |     15     |    25    |
 * ==========================================
 *  sign    work-id   delta-days    sequence
 *
 * 1. 最高位符号位1bit，始终为0，代表生成Duuid为正数；
 *
 * 2. workid作为Duuid生成器标识，占据23bit，取值范围：[0， 8388607]，
 * 每次Duuid生成器启动后通过WorkId分配器自动分配一个id，解决手动指定WorkerId问题；
 * 当前自动分配WorkId是通过集中节点(Redis，ETCD，Mysql等)，每次都分配一个单调递增
 * 的不重复Id，确保每次启动的Duuid生成器都有一个唯一的递增ID，解决时钟回拨问题。
 * 按照每个Duuid生成器每天重启100次，当前WorkId取值范围可以确保使用228年。
 *
 * 3. delta-days占据15bit，表示Duuid启动时刻与2020-01-01 00:00:00之间的天数差，
 * 取值范围：[0, 32767]，可确保Duuid生成器可以以每天33554432个ID的速度使用到2100年，
 * 如果当天的3000多wID被用完，可以提前使用明天的ID进行分配，以此类推，由此可以彻底解决
 * snowflake ID算法时钟回拨问题。
 *
 * 4. 最后是25bit的序列号，取值范围：[0， 33554431]
 *
 * 性能：
 * 在彻底解决SnowFlake ID固有问题的前提下，还通过引入JCtools提供的高性能
 * 无锁环状队列(SpmcArrayQueue)，避免id生成使用同步代码块，大幅提升性能，
 * 实现更高的吞吐率。
 *
 * Duuid生成器支持定制各分段占用比特位，用户可以按需调整。
 *
 * </pre>
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2021-12-28 22:30
 */
public class CircularQueueDuuidGenerator implements DuuidGenerator {

  /** 默认环状队列容量，为确保队列性能必须为2的次方 */
  public static final int DEFAULT_QUEUE_CAPACITY = 8192;

  /** 环状队列填充率 */
  public static final double DEFAULT_PADDING_FACTOR = 0.7;

  /** 以2020-01-01 00:00:00为起始时间距1970-01-01 00:00:00的天数差 */
  public static final long EPOCH = 18261;

  /** 最高位为符号位，为了确保生成id为正数，符号位固定为0 */
  public static final int SIGN_BIT = 1;

  /** 节点id占用的bits */
  public static final int DEFAULT_WORK_ID_BITS = 23;

  /** 启动时间与基准时间的差值，单位：天 */
  public static final int DEFAULT_DELTA_DAYS_BITS = 15;

  /** 序号占用bit位 */
  public static final int DEFAULT_SEQUENCE_BITS = 25;

  /** 安全随机数生成器 */
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  /** workId占用比特位数 */
  protected int workIdBits;

  /** deltaDays占用比特位数 */
  protected int deltaDaysBits;

  /** 序号占用比特位数 */
  protected int sequenceBits;

  /** 最大可用workid值，workid取值范围：[0, maxWorkId - 1] */
  protected long maxWorkId;

  /** 最大可用delta-days值，delta-days取值范围：[0, maxDeltaDays - 1] */
  protected long maxDeltaDays;

  /** 最大可用sequence值，sequence取值范围：[0, maxSequence - 1] */
  protected long maxSequence;

  /** deltaDays左移位数 */
  protected int deltaDaysLeftShiftBits;

  /** workId左移位数 */
  protected int workIdLeftShiftBits;

  protected long workId;

  protected long deltaDays;

  protected long sequence;

  /** 是否开启序号随机，避免生成id连续可能引起的潜在安全问题 */
  protected boolean enableSequenceRandom;

  /** 单生产者，多消费者环状队列 */
  protected final SpmcArrayQueue<Long> queue;

  /**
   * 构造方法
   *
   * @param workId workerId编号
   */
  public CircularQueueDuuidGenerator(long workId) {
    this(
        DEFAULT_WORK_ID_BITS,
        DEFAULT_DELTA_DAYS_BITS,
        DEFAULT_SEQUENCE_BITS,
        workId,
        calculateDeltaDays(),
        0,
        DEFAULT_QUEUE_CAPACITY,
        DEFAULT_PADDING_FACTOR,
        false);
  }

  /**
   * 构造方法
   *
   * @param workId workerId编号
   * @param deltaDays 时间差
   * @param sequence 序列号起始值
   */
  public CircularQueueDuuidGenerator(long workId, long deltaDays, long sequence) {
    this(
        DEFAULT_WORK_ID_BITS,
        DEFAULT_DELTA_DAYS_BITS,
        DEFAULT_SEQUENCE_BITS,
        workId,
        deltaDays,
        sequence,
        DEFAULT_QUEUE_CAPACITY,
        DEFAULT_PADDING_FACTOR,
        false);
  }

  /**
   * 构造方法
   *
   * @param workIdBits workId占用比特位数量
   * @param deltaDaysBits 时间差占用比特位数量
   * @param sequenceBits 序列号占用比特位数量
   * @param workerId workerId编号
   * @param deltaDays 时间差
   * @param sequence 序列号初始值
   * @param queueCapacity 队列容量，必须位2的次方
   * @param paddingFactor 队列填充率，取值：(0, 1.0]
   * @param enableSequenceRandom 是否开启id随机，避免id连续
   */
  public CircularQueueDuuidGenerator(
      int workIdBits,
      int deltaDaysBits,
      int sequenceBits,
      long workerId,
      long deltaDays,
      long sequence,
      int queueCapacity,
      double paddingFactor,
      boolean enableSequenceRandom) {
    if (workIdBits + deltaDaysBits + sequenceBits + SIGN_BIT != Long.SIZE) {
      throw new IllegalArgumentException(
          String.format(
              "The equation [workIdBits + deltaDaysBits + sequenceBits == %d] must hold.",
              Long.SIZE - SIGN_BIT));
    }

    this.queue = new SpmcArrayQueue<>(queueCapacity);
    this.workIdBits = workIdBits;
    this.deltaDaysBits = deltaDaysBits;
    this.sequenceBits = sequenceBits;
    this.maxWorkId = maxValues(workIdBits);
    this.maxDeltaDays = maxValues(deltaDaysBits);
    this.maxSequence = maxValues(sequenceBits);
    this.deltaDaysLeftShiftBits = sequenceBits;
    this.workIdLeftShiftBits = deltaDaysBits + sequenceBits;
    this.enableSequenceRandom = enableSequenceRandom;

    if (workerId < 0 || workerId >= maxWorkId) {
      throw new IllegalArgumentException(
          String.format("workerId value range [0, %d]", maxWorkId - 1));
    }

    if (deltaDays < 0 || deltaDays >= maxDeltaDays) {
      throw new IllegalArgumentException(
          String.format("deltaDays value range [0, %d]", maxDeltaDays - 1));
    }

    if (sequence < 0 || sequence >= maxSequence) {
      throw new IllegalArgumentException(
          String.format("sequence value range [0, %d]", maxSequence - 1));
    }

    this.workId = workerId;
    this.deltaDays = deltaDays;
    this.sequence = sequence;

    // 启动生产者线程
    new DuuidProducer(queue, this::generate, paddingFactor);
  }

  /**
   * 根据定义的格式生成id，由于是单生产者多消费者模型，所以无需同步<br>
   * 为了避免id连续可能带来的潜在安全问题，此处加入初始值随机开关。
   *
   * @return duuid
   */
  protected long generate() {
    // 如果序列号耗尽，则预支明天序列号
    sequence = (sequence + randomIncrement()) & maxSequence;
    if (sequence == 0) {
      deltaDays++;
    }
    return (workId << workIdLeftShiftBits) | (deltaDays << deltaDaysLeftShiftBits) | sequence;
  }

  /**
   * 如果开启随机增量，增量值随机范围[1，128]<br>
   * 否则自增值固定为1。
   *
   * @return 随机增量
   */
  protected long randomIncrement() {
    return enableSequenceRandom ? SECURE_RANDOM.nextInt(128) + 1 : 1;
  }

  @Override
  public long nextId() {
    Long id = queue.poll();
    while (id == null) {
      id = queue.poll();
    }
    return id;
  }

  /**
   * 获取当前时间到时间起算点之差
   *
   * @return 时间差
   */
  private static long calculateDeltaDays() {
    return TimeUnit.DAYS.convert(System.currentTimeMillis(), MILLISECONDS) - EPOCH;
  }

  /**
   * 按照比特位计算最大值
   *
   * @param bits 比特位
   * @return 最大值
   */
  private static long maxValues(int bits) {
    if (bits > 0) {
      return ~(1L << bits);
    } else {
      throw new IllegalArgumentException("Invalid bits: " + bits);
    }
  }
}
