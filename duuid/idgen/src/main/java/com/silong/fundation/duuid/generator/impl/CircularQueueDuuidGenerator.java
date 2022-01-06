package com.silong.fundation.duuid.generator.impl;

import com.silong.fundation.duuid.generator.DuuidGenerator;
import lombok.extern.slf4j.Slf4j;
import org.jctools.queues.SpmcArrayQueue;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import static com.silong.fundation.duuid.generator.utils.Constants.*;
import static java.util.Calendar.*;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 *
 *
 * <pre>
 * Duuid是趋势递增的uuid，可以用于分布式系统中各种需要uuid的场景。
 * Duuid是基于推特开源的SnowFlake ID生成算法改进而来，
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
 * 当前自动分配WorkId是通过集中节点(Redis，ETCD，Mysql等，可自行通过SPI机制扩展)，
 * 每次都分配一个单调递增的不重复workerId，确保每次启动的Duuid生成器都有一个唯一的递增ID，
 * 解决时钟回拨问题。
 * 如果当天的3000多万sequence被用完，可以重新申请一个新的WorkerId后Sequence重置开始分配新Id，
 * 以此类推，由此可以彻底解决 snowflake ID算法时钟回拨问题。
 * 按照每个Duuid生成器每天使用100个workerId，当前WorkId取值范围可以确保使用228年。
 *
 * 3. delta-days占据15bit，表示Duuid启动时刻与2020-01-01 00:00:00之间的天数差，
 * 取值范围：[0, 32767]，可确保Duuid生成器可以以每天33554432个ID的速度使用到2100年，
 * duuid生成器服务启动时刻的时间点会作为日期基准，按日历更新此字段时间差，在运行过程中不受操作系统时间变更影响。
 *
 * 4. 最后是25bit的序列号，取值范围：[0， 33554431]
 *
 * 性能：
 * 在彻底解决SnowFlake ID固有问题的前提下，还通过引入JCtools提供的高性能
 * 无锁环状队列(SpmcArrayQueue)，避免id生成使用同步代码块，大幅提升性能，
 * 实现更高的吞吐率。
 *
 * 安全性：
 * 为了避免在某些场景下对ID连续性可能带来的安全影响，可以通过配置随机增量确保
 * 生成id不连续，增强安全性。
 *
 * Duuid生成器支持定制各分段占用比特位，用户可以按需调整。
 *
 * </pre>
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2021-12-28 22:30
 */
@Slf4j
public class CircularQueueDuuidGenerator extends Thread implements DuuidGenerator {

  private static final String DUUID_PRODUCER_THREAD_PREFIX = "DUuid-Producer";

  /** 是否开启序号随机，避免生成id连续可能引起的潜在安全问题 */
  protected final boolean enableSequenceRandom;

  /** 单生产者，多消费者环状队列 */
  protected final SpmcArrayQueue<Long> queue;

  /** workerId占用比特位数 */
  protected final int workerIdBits;

  /** deltaDays占用比特位数 */
  protected final int deltaDaysBits;

  /** 序号占用比特位数 */
  protected final int sequenceBits;

  /** 最大可用workerId值，workerId取值范围：[0, maxWorkId - 1] */
  protected final long maxWorkerId;

  /** 最大可用delta-days值，delta-days取值范围：[0, maxDeltaDays - 1] */
  protected final long maxDeltaDays;

  /** 最大可用sequence值，sequence取值范围：[0, maxSequence - 1] */
  protected final long maxSequence;

  /** deltaDays左移位数 */
  protected final int deltaDaysLeftShiftBits;

  /** workId左移位数 */
  protected final int workerIdLeftShiftBits;

  /** 环状队列填充率，即需要保证环状队列内的填充的id和容量的占比 */
  protected final double fillingFactor;

  /** 最大随机上限 */
  protected final int maxRandomIncrement;

  /** worker id提供器 */
  protected final Supplier<Long> workerIdSupplier;

  /** 以服务启动时间为基准，计算明天0点 */
  protected final Calendar tomorrowStartTime;

  /** producer是否运行中 */
  protected volatile boolean isRunning;

  /** workerId */
  protected long workerId;

  /** 与基准时间的时差 */
  protected long deltaDays;

  /** 序列号 */
  protected long sequence;

