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

import com.google.protobuf.MessageLite;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.function.Supplier;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.jgroups.BaseMessage;
import org.jgroups.Message;
import org.jgroups.MessageFactory;
import org.jgroups.util.ByteArray;

/**
 * protobuf 消息
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-12 8:49
 * @param <T> 消息负载类型
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class PbMessage<T extends MessageLite> extends BaseMessage {
  /** 消息类型 */
  private static final short PB_MSG_TYPE = 679;

  /** pb消息 */
  private T payload;

  /**
   * 注册消息类型
   *
   * @param messageFactory 消息工厂
   */
  public static void register(@NonNull MessageFactory messageFactory) {
    messageFactory.register(PB_MSG_TYPE, () -> PbMessage.builder().build());
  }

  @Override
  public short getType() {
    return PB_MSG_TYPE;
  }

  @Override
  public boolean hasPayload() {
    return payload != null;
  }

  @Override
  public boolean hasArray() {
    return false;
  }

  @Override
  public byte[] getArray() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getOffset() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getLength() {
    return payload.getSerializedSize();
  }

  @Override
  public PbMessage<T> setArray(byte[] b, int offset, int length) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PbMessage<T> setArray(ByteArray buf) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T getObject() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Message setObject(Object obj) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void writePayload(DataOutput out) throws IOException {
    // TODO  待完善
  }

  @Override
  public void readPayload(DataInput in) throws IOException, ClassNotFoundException {}

  @Override
  public Supplier<? extends PbMessage<T>> create() {
    return null;
  }
}