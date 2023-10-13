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

package com.silong.foundation.utilities.xxhash;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

/**
 * xxhash单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-09-20 22:13
 */
public class XxHashTest {

  @Test
  public void test1() {
    int count = 1000;
    do {
      String str = RandomStringUtils.random(1024);
      byte[] bytes = str.getBytes(UTF_8);
      assertEquals(XxHashUtils.hash32(bytes), XxHashUtils.hash32(bytes));
    } while (--count > 0);
  }

  @Test
  public void test2() {
    int count = 1000;
    do {
      String str = RandomStringUtils.random(1024);
      byte[] bytes = str.getBytes(UTF_8);
      assertEquals(XxHashUtils.hash64(bytes), XxHashUtils.hash64(bytes));
    } while (--count > 0);
  }
}
