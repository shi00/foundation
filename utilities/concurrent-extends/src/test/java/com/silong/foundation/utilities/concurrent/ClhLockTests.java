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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-07-12 21:02
 */
public class ClhLockTests {

  @Test
  @DisplayName("CLH-Lock")
  void test1() throws InterruptedException {
    AtomicInteger count = new AtomicInteger(0);
    ClhSpinLock lock = new ClhSpinLock();
    int num = 100;
    ResettableCountDownLatch latch = new ResettableCountDownLatch(num);
    ResettableCountDownLatch start = new ResettableCountDownLatch(1);
    for (int i = 0; i < num; i++) {
      new Thread(
              () -> {
                try {
                  start.await();
                } catch (InterruptedException e) {
                  // ignore
                }

                lock.lock();
                try {
                  System.out.printf(
                      "%s: %d%n", Thread.currentThread().getName(), count.getAndIncrement());
                  latch.countDown();
                } finally {
                  lock.unlock();
                }
              })
          .start();
    }
    start.countDown();
    latch.await();
    Assertions.assertEquals(num, count.get());
  }
}
