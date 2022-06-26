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
package com.silong.foundation.utilities.hwtimer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-02-28 10:03
 */
public class HashedWheelTimerTests {

  @Test
  public void test1() throws Exception {
    HashedWheelTimer timer = new HashedWheelTimer();
    int result = 1;
    try (DelayedTask a = timer.submit("A", () -> result, 0, TimeUnit.HOURS)) {
      int num = (int) a.getResult();
      Assertions.assertEquals(result, num);
    }
    Assertions.assertTrue(timer.stop());
  }

  @Test
  public void test2() throws Exception {
    HashedWheelTimer timer = new HashedWheelTimer();
    AtomicInteger count = new AtomicInteger(0);
    int value = 100;
    CountDownLatch countDownLatch = new CountDownLatch(value);
    for (int i = 0; i < value; i++) {
      String name = "" + i;
      new Thread(
              () -> {
                try (DelayedTask delayedTask =
                    timer.submit(
                        name, (Runnable) count::getAndIncrement, 1, TimeUnit.MILLISECONDS)) {
                  delayedTask.getResult();
                  countDownLatch.countDown();
                } catch (Exception e) {
                  throw new RuntimeException(e);
                }
              })
          .start();
    }

    countDownLatch.await();
    Assertions.assertEquals(value, count.get());
    Assertions.assertTrue(timer.stop());
  }

  @Test
  public void test3() throws Exception {
    HashedWheelTimer timer = new HashedWheelTimer();
    NullPointerException npe = new NullPointerException();
    try (DelayedTask b =
        timer.submit(
            "B",
            () -> {
              throw npe;
            },
            0,
            TimeUnit.DAYS)) {
      Exception exception = b.getException();
      Assertions.assertEquals(npe, exception);
    }
    Assertions.assertTrue(timer.stop());
  }

  @Test
  public void test4() throws Exception {
    HashedWheelTimer timer = new HashedWheelTimer();
    NullPointerException npe = new NullPointerException();
    try (DelayedTask c =
        timer.submit(
            "C",
            () -> {
              throw npe;
            },
            0,
            TimeUnit.DAYS)) {
      Exception expected = npe;
      if (c.cancel()) {
        expected = null;
      }
      Exception exception = c.getException();
      Assertions.assertEquals(expected, exception);
    }
    Assertions.assertTrue(timer.stop());
  }
}
