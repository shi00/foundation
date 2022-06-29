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
package com.silong.foundation.utilities.pool;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * 单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-02-28 10:03
 */
public class SimpleSoftRefObjectPoolTests {

  /** 消耗内存 */
  private static final Map<String, String> GARBAGE_BUCKET = new HashMap<>();

  static final int SIZE = 1024;

  @BeforeAll
  static void init() {
    for (int i = 0; i < 1500; i++) {
      GARBAGE_BUCKET.put(RandomStringUtils.random(SIZE), RandomStringUtils.random(SIZE));
    }
  }

  @Test
  void test1() {
    try (var pool = SimpleObjectPool.buildSoftRefObjectPool(1, TestObj::new)) {
      var obj1 = pool.obtain();
      pool.returns(obj1);
      var obj2 = pool.obtain();
      Assertions.assertSame(obj1, obj2);
    }
  }

  @AfterAll
  static void clean() {
    GARBAGE_BUCKET.clear();
  }
}