  /**
   * 构造方法
   *
   * @param workerIdSupplier workerId提供者
   * @param enableSequenceRandom 是否随机id增量
   */
  public CircularQueueDuuidGenerator(
      Supplier<Long> workerIdSupplier, boolean enableSequenceRandom) {
    this(
        workerIdSupplier,
        0,
        DEFAULT_QUEUE_CAPACITY,
        DEFAULT_FILLING_FACTOR,
        enableSequenceRandom,
        DEFAULT_MAX_RANDOM_INCREMENT);
  }

  /**
   * 构造方法
   *
   * @param workerIdSupplier workerId提供者
   * @param initialSequence 初始序列号
   * @param queueCapacity 队列容量
   * @param fillingFactor 队列填充率
   * @param enableSequenceRandom 是否随机id增量
   * @param maxRandomIncrement 序列号随机增量最大值
   */
  public CircularQueueDuuidGenerator(
      Supplier<Long> workerIdSupplier,
      long initialSequence,
      int queueCapacity,
      double fillingFactor,
      boolean enableSequenceRandom,
      int maxRandomIncrement) {
    this(
        DEFAULT_WORK_ID_BITS,
        DEFAULT_DELTA_DAYS_BITS,
        DEFAULT_SEQUENCE_BITS,
        workerIdSupplier,
        initialSequence,
        queueCapacity,
        fillingFactor,
        enableSequenceRandom,
        maxRandomIncrement);
  }

  /**
   * 构造方法
   *
   * @param workerIdBits workerId占用比特位数量
   * @param deltaDaysBits 时间差占用比特位数量
   * @param sequenceBits 序列号占用比特位数量
   * @param workerIdSupplier workerId提供器
   * @param sequence 序列号初始值
   * @param queueCapacity 队列容量，必须位2的次方
   * @param fillingFactor 队列填充率，取值：(0, 1.0]
   * @param enableSequenceRandom 是否开启id随机，避免id连续
   * @param maxRandomIncrement 最大随机增量值，必须大于0
   */
  public CircularQueueDuuidGenerator(
      int workerIdBits,
      int deltaDaysBits,
      int sequenceBits,
      Supplier<Long> workerIdSupplier,
      long sequence,
      int queueCapacity,
      double fillingFactor,
      boolean enableSequenceRandom,
      int maxRandomIncrement) {
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
    if (fillingFactor <= 0 || fillingFactor > 1.0) {
      throw new IllegalArgumentException("fillingFactor value range (0, 1.0].");
    }
    // 如果开启随机增量则校验
    if (enableSequenceRandom && maxRandomIncrement <= 0) {
      throw new IllegalArgumentException("maxRandomIncrement must be greater than 0.");
    }
    if (workerIdSupplier == null) {
      throw new IllegalArgumentException("workerIdSupplier must not be null.");
    }

    this.enableSequenceRandom = enableSequenceRandom;
    this.maxRandomIncrement = maxRandomIncrement;
    this.queue = new SpmcArrayQueue<>(queueCapacity);
    this.workerIdBits = workerIdBits;
    this.deltaDaysBits = deltaDaysBits;
    this.sequenceBits = sequenceBits;
    this.maxWorkerId = maxValues(workerIdBits);
    this.maxDeltaDays = maxValues(deltaDaysBits);
    this.maxSequence = maxValues(sequenceBits);
    this.deltaDaysLeftShiftBits = sequenceBits;
    this.workerIdLeftShiftBits = deltaDaysBits + sequenceBits;
    this.workerIdSupplier = workerIdSupplier;
    this.workerId = workerIdSupplier.get();
    this.deltaDays = DAYS.convert(System.currentTimeMillis(), MILLISECONDS) - EPOCH;

    // 计算从服务启动时间到明天0点时间差
    this.tomorrowStartTime = calculateTomorrowStartTime();

    if (workerId < 0 || workerId > maxWorkerId) {
      throw new IllegalArgumentException(
          String.format("workerId value range [0, %d]", maxWorkerId));
    }

    if (deltaDays < 0 || deltaDays > maxDeltaDays) {
      throw new IllegalArgumentException(
          String.format("deltaDays value range [0, %d]", maxDeltaDays));
    }

    if (sequence < 0 || sequence > maxSequence) {
      throw new IllegalArgumentException(
          String.format("sequence value range [0, %d]", maxSequence));
    }

    this.sequence = sequence;
    this.fillingFactor = fillingFactor;

    // 启动生产者线程
    this.isRunning = true;
    setName(DUUID_PRODUCER_THREAD_PREFIX);
    start();
  }

