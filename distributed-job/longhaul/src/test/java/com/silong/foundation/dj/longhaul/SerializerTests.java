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

package com.silong.foundation.dj.longhaul;

import com.silong.foundation.dj.longhaul.serialize.Int2BytesSerializer;
import com.silong.foundation.dj.longhaul.serialize.Long2BytesSerializer;
import com.silong.foundation.dj.longhaul.serialize.String2BytesSerializer;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 测试用例
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-26 13:48
 */
public class SerializerTests {

  @Test
  void test1() {
    for (int i = 0; i < 10000; i++) {
      int a = RandomUtils.nextInt();
      a = a * (a % 2 == 0 ? 1 : -1);
      byte[] bytes = Int2BytesSerializer.INSTANCE.to(a);
      Integer b = Int2BytesSerializer.INSTANCE.from(bytes);
      Assertions.assertEquals(a, b);
    }
  }

  @Test
  void test2() {
    for (int i = 0; i < 10000; i++) {
      long a = RandomUtils.nextLong();
      a = a * (a % 2 == 0 ? 1 : -1);
      byte[] bytes = Long2BytesSerializer.INSTANCE.to(a);
      Long b = Long2BytesSerializer.INSTANCE.from(bytes);
      Assertions.assertEquals(a, b);
    }
  }

  @Test
  void test3() {
    for (int i = 0; i < 10000; i++) {
      String a = RandomStringUtils.random(RandomUtils.nextInt(0, 1024));
      byte[] bytes = String2BytesSerializer.INSTANCE.to(a);
      String b = String2BytesSerializer.INSTANCE.from(bytes);
      Assertions.assertEquals(a, b);
    }
  }
}
