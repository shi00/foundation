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
package com.silong.foundation.utilities.concurrent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import jdk.internal.vm.annotation.Contended;

/**
 * MCS自旋锁实现
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-07-12 20:12
 */
public class McsSpinLock implements Lock {

  private final AtomicReference<LockNode> tail = new AtomicReference<>(null);

  private final ThreadLocal<LockNode> myNode = ThreadLocal.withInitial(LockNode::new);

  @Override
  public void lock() {
    LockNode qNode = myNode.get();
    LockNode preNode = tail.getAndSet(qNode);
    if (preNode != null) {
      qNode.locked = false;
      preNode.next = qNode;
      // wait until predecessor gives up the lock
      while (!qNode.locked) {
        Thread.onSpinWait();
      }
    }
    qNode.locked = true;
  }

  @Override
  public void unlock() {
    LockNode qNode = myNode.get();
    if (qNode.next == null) {
      // There is no waiting thread
      if (tail.compareAndSet(qNode, null)) {
        // If there is no waiting thread, it will return directly without notification
        return;
      }
      // wait until predecessor fills in its next field
      // Suddenly someone is behind him. Maybe he doesn't know who he is. Here are the people
      // waiting for the follow-up
      while (qNode.next == null) {
        Thread.onSpinWait();
      }
    }

    // If there is a waiting thread behind, the following thread will be notified
    qNode.next.locked = true;
    qNode.next = null;
  }

  @Override
  public Condition newCondition() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void lockInterruptibly() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean tryLock() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean tryLock(long time, TimeUnit unit) {
    throw new UnsupportedOperationException();
  }

  void close() {
    myNode.remove();
  }

  /** 锁节点 */
  @Contended
  static class LockNode {
    /** Is it locked by the thread of qNode */
    volatile boolean locked;

    /** Compared with CLHLock, there is a real next */
    volatile LockNode next;
  }
}
