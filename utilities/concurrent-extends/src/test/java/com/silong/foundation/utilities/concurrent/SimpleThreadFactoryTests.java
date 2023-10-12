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

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 简介
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-07-03 17:17
 */
public class SimpleThreadFactoryTests {
  AtomicInteger count = new AtomicInteger();

  @BeforeEach
  void init() {
    SimpleThreadFactory.reset();
  }

  @Test
  void test1() {
    String prefix = "A-";
    SimpleThreadFactory factory = new SimpleThreadFactory(prefix);
    for (int i = 0; i < 100; i++) {
      Thread t = factory.newThread(() -> System.out.println(count.incrementAndGet()));
      t.start();
      Assertions.assertEquals(prefix + i, t.getName());
    }
  }

  @Test
  void test2() {
    String prefix = "A";
    SimpleThreadFactory factory = new SimpleThreadFactory(prefix);
    for (int i = 0; i < 100; i++) {
      Thread t = factory.newThread(() -> System.out.println(count.incrementAndGet()));
      t.start();
      Assertions.assertEquals(prefix + "-" + i, t.getName());
    }
  }
}
