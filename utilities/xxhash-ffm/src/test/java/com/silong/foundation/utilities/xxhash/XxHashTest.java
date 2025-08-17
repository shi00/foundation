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
import static org.junit.jupiter.api.Assertions.*;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * xxhash单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-09-20 22:13
 */
public class XxHashTest {

  @ParameterizedTest
  @ValueSource(ints = {8448, 9981, 5521})
  public void testHash32(int length) {
    String str = RandomStringUtils.random(length);
    byte[] bytes = str.getBytes(UTF_8);
    int expected = XxHashGenerator.hash32(bytes);
    int actual = XxHashGenerator.hash32(bytes, 0, bytes.length);
    assertEquals(expected, actual);
  }

  @ParameterizedTest
  @ValueSource(ints = {8448, 9981, 5521})
  public void testHash64(int length) {
    String str = RandomStringUtils.random(length);
    byte[] bytes = str.getBytes(UTF_8);
    long expected = XxHashGenerator.hash64(bytes);
    long actual = XxHashGenerator.hash64(bytes, 0, bytes.length);
    assertEquals(expected, actual);
  }

  @ParameterizedTest
  @ValueSource(ints = {8448, 9981, 5521})
  public void testHash128(int length) {
    String str = RandomStringUtils.random(length);
    byte[] bytes = str.getBytes(UTF_8);
    byte[] expected = XxHashGenerator.hash128(bytes);
    byte[] actual = XxHashGenerator.hash128(bytes, 0, bytes.length);
    assertArrayEquals(expected, actual);
  }

  @ParameterizedTest
  @ValueSource(strings = {"hash32", "hash64", "hash128"})
  public void testEmptyInput(String method) {
    byte[] bytes = new byte[0];
    switch (method) {
      case "hash32":
        assertThrowsExactly(IllegalArgumentException.class, () -> XxHashGenerator.hash32(bytes));
        break;
      case "hash64":
        assertThrowsExactly(IllegalArgumentException.class, () -> XxHashGenerator.hash64(bytes));
        break;
      case "hash128":
        assertThrowsExactly(IllegalArgumentException.class, () -> XxHashGenerator.hash128(bytes));
        break;
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"hash32", "hash64", "hash128"})
  public void testNullInput(String method) {
    switch (method) {
      case "hash32":
        assertThrowsExactly(NullPointerException.class, () -> XxHashGenerator.hash32(null));
        break;
      case "hash64":
        assertThrowsExactly(NullPointerException.class, () -> XxHashGenerator.hash64(null));
        break;
      case "hash128":
        assertThrowsExactly(NullPointerException.class, () -> XxHashGenerator.hash128(null));
        break;
    }
  }
}
