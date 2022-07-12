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
  /** The end of the lock wait queue */
  private final AtomicReference<QNode> tail = new AtomicReference<>(null);

  private final ThreadLocal<QNode> preNode = ThreadLocal.withInitial(() -> null);

  private final ThreadLocal<QNode> myNode = ThreadLocal.withInitial(QNode::new);

  @Override
  public void lock() {
    QNode qnode = myNode.get();
    // Setting your own state to locked=true indicates that you need to obtain a lock
    qnode.locked = true;
    // The tail of the linked list is set to the qNode of the thread, and the previous tail is set
    // to the preNode of the current thread
    QNode pre = tail.getAndSet(qnode);
    preNode.set(pre);
    if (pre != null) {
      // The current thread rotates on the locked field of the precursor node until the precursor
      // node releases the lock resource
      while (pre.locked) {
        Thread.onSpinWait();
      }
    }
  }

  @Override
  public void unlock() {
    QNode qnode = myNode.get();
    // When the lock is released, its own locked is set to false, so that its successor node can end
    // the spin
    qnode.locked = false;
    // Recycle the node and delete it from the virtual queue
    // If the current node reference is set as its own preNode, the next node's preNode will become
    // the current node's preNode, thus removing the current node from the queue
    myNode.set(preNode.get());
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
