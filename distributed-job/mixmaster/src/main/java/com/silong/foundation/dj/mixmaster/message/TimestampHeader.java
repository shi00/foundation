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

package com.silong.foundation.dj.mixmaster.message;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.function.Supplier;
import lombok.*;
import lombok.experimental.Accessors;
import org.jgroups.Header;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.util.Streamable;

/**
 * 逻辑时钟时间戳
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-23 13:51
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimestampHeader extends Header implements Streamable {

  /** 请求头类型 */
  public static final short TYPE = (short) 7712;

  /** 混合逻辑时钟时间戳，HLC */
  @Setter
  @Accessors(fluent = true)
  private long timestamp;

  /** 注册消息头 */
  public static void register() {
    ClassConfigurator.add(TimestampHeader.TYPE, TimestampHeader.class);
  }

  @Override
  public short getMagicId() {
    return TYPE;
  }

  @Override
  public Supplier<? extends Header> create() {
    return TimestampHeader::new;
  }

  @Override
  public int serializedSize() {
    return Long.BYTES;
  }

  @Override
  public void writeTo(DataOutput out) throws IOException {
    out.writeLong(timestamp);
  }

  @Override
  public void readFrom(DataInput in) throws IOException {
    timestamp = in.readLong();
  }
}