  private Calendar calculateTomorrowStartTime() {
    Calendar utc = getInstance(TimeZone.getTimeZone("UTC"));
    utc.add(DAY_OF_YEAR, 1);
    utc.set(MINUTE, 0);
    utc.set(SECOND, 0);
    utc.set(HOUR_OF_DAY, 0);
    return utc;
  }

  /** id生产 */
  @Override
  public void run() {
    log.info("Thread {} started successfully.", getName());
    queue.fill(
        this::generate,
        idleCounter -> {
          while (((queue.size() * 1.0) / queue.capacity()) > fillingFactor) {
            try {
              MILLISECONDS.sleep(0);
            } catch (InterruptedException e) {
              // never happen in regular
              log.error("Thread {} is interrupted and ends running", getName());
            }
          }
          return idleCounter + 1;
        },
        () -> isRunning);
    log.info("Thread {} runs to the end.", getName());
  }

  /** 停止运行 */
  public void finish() {
    if (isRunning) {
      this.isRunning = false;
      this.queue.clear();
      this.interrupt();
    }
  }

  /**
   * 根据定义的格式生成id，由于是单生产者多消费者模型，所以无需同步<br>
   * 为了避免id连续可能带来的潜在安全问题，此处加入初始值随机开关。<br>
   * 此方法不能抛出异常，否则会导致环状队列奔溃。
   *
   * @return id
   */
  protected long generate() {
    CompletableFuture<Void> completableFuture = null;
    try {
      // 如果当前系统时间大于明天0点，则更新时间差，如果时钟回拨导致时间出现偏差则不变更时间差字段
      if (System.currentTimeMillis() >= tomorrowStartTime.getTimeInMillis()) {
        deltaDays++;
        tomorrowStartTime.add(DAY_OF_YEAR, 1);

        // 时间变更异步清除当前队列中的所有已生成id，过期了
        completableFuture = CompletableFuture.runAsync(queue::clear);
      }

      sequence = sequence + randomIncrement();
      // 如果序列号耗尽，则变更workerId
      if (sequence > maxSequence) {
        workerId = workerIdSupplier.get();
        sequence = sequence - maxSequence;
      }

      return (workerId << workerIdLeftShiftBits) | (deltaDays << deltaDaysLeftShiftBits) | sequence;
    } finally {
      if (completableFuture != null) {
        completableFuture.join();
      }
    }
  }

  /**
   * 如果开启随机增量，增量值随机范围[1，32]<br>
   * 否则自增值固定为1。
   *
   * @return 随机增量
   */
  protected long randomIncrement() {
    // 随机数生成器，此处考虑性能，无需使用安全随机，只要保证生成id不连续即可
    return enableSequenceRandom ? ThreadLocalRandom.current().nextInt(maxRandomIncrement) + 1 : 1;
  }

  /**
   * 从环状队列中取出id
   *
   * @return duuid
   */
  @Override
  public long nextId() {
    Long id = queue.poll();
    while (id == null) {
      id = queue.poll();
    }
    return id;
  }

  /**
   * 按照比特位计算最大值
   *
   * @param bits 比特位
   * @return 最大值
   */
  protected long maxValues(int bits) {
    if (bits <= 0) {
      throw new IllegalArgumentException("Invalid bits: " + bits);
    }
    return ~(-1L << bits);
  }

  @Override
  public String toString() {
    return String.format(
        "%s[workerIdBits:%d, deltaDaysBits:%d, sequenceBits:%d, maxWorkerId:%d, maxDeltaDays:%d, maxSequence:%d, workerId:%d, deltaDays:%d, sequence:%d]",
        getClass().getSimpleName(),
        workerIdBits,
        deltaDaysBits,
        sequenceBits,
        maxWorkerId,
        maxDeltaDays,
        maxSequence,
        workerId,
        deltaDays,
        sequence);
  }
}
