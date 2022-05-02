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
import org.jgroups.Address;
import org.jgroups.BytesMessage;
import org.jgroups.MessageFactory;
import org.jgroups.Refcountable;
import org.jgroups.util.RefcountImpl;

import static com.silong.foundation.devastator.message.PooledDistributedJobNioMessage.DISTRIBUTED_JOB_MESSAGE_POOL_CAPACITY;
import static org.rocksdb.util.SizeUnit.KB;

/**
 * 对象池分布式任务消息
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-05-01 21:30
 */
public class PooledDistributedJobBytesMessage extends BytesMessage
    implements Refcountable<PooledDistributedJobBytesMessage> {

  /** 消息类型 */
  public static final short MSG_TYPE = 680;

  /** 分布式任务消息池 */
  public static final Pool<PooledDistributedJobBytesMessage> DISTRIBUTED_JOB_MESSAGE_POOL =
      new Pool<>(true, false, DISTRIBUTED_JOB_MESSAGE_POOL_CAPACITY) {
        @Override
        protected PooledDistributedJobBytesMessage create() {
          return new PooledDistributedJobBytesMessage(null, new byte[(int) (8 * KB)]);
        }
      };

  /** 引用计数器 */
  private final RefcountImpl<PooledDistributedJobBytesMessage> refCount = new RefcountImpl<>();

  /** 默认构造方法 */
  public PooledDistributedJobBytesMessage() {
    release();
  }

  /**
   * 构造方法
   *
   * @param dest 消息目的地址
   */
  public PooledDistributedJobBytesMessage(Address dest) {
    super(dest);
    release();
  }

  /**
   * 构造方法
   *
   * @param dest 消息目的地
   * @param payload 分布式任务
   */
  public PooledDistributedJobBytesMessage(Address dest, byte[] payload) {
    super(dest, payload);
    release();
  }

  private void release() {
    refCount.onRelease(DISTRIBUTED_JOB_MESSAGE_POOL::free);
  }

  /**
   * 获取对象池内的消息
   *
   * @return 消息
   */
  public static PooledDistributedJobBytesMessage obtain() {
    return DISTRIBUTED_JOB_MESSAGE_POOL.obtain();
  }

  /**
   * 注册消息类型
   *
   * @param messageFactory 消息工厂
   */
  public static void register(MessageFactory messageFactory) {
    assert messageFactory != null;
    messageFactory.register(MSG_TYPE, PooledDistributedJobBytesMessage::new);
  }

  @Override
  public short getType() {
    return MSG_TYPE;
  }

  /**
   * 获取对象引用计数
   *
   * @return 引用计数
   */
  public synchronized byte getRefCount() {
    return refCount.getRefcount();
  }

  @Override
  public synchronized PooledDistributedJobBytesMessage incr() {
    refCount.incr();
    return this;
  }

  @Override
  public synchronized PooledDistributedJobBytesMessage decr() {
    refCount.decr(this);
    return this;
  }

  @Override
  public String toString() {
    return String.format("%s (refcnt=%d)", super.toString(), getRefCount());
  }
}
