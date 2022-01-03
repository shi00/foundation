package com.silong.fundation.duuid.generator.impl;

import com.silong.fundation.duuid.generator.DuuidGenerator;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Twitter SnowFlake ID
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-03 08:58
 */
@Slf4j
public class TwitterSnowFlakeIdGenerator implements DuuidGenerator {

  /** bits allocations for timeStamp, datacenterId, workerId and sequence */
  private static final long UNUSED_BITS = 1L;

  /**
   * 'time stamp' here is defined as the number of millisecond that have elapsed since the {@link
   * #epoch} given by users on {@link TwitterSnowFlakeIdGenerator} instance initialization
   */
  private static final long TIMESTAMP_BITS = 41L;

  private static final long DATACENTER_ID_BITS = 5L;
  private static final long WORKER_ID_BITS = 5L;
  private static final long SEQUENCE_BITS = 12L;

  /** max values of timeStamp, workerId, datacenterId and sequence */
  private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
  private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
  private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

  /** left shift bits of timeStamp, workerId and datacenterId */
  private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + DATACENTER_ID_BITS + WORKER_ID_BITS;

  private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
  private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

  /*
   * object status variables
   */

  /**
   * reference material of 'time stamp' is '2016-01-01'. its value can't be modified after
   * initialization.
   */
  private final long epoch = 1451606400000L;

  /**
   * data center number the process running on, its value can't be modified after initialization.
   *
   * <p>max: 2^5-1 range: [0,31]
   */
  private final long datacenterId;

  /**
   * machine or process number, its value can't be modified after initialization.
   *
   * <p>max: 2^5-1 range: [0,31]
   */
  private final long workerId;

  /**
   * the unique and incrementing sequence number scoped in only one period/unit (here is ONE
   * millisecond). its value will be increased by 1 in the same specified period and then reset to 0
   * for next period.
   *
   * <p>max: 2^12-1 range: [0,4095]
   */
  private long sequence = 0L;

  /** the time stamp last snowflake ID generated */
  private long lastTimestamp = -1L;

  /**
   * generate an unique and incrementing id
   *
   * @return id
   */
  @Override
  public synchronized long nextId() {
    long currTimestamp = timestampGen();

    if (currTimestamp < lastTimestamp) {
      throw new IllegalStateException(
          String.format(
              "Clock moved backwards. Refusing to generate id for %d milliseconds",
              lastTimestamp - currTimestamp));
    }

    if (currTimestamp == lastTimestamp) {
      sequence = (sequence + 1) & MAX_SEQUENCE;
      // overflow: greater than max sequence
      if (sequence == 0) {
        currTimestamp = waitNextMillis(currTimestamp);
      }

    } else { // reset to 0 for next period/millisecond
      sequence = 0L;
    }

    // track and memo the time stamp last snowflake ID generated
    lastTimestamp = currTimestamp;

    return ((currTimestamp - epoch) << TIMESTAMP_SHIFT)
        | //
        (datacenterId << DATACENTER_ID_SHIFT)
        | //
        (workerId << WORKER_ID_SHIFT)
        | // new line for nice looking
        sequence;
  }

  /**
   * @param datacenterId data center number the process running on, value range: [0,31]
   * @param workerId machine or process number, value range: [0,31]
   */
  public TwitterSnowFlakeIdGenerator(long datacenterId, long workerId) {
    if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
      throw new IllegalArgumentException(
          String.format(
              "datacenter Id can't be greater than %d or less than 0", MAX_DATACENTER_ID));
    }
    if (workerId > MAX_WORKER_ID || workerId < 0) {
      throw new IllegalArgumentException(
          String.format("worker Id can't be greater than %d or less than 0", MAX_WORKER_ID));
    }

    this.datacenterId = datacenterId;
    this.workerId = workerId;
  }

  /** track the amount of calling {@link #waitNextMillis(long)} method */
  private final AtomicLong waitCount = new AtomicLong(0);

  /** @return the amount of calling {@link #waitNextMillis(long)} method */
  public long getWaitCount() {
    return waitCount.get();
  }

  /**
   * running loop blocking until next millisecond
   *
   * @param currTimestamp current time stamp
   * @return current time stamp in millisecond
   */
  protected long waitNextMillis(long currTimestamp) {
    waitCount.incrementAndGet();
    while (currTimestamp <= lastTimestamp) {
      currTimestamp = timestampGen();
    }
    return currTimestamp;
  }

  /**
   * get current time stamp
   *
   * @return current time stamp in millisecond
   */
  protected long timestampGen() {
    return System.currentTimeMillis();
  }

  /** show settings of Snowflake */
  @Override
  public String toString() {
    return "Snowflake Settings [timestampBits="
        + TIMESTAMP_BITS
        + ", datacenterIdBits="
        + DATACENTER_ID_BITS
        + ", workerIdBits="
        + WORKER_ID_BITS
        + ", sequenceBits="
        + SEQUENCE_BITS
        + ", epoch="
        + epoch
        + ", datacenterId="
        + datacenterId
        + ", workerId="
        + workerId
        + "]";
  }

  public long getEpoch() {
    return this.epoch;
  }

  /**
   * extract time stamp, datacenterId, workerId and sequence number information from the given id
   *
   * @param id a snowflake id generated by this object
   * @return an array containing time stamp, datacenterId, workerId and sequence number
   */
  public long[] parseId(long id) {
    long[] arr = new long[5];
    arr[4] = ((id & diode(UNUSED_BITS, TIMESTAMP_BITS)) >> TIMESTAMP_SHIFT);
    arr[0] = arr[4] + epoch;
    arr[1] = (id & diode(UNUSED_BITS + TIMESTAMP_BITS, DATACENTER_ID_BITS)) >> DATACENTER_ID_SHIFT;
    arr[2] =
        (id & diode(UNUSED_BITS + TIMESTAMP_BITS + DATACENTER_ID_BITS, WORKER_ID_BITS))
            >> WORKER_ID_SHIFT;
    arr[3] =
        (id
            & diode(
                UNUSED_BITS + TIMESTAMP_BITS + DATACENTER_ID_BITS + WORKER_ID_BITS, SEQUENCE_BITS));
    return arr;
  }

  /**
   * extract and display time stamp, datacenterId, workerId and sequence number information from the
   * given id in humanization format
   *
   * @param id snowflake id in Long format
   * @return snowflake id in String format
   */
  public String formatId(long id) {
    long[] arr = parseId(id);
    String tmf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(arr[0]));
    return String.format("%s, #%d, @(%d,%d)", tmf, arr[3], arr[1], arr[2]);
  }

  /**
   * a diode is a long value whose left and right margin are ZERO, while middle bits are ONE in
   * binary string layout. it looks like a diode in shape.
   *
   * @param offset left margin position
   * @param length offset+length is right margin position
   * @return a long value
   */
  private long diode(long offset, long length) {
    int lb = (int) (64 - offset);
    int rb = (int) (64 - (offset + length));
    return (-1L << lb) ^ (-1L << rb);
  }
}
