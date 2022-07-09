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
package com.silong.foundation.devastator.core;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.silong.foundation.devastator.utils.TypeConverter.String2Bytes.INSTANCE;

/**
 * 单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-07-08 22:28
 */
public class String2BytesTypeConverterTests {
  @Test
  @DisplayName("String-to-bytes")
  void test1() {
    for (int i = 0; i < 10; i++) {
      String str = RandomStringUtils.random(1024);
      byte[] to = INSTANCE.to(str);
      String from = INSTANCE.from(to);
      Assertions.assertEquals(str, from);
    }
  }
}
