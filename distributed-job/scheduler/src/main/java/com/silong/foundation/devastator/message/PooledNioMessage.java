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

import com.esotericsoftware.kryo.util.Pool;
import com.esotericsoftware.kryo.util.Pool.Poolable;
import org.jgroups.Address;
import org.jgroups.MessageFactory;
import org.jgroups.RefcountedNioMessage;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * 对象池消息
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-05-01 21:30
 */
public class PooledNioMessage extends RefcountedNioMessage implements Poolable {

  /** 消息类型 */
  public static final short MSG_TYPE = 679;

  /** 消息对象池容量，默认：16 */
  public static final int MESSAGE_POOL_CAPACITY =
      Integer.parseInt(System.getProperty("distributed.engine.message.pool.capacity", "16"));

  /** 分布式任务消息池 */
  public static final Pool<PooledNioMessage> NIO_MESSAGE_POOL =
      new Pool<>(true, false, MESSAGE_POOL_CAPACITY) {
        @Override
        protected PooledNioMessage create() {
          return new PooledNioMessage();
        }
      };

  /** 默认构造方法 */
  public PooledNioMessage() {
    release();
  }

  /**
   * 构造方法
   *
   * @param dest 消息目的地址
   */
  public PooledNioMessage(Address dest) {
    super(dest);
    release();
  }

  /**
   * 构造方法
   *
   * @param dest 消息目的地
   * @param payload 分布式任务
   */
  public PooledNioMessage(Address dest, ByteBuffer payload) {
    super(dest, payload);
    release();
  }

  private void release() {
    onRelease(message -> NIO_MESSAGE_POOL.free((PooledNioMessage) message));
  }

  /**
   * 获取对象池内的消息
   *
   * @return 消息
   */
  public static PooledNioMessage obtain() {
    return NIO_MESSAGE_POOL.obtain();
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
    messageFactory.register(MSG_TYPE, PooledNioMessage::new);
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
    this.use_direct_memory_for_allocations = false;
    this.buf = null;
  }
}
