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

package com.silong.foundation.dj.hook.clock;

import static com.silong.foundation.dj.hook.utils.StampedLocks.tryOptimisticRead;
import static com.silong.foundation.dj.hook.utils.StampedLocks.writeLock;

import java.io.*;
import java.time.Clock;
import java.util.concurrent.locks.StampedLock;
import lombok.*;

/**
 * 混合逻辑时钟实现，参考论文《Logical Physical Clocks and Consistent Snapshots in Globally Distributed Databases》
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-22 21:42
 */
public class HybridLogicalClock implements LogicalClock, Serializable {

  @Serial private static final long serialVersionUID = -340_766_300_112_413_663L;

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

  /** 复位逻辑时钟 */
  public void reset() {
    writeLock(lock, (Runnable) () -> lt = ct = 0);
  }

  @Override
  public long now() {
    return tryOptimisticRead(lock, () -> to(lt, ct));
  }

  static long extractLT(long ts) {
    return (ts & 0XFFFFFFFFFFFF0000L) >> 16;
  }

  static long extractCT(long ts) {
    return ts & 0XFFFFL;
  }

  static long to(long lt, long ct) {
    return ((lt & 0X0000FFFFFFFFFFFFL) << 16) | (ct & 0XFFFFL);
  }

  @Override
  @SneakyThrows
  public long tick() {
    return writeLock(
        lock,
        () -> {
          long tlt = lt;
          lt = Math.max(getPhysicalTime(), tlt);
          if (tlt == lt) {
            ct += 1;
          } else {
            ct = 0;
          }
          return to(lt, ct);
        });
  }

  @Override
  @SneakyThrows
  public long update(long m) {
    return writeLock(
        lock,
        () -> {
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
        });
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
