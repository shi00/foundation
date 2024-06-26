/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.silong.foundation.duuid.generator.impl;

import static com.silong.foundation.duuid.generator.impl.CircularQueueDuuidGenerator.Constants.*;
import static java.util.Calendar.*;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.silong.foundation.duuid.generator.DuuidGenerator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.jctools.queues.SpmcArrayQueue;

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

  /**
   * 常量
   *
   * @author louis sin
   * @version 1.0.0
   * @since 2021-12-31 22:39
   */
  public interface Constants {

    /** 系统时钟提供器 */
    Supplier<Long> SYSTEM_CLOCK_PROVIDER = System::currentTimeMillis;

    /** 默认环状队列容量，为确保队列性能必须为2的次方，默认值：2的13次方 */
    int DEFAULT_QUEUE_CAPACITY = 8192;

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

    /** 默认最大随机增量10 */
    int DEFAULT_MAX_RANDOM_INCREMENT = 10;
  }

  /** 日期格式化 */
  private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  private static final String DUUID_PRODUCER_THREAD_PREFIX = "Duuid-Producer";

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

  /** 最大随机上限 */
  protected final int maxRandomIncrement;

  /** worker id提供器 */
  protected final Supplier<Long> workerIdProvider;

  /** 时间提供者 */
  protected final Supplier<Long> utcTimeProvider;

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
   * @param workerIdProvider workerId提供者
   * @param enableSequenceRandom 是否随机id增量
   */
  public CircularQueueDuuidGenerator(
      Supplier<Long> workerIdProvider, boolean enableSequenceRandom) {
    this(
        workerIdProvider,
        SYSTEM_CLOCK_PROVIDER,
        0,
        DEFAULT_QUEUE_CAPACITY,
        enableSequenceRandom,
        DEFAULT_MAX_RANDOM_INCREMENT);
  }

  /**
   * 构造方法
   *
   * @param workerIdProvider workerId提供者
   * @param utcTimeProvider 时间提供者
   * @param initialSequence 初始序列号
   * @param queueCapacity 队列容量
   * @param enableSequenceRandom 是否随机id增量
   * @param maxRandomIncrement 序列号随机增量最大值
   */
  public CircularQueueDuuidGenerator(
      Supplier<Long> workerIdProvider,
      Supplier<Long> utcTimeProvider,
      long initialSequence,
      int queueCapacity,
      boolean enableSequenceRandom,
      int maxRandomIncrement) {
    this(
        DEFAULT_WORK_ID_BITS,
        DEFAULT_DELTA_DAYS_BITS,
        DEFAULT_SEQUENCE_BITS,
        workerIdProvider,
        utcTimeProvider,
        initialSequence,
        queueCapacity,
        enableSequenceRandom,
        maxRandomIncrement);
  }

  /**
   * 构造方法
   *
   * @param workerIdBits workerId占用比特位数量
   * @param deltaDaysBits 时间差占用比特位数量
   * @param sequenceBits 序列号占用比特位数量
   * @param workerIdProvider workerId提供器
   * @param utcTimeProvider utc时间提供者
   * @param sequence 序列号初始值
   * @param queueCapacity 队列容量，必须位2的次方
   * @param enableSequenceRandom 是否开启id随机，避免id连续
   * @param maxRandomIncrement 最大随机增量值，必须大于0
   */
  public CircularQueueDuuidGenerator(
      int workerIdBits,
      int deltaDaysBits,
      int sequenceBits,
      Supplier<Long> workerIdProvider,
      Supplier<Long> utcTimeProvider,
      long sequence,
      int queueCapacity,
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
    // 如果开启随机增量则校验
    if (enableSequenceRandom && maxRandomIncrement <= 0) {
      throw new IllegalArgumentException("maxRandomIncrement must be greater than 0.");
    }
    if (workerIdProvider == null) {
      throw new IllegalArgumentException("workerIdProvider must not be null.");
    }
    if (utcTimeProvider == null) {
      throw new IllegalArgumentException("utcTimeProvider must not be null.");
    }
    if (!isPowOfTwo(queueCapacity)) {
      throw new IllegalArgumentException("queueCapacity must be a power of 2.");
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
    this.workerIdProvider = workerIdProvider;
    this.utcTimeProvider = utcTimeProvider;
    this.workerId = workerIdProvider.get();
    if (workerId < 0 || workerId > maxWorkerId) {
      throw new IllegalArgumentException(
          String.format("workerId value range [0, %d]", maxWorkerId));
    }

    // 计算从服务启动时间到明天0点时间
    long now = utcTimeProvider.get();
    this.tomorrowStartTime = calculateTomorrowStartTime(now);
    this.deltaDays = DAYS.convert(now, MILLISECONDS) - EPOCH;
    if (deltaDays < 0 || deltaDays > maxDeltaDays) {
      throw new IllegalArgumentException(
          String.format("deltaDays value range [0, %d]", maxDeltaDays));
    }

    this.sequence = sequence;
    if (sequence < 0 || sequence > maxSequence) {
      throw new IllegalArgumentException(
          String.format("sequence value range [0, %d]", maxSequence));
    }

    // 启动生产者线程
    this.isRunning = true;
    setName(DUUID_PRODUCER_THREAD_PREFIX);
    start();
  }

  /** id生产 */
  @Override
  public void run() {
    log.info("Duuid producer thread started successfully.");
    log.info("Initial states: {}", this);
    queue.fill(
        this::generate,
        idleCounter -> {
          Thread.onSpinWait();
          return idleCounter;
        },
        () -> isRunning);
    log.info("Duuid producer thread runs to the end.");
  }

  /** 停止运行 */
  @Override
  public void close() {
    if (isRunning) {
      this.isRunning = false;
      this.queue.clear();
      this.interrupt();
    }
  }

  /**
   * 根据定义的格式生成id，由于是单生产者多消费者模型，所以无需同步<br>
   * 为了避免id连续可能带来的潜在安全问题，此处加入初始值随机开关。<br>
   * 此方法不能抛出异常，否则会导致环状队列崩溃。
   *
   * @return id
   */
  @SuppressFBWarnings(
      value = "STCAL_INVOKE_ON_STATIC_DATE_FORMAT_INSTANCE",
      justification = "SDF是在单线程中使用，不存在并发访问，此处无需修改")
  protected long generate() {
    // 如果当前系统时间大于明天0点，则更新时间差，如果时钟回拨导致时间出现偏差则不变更时间差字段
    if (utcTimeProvider.get() >= tomorrowStartTime.getTimeInMillis()) {
      sequence = 0;
      deltaDays++;
      tomorrowStartTime.add(DAY_OF_YEAR, 1);
      log.info(
          "Time has elapsed to {}, reset sequence to 0. {}",
          SDF.format(tomorrowStartTime.getTime()),
          this);
    }

    sequence = sequence + randomIncrement();

    // 如果序列号耗尽，则重新申请workerId
    if (sequence > maxSequence) {
      workerId = workerIdProvider.get();
      sequence = sequence - maxSequence;
      log.info(
          "sequence has exceeded maximum {}, reassign workerId and reset sequence to {}. {}",
          maxSequence,
          sequence,
          this);
    }

    return (workerId << workerIdLeftShiftBits) | (deltaDays << deltaDaysLeftShiftBits) | sequence;
  }

  /**
   * 如果开启随机增量，增量值随机范围[1，32]<br>
   * 否则自增值固定为1。
   *
   * @return 随机增量
   */
  @SuppressFBWarnings(
      value = "PREDICTABLE_RANDOM",
      justification = "随机数生成器，此处考虑性能，无需使用安全随机，只要保证生成id不连续即可")
  protected long randomIncrement() {
    return enableSequenceRandom ? ThreadLocalRandom.current().nextInt(maxRandomIncrement) + 1 : 1;
  }

  /**
   * 从环状队列中取出id
   *
   * @return duuid
   */
  @Override
  public Long nextId() {
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
      throw new IllegalArgumentException("bits must be greater than 0.");
    }
    return ~(-1L << bits);
  }

  /**
   * 获取以now为基准时间的明天0点日历
   *
   * @param nowUtc utc时间，单位：ms
   * @return 明天0点日历
   */
  protected Calendar calculateTomorrowStartTime(long nowUtc) {
    if (nowUtc <= 0) {
      throw new IllegalArgumentException("nowUtc must be greater than 0.");
    }
    Calendar tomorrowZero = getInstance(TimeZone.getTimeZone("UTC"));
    tomorrowZero.setTimeInMillis(nowUtc);
    tomorrowZero.add(DAY_OF_YEAR, 1);
    tomorrowZero.set(MINUTE, 0);
    tomorrowZero.set(SECOND, 0);
    tomorrowZero.set(HOUR_OF_DAY, 0);
    return tomorrowZero;
  }

  /**
   * 指定整数是否是2的幂
   *
   * @param x 整数
   * @return true or false
   */
  protected boolean isPowOfTwo(int x) {
    return x > 0 & (x & (x - 1)) == 0;
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
