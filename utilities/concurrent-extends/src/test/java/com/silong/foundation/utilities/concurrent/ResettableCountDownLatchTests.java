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
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * 单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-02-28 10:03
 */
public class ResettableCountDownLatchTests {

  @Test
  public void test1() throws InterruptedException {
    ResettableCountDownLatch latch = new ResettableCountDownLatch(1);
    AtomicInteger count = new AtomicInteger(0);
    new Thread(
            () -> {
              count.getAndIncrement();
              latch.countDown();
            })
        .start();

    latch.await();
    Assertions.assertEquals(1, count.get());

    latch.reset();
    new Thread(
            () -> {
              count.getAndIncrement();
              latch.countDown();
            })
        .start();
    latch.await();
    Assertions.assertEquals(2, count.get());
  }

  @Test
  public void test2() throws InterruptedException {
    ResettableCountDownLatch latch = new ResettableCountDownLatch(1);
    AtomicInteger count = new AtomicInteger(0);
    new Thread(
            () -> {
              count.getAndIncrement();
              latch.countDown();
            })
        .start();

    latch.await();
    latch.reset();
    Assertions.assertEquals(1, latch.getCount());
  }

  @Test
  public void test3() throws InterruptedException {
    ResettableCountDownLatch latch = new ResettableCountDownLatch(1);
    AtomicInteger count = new AtomicInteger(0);
    new Thread(count::getAndIncrement).start();
    Assertions.assertFalse(latch.await(3, MILLISECONDS));
    Assertions.assertEquals(1, count.get());
  }
}
