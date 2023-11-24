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

package com.silong.foundation.dj.hook;

import static com.silong.foundation.dj.hook.clock.LogicalClock.compare;
import static com.silong.foundation.dj.hook.clock.LogicalClock.from;

import com.silong.foundation.dj.hook.clock.HybridLogicalClock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 逻辑时钟测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-24 11:36
 */
public class LogicalClockTests {

  private final HybridLogicalClock clock = new HybridLogicalClock();

  @BeforeEach
  public void init() {
    clock.reset();
  }

  @Test
  public void test1() {
    long tick = clock.tick();
    long now = clock.now();
    Assertions.assertEquals(tick, now);
  }

  @Test
  public void test2() {
    long tick = clock.tick();
    Assertions.assertEquals(0, compare(from(tick).ct(), 0));
  }

  @Test
  public void test3() {
    long tick = clock.tick();
    HybridLogicalClock hybridLogicalClock = new HybridLogicalClock();
    long update = hybridLogicalClock.update(tick);
    Assertions.assertEquals(-1, compare(tick, update));
  }
}
