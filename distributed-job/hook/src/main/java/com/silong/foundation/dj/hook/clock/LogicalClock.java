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

import static com.silong.foundation.dj.hook.clock.HybridLogicalClock.*;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import lombok.*;
import lombok.experimental.Accessors;
import org.jgroups.util.SizeStreamable;

/**
 * 逻辑时钟接口
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-24 11:00
 */
public interface LogicalClock {

  /**
   * 逻辑时间戳，由两部分组成，lt为物理时钟，单位：秒，ct为逻辑时钟，计数器
   *
   * @author louis sin
   * @version 1.0.0
   * @since 2023-11-24 11:05
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Accessors(fluent = true)
  class Timestamp implements Comparable<Timestamp>, SizeStreamable {

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
      out.writeLong(HybridLogicalClock.to(lt, ct));
    }

    @Override
    public void readFrom(DataInput in) throws IOException {
      long t = in.readLong();
      lt = extractLT(t);
      ct = extractCT(t);
    }
  }

  /**
   * 获取时间戳对象
   *
   * @param timestamp 时间戳
   * @return 时间戳对象
   */
  static Timestamp from(long timestamp) {
    return new Timestamp(extractLT(timestamp), extractCT(timestamp));
  }

  /**
   * 时间戳对象转时间戳
   *
   * @param timestamp 时间戳对象
   * @return 时间戳
   */
  static long to(@NonNull Timestamp timestamp) {
    return HybridLogicalClock.to(timestamp.lt(), timestamp.ct());
  }

  /**
   * 比较两个时间戳大小
   *
   * @param ts1 时间戳
   * @param ts2 时间戳
   * @return 相等返回0，ts1>ts2返回1，否则返回-1
   */
  static int compare(long ts1, long ts2) {
    return from(ts1).compareTo(from(ts2));
  }

  /**
   * 比较两个时间戳大小
   *
   * @param ts1 时间戳
   * @param ts2 时间戳
   * @return 相等返回0，ts1>ts2返回1，否则返回-1
   */
  static int compare(long ts1, @NonNull Timestamp ts2) {
    return from(ts1).compareTo(ts2);
  }

  /**
   * 比较两个时间戳大小
   *
   * @param ts1 时间戳
   * @param ts2 时间戳
   * @return 相等返回0，ts1>ts2返回1，否则返回-1
   */
  static int compare(@NonNull Timestamp ts1, @NonNull Timestamp ts2) {
    return ts1.compareTo(ts2);
  }

  /**
   * 在向其他节点发送消息，或者本地发生事件时驱动混合逻辑时钟推进，生成时间戳
   *
   * <p>send or local event
   *
   * @return 时间戳
   */
  long tick();

  /**
   * 接收消息时，根据消息携带的时间戳更新本地时间
   *
   * <p>receive event of message m
   *
   * @param m 接收到的消息所带的HLC时间戳
   * @return 更新后的时间戳
   */
  long update(long m);

  /**
   * 获取当前时间戳，不推进时钟
   *
   * @return 时间戳
   */
  long now();
}
