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

import com.silong.foundation.utilities.concurrent.McsSpinLock.QNode;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * CLH自旋锁实现
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-07-12 20:12
 */
public class ClhSpinLock implements Lock {

  /** Ensure atomic operation */
  private final AtomicReference<QNode> tail = new AtomicReference<>(new QNode());

  /** Node variables of each thread are different */
  private final ThreadLocal<QNode> current = ThreadLocal.withInitial(QNode::new);

  /**
   * Record the front drive node and reuse this node to prevent deadlocks: Assume that there is no
   * use of the node, there are T1, T2 thread, T1 first Lock success, then T2 calls lock () Spin on
   * the Locked field of the T1 node, if the T1 thread unlock () is followed, (T2 has not yet
   * obtained the CPU time film), T1 calls Lock () again, because the value of tail is the node of
   * the T2 thread, which is TRUE, so T1 spin is waiting T2 release the lock, while T2 at this time
   * is still waiting to release the lock, which causes a deadlock.
   */
  private final ThreadLocal<QNode> pred = new ThreadLocal<>();

  public void lock() {
    QNode node = current.get();
    node.locked = true;
    //  Set TAIL to the node of the current thread and get the previous node, this operation is
    // atomic operation
    QNode preNode = tail.getAndSet(node);
    pred.set(preNode);
    //  Busy waiting for the Locked field of the front drive node
    while (preNode.locked) {
      Thread.onSpinWait();
    }
  }

  public void unlock() {
    QNode node = current.get();
    //  Set the Locked property of the current thread node to false so that the next node
    // successfully acquires the lock.
    node.locked = false;
    current.set(pred.get());
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
}
