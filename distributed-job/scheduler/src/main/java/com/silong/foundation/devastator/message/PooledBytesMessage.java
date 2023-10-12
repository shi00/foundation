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
package com.silong.foundation.devastator.message;

import static com.silong.foundation.devastator.message.PooledNioMessage.MESSAGE_POOL_CAPACITY;

import com.esotericsoftware.kryo.util.Pool;
import com.esotericsoftware.kryo.util.Pool.Poolable;
import java.util.Arrays;
import org.jgroups.Address;
import org.jgroups.BytesMessage;
import org.jgroups.MessageFactory;
import org.jgroups.util.ByteArray;

/**
 * 对象池消息
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-05-01 21:30
 */
public class PooledBytesMessage extends BytesMessage implements Poolable {

  /** 消息类型 */
  public static final short MSG_TYPE = 680;

  /** 消息池 */
  private static final Pool<PooledBytesMessage> BYTES_MESSAGE_POOL =
      new Pool<>(true, false, MESSAGE_POOL_CAPACITY) {
        @Override
        protected PooledBytesMessage create() {
          return new PooledBytesMessage();
        }
      };

  /** 默认构造方法 */
  public PooledBytesMessage() {
    release();
  }

  /**
   * 构造方法
   *
   * @param dest 消息目的地址
   */
  public PooledBytesMessage(Address dest) {
    super(dest);
    release();
  }

  /**
   * 构造方法
   *
   * @param dest 消息目的地址
   * @param payload 消息内容
   */
  public PooledBytesMessage(Address dest, byte[] payload) {
    super(dest, payload);
    release();
  }

  /**
   * 构造方法
   *
   * @param dest 消息目的地址
   * @param payload 消息内容
   * @param offset 消息偏移量
   * @param length 消息长度
   */
  public PooledBytesMessage(Address dest, byte[] payload, int offset, int length) {
    super(dest, payload, offset, length);
    release();
  }

  /**
   * 构造方法
   *
   * @param dest 消息目的地址
   * @param payload 消息内容
   */
  public PooledBytesMessage(Address dest, ByteArray payload) {
    super(dest, payload);
    release();
  }

  /**
   * 构造方法
   *
   * @param dest 消息目的地址
   * @param obj 消息对象
   */
  public PooledBytesMessage(Address dest, Object obj) {
    super(dest, obj);
    release();
  }

  private void release() {
    //    onRelease(message -> BYTES_MESSAGE_POOL.free((PooledBytesMessage) message));
  }

  /**
   * 获取对象池内的消息
   *
   * @return 消息
   */
  public static PooledBytesMessage obtain() {
    return BYTES_MESSAGE_POOL.obtain();
  }

  /**
   * 注册消息类型
   *
   * @param messageFactory 消息工厂
   */
  public static void register(MessageFactory messageFactory) {
    if (messageFactory == null) {
      throw new IllegalArgumentException("messageFactory must not be null.");
    }
    messageFactory.register(MSG_TYPE, PooledBytesMessage::new);
  }

  @Override
  public short getType() {
    return MSG_TYPE;
  }

  @Override
  public void reset() {
    if (headers != null) {
      Arrays.fill(headers, null);
    }
    this.transient_flags = 0;
    this.flags = 0;
    this.dest = this.sender = null;
    this.array = null;
    this.length = this.offset = 0;
  }
}
