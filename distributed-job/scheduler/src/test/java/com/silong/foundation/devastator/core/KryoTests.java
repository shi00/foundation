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

import com.silong.foundation.devastator.model.KvPair;
import com.silong.foundation.devastator.utils.LambdaSerializable;
import com.silong.foundation.devastator.utils.LambdaSerializable.SerializableRunnable;
import com.silong.foundation.devastator.utils.TypeConverter;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.silong.foundation.devastator.utils.TypeConverter.getKryoTypeConverter;
import static java.lang.Long.valueOf;

/**
 * 序列化测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-23 19:22
 */
public class KryoTests {

  @Test
  @DisplayName("kryo-ImmutableList")
  void test1() throws IOException {
    List<String> list =
        IntStream.range(0, 10).mapToObj(i -> RandomStringUtils.random(100)).toList();

    TypeConverter<List<String>, byte[]> typeConverter = getKryoTypeConverter();

    byte[] bytes = typeConverter.to(list);
    List<String> list1 = typeConverter.from(bytes);

    Assertions.assertEquals(list, list1);
  }

  @Test
  @DisplayName("kryo-ArrayList")
  void test2() throws IOException {
    List<Integer> list =
        IntStream.range(0, 11).boxed().collect(Collectors.toCollection(ArrayList::new));

    TypeConverter<List<Integer>, byte[]> typeConverter = getKryoTypeConverter();

    byte[] bytes = typeConverter.to(list);
    List<Integer> list1 = typeConverter.from(bytes);

    Assertions.assertEquals(list, list1);
  }

  @Test
  @DisplayName("kryo-LinkedList")
  void test3() throws IOException {
    List<KvPair<String, Integer>> list =
        IntStream.range(0, 15)
            .mapToObj(i -> new KvPair<>(RandomStringUtils.random(100), i))
            .collect(Collectors.toCollection(LinkedList::new));

    TypeConverter<List<KvPair<String, Integer>>, byte[]> typeConverter = getKryoTypeConverter();

    byte[] bytes = typeConverter.to(list);
    List<KvPair<String, Integer>> list1 = typeConverter.from(bytes);

    Assertions.assertEquals(list, list1);
  }

  @Test
  @DisplayName("kryo-HashMap")
  void test4() throws IOException {
    Map<String, Long> map =
        IntStream.range(0, 15)
            .mapToObj(i -> new AbstractMap.SimpleEntry<>(RandomStringUtils.random(100), i))
            .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, e -> valueOf(e.getValue())));

    TypeConverter<Map<String, Long>, byte[]> typeConverter = getKryoTypeConverter();

    byte[] bytes = typeConverter.to(map);
    Map<String, Long> map1 = typeConverter.from(bytes);
    Assertions.assertEquals(map, map1);
  }

  @Test
  @DisplayName("kryo-ImmutableMap")
  void test5() throws IOException {
    Map<String, List<byte[]>> map =
        IntStream.range(0, 15)
            .mapToObj(
                i ->
                    new AbstractMap.SimpleEntry<>(
                        RandomStringUtils.random(100),
                        IntStream.rangeClosed(0, i)
                            .mapToObj(c -> RandomUtils.nextBytes(100))
                            .toList()))
            .collect(
                Collectors.toUnmodifiableMap(
                    AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

    TypeConverter<Map<String, List<byte[]>>, byte[]> typeConverter = getKryoTypeConverter();

    byte[] bytes = typeConverter.to(map);
    Map<String, List<byte[]>> map1 = typeConverter.from(bytes);
    Assertions.assertEquals(map.size(), map1.size());

    map.forEach(
        (k, v) -> {
          List<byte[]> bytes1 = map1.get(k);

          Assertions.assertEquals(v.size(), bytes1.size());

          for (int i = 0; i < v.size(); i++) {
            Assertions.assertArrayEquals(v.get(i), bytes1.get(i));
          }
        });
  }

  @Test
  @DisplayName("kryo-SerializableRunnable")
  void test6() throws IOException {
    Runnable r =
        new SerializableRunnable() {
          AtomicInteger i = new AtomicInteger(0);

          @Override
          public int hashCode() {
            return i.get();
          }

          @Override
          public void run() {
            i.getAndIncrement();
          }
        };

    TypeConverter<Runnable, byte[]> typeConverter = getKryoTypeConverter();

    byte[] to = typeConverter.to(r);
    r.run();

    Runnable runnable = typeConverter.from(to);

    runnable.run();

    Assertions.assertEquals(r.hashCode(), runnable.hashCode());
  }

  @Test
  @DisplayName("kryo-SerializableCallable")
  void test7() throws Exception {
    Callable<Integer> c =
        new LambdaSerializable.SerializableCallable<>() {
          AtomicInteger i = new AtomicInteger(0);

          @Override
          public Integer call() {
            return i.getAndIncrement();
          }
        };

    TypeConverter<Callable<Integer>, byte[]> typeConverter = getKryoTypeConverter();

    byte[] to = typeConverter.to(c);

    Callable<Integer> cc = typeConverter.from(to);

    Assertions.assertEquals(c.call(), cc.call());
  }
}
