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

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

/**
 * 单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-02-28 10:03
 */
public class HashedWheelTimerTests {

  @Test
  public void test1() {
    long l1 = System.nanoTime();
    long l = Long.MAX_VALUE - Long.MIN_VALUE;
    System.out.println( Long.MAX_VALUE);
    System.out.println( Long.MIN_VALUE);
    System.out.println( TimeUnit.MILLISECONDS.toDays(9223372036854L)/365);
    System.out.println( -Long.MAX_VALUE);
  }
}
