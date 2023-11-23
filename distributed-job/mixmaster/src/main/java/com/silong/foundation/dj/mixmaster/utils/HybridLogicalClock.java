/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package com.silong.foundation.dj.mixmaster.utils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.time.Clock;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.StampedLock;
import lombok.*;
import lombok.experimental.Accessors;
import org.jgroups.util.SizeStreamable;

/**
 * 混合逻辑时钟实现，参考论文《Logical Physical Clocks and Consistent Snapshots in Globally Distributed Databases》
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-22 21:42
 */
public class HybridLogicalClock {

  /** 混合逻辑时钟时间戳 */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Accessors(fluent = true)
  public static class Timestamp implements Comparable<Timestamp>, SizeStreamable {

    /** 物理时钟，单位：毫秒 */
    private long lt;

    /** 逻辑时钟 */
    private long ct;

    @Override
    public int compareTo(@NonNull Timestamp o) {
      int ret;
      return (ret = Long.compare(lt, o.lt)) == 0 ? Long.compare(ct, o.ct) : ret;
    }

    @Override
    public int serializedSize() {
      return Long.BYTES;
    }

    @Override
    public void writeTo(DataOutput out) throws IOException {
      out.writeLong(to(lt, ct));
    }

    @Override
    public void readFrom(DataInput in) throws IOException {
      long t = in.readLong();
      lt = extractLT(t);
      ct = extractCT(t);
    }
  }

  /** 同步锁 */
  private final StampedLock lock = new StampedLock();

  /** 获取pt */
  private final Clock clock;

  /** 物理时钟，取48比特，初始值：0，单位：毫秒 */
  private long lt;

  /** 逻辑时钟，取比特，初始值：0，最大值：65535 */
  private long ct;

  /**
   * 构造方法
   *
   * @param clock 时钟
   */
  public HybridLogicalClock(@NonNull Clock clock) {
    this.clock = clock;
  }

  /** 默认构造方法，使用系统UTC时钟 */
  public HybridLogicalClock() {
    this(Clock.systemUTC());
  }

  /**
   * 比较两个时间戳大小
   *
   * @param ts1 时间戳
   * @param ts2 时间戳
   * @return 相等返回0，ts1>ts2返回1，否则返回-1
   */
  public static int compare(long ts1, long ts2) {
    return from(ts1).compareTo(from(ts2));
  }

  /**
   * 获取当前时间戳，不推进时钟
   *
   * @return 时间戳
   */
  public long now() {
    return tryOptimisticRead(() -> to(lt, ct));
  }

  /**
   * 获取时间戳对象
   *
   * @param timestamp 时间戳
   * @return 时间戳对象
   */
  public static Timestamp from(long timestamp) {
    return new Timestamp(extractLT(timestamp), extractCT(timestamp));
  }

  private static long extractLT(long ts) {
    return (ts & 0XFFFFFFFFFFFF0000L) >> 16;
  }

  private static long extractCT(long ts) {
    return ts & 0XFFFFL;
  }

  /**
   * 时间戳对象转时间戳
   *
   * @param timestamp 时间戳对象
   * @return 时间戳
   */
  public static long to(@NonNull Timestamp timestamp) {
    return to(timestamp.lt, timestamp.ct);
  }

  private static long to(long lt, long ct) {
    return ((lt & 0X0000FFFFFFFFFFFFL) << 16) | (ct & 0XFFFFL);
  }

  /**
   * 在向其他节点发送消息，或者本地发生事件时驱动混合逻辑时钟推进，生成时间戳
   *
   * <p>send or local event
   *
   * @return 时间戳
   */
  public long tick() {
    long stamp = lock.writeLock();
    try {
      long tlt = lt;
      lt = Math.max(getPhysicalTime(), tlt);
      if (tlt == lt) {
        ct += 1;
      } else {
        ct = 0;
      }
      return to(lt, ct);
    } finally {
      lock.unlockWrite(stamp);
    }
  }

  /**
   * 接收消息时，根据消息携带的时间戳更新本地时间
   *
   * <p>receive event of message m
   *
   * @param m 接收到的消息所带的HLC时间戳
   * @return 更新后的时间戳
   */
  public long update(long m) {
    long stamp = lock.writeLock();
    try {
      long lm = extractLT(m);
      long cm = extractCT(m);
      long tlt = lt;
      lt = Math.max(tlt, Math.max(lm, getPhysicalTime()));
      if (lt == tlt && lt == lm) {
        ct = Math.max(ct, cm) + 1;
      } else if (lt == tlt) {
        ct += 1;
      } else if (lt == lm) {
        ct = cm + 1;
      } else {
        ct = 0;
      }
      return to(lt, ct);
    } finally {
      lock.unlockWrite(stamp);
    }
  }

  @SneakyThrows
  private <T> T tryOptimisticRead(Callable<T> callable) {
    long stamp = lock.tryOptimisticRead();
    T call = callable.call();
    if (!lock.validate(stamp)) {
      stamp = lock.readLock();
      try {
        call = callable.call();
      } finally {
        lock.unlockRead(stamp);
      }
    }
    return call;
  }

  /**
   * 获取物理时钟时间，单位：毫秒
   *
   * @return 真实时间
   */
  private long getPhysicalTime() {
    return clock.millis();
  }
}
