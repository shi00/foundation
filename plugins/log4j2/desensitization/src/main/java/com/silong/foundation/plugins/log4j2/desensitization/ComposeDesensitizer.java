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
package com.silong.foundation.plugins.log4j2.desensitization;

import com.silong.foundation.model.Tuple3;
import com.silong.foundation.plugins.log4j2.desensitization.Desensitizer;

import java.io.Closeable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 组合脱敏器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-21 22:13
 */
public class ComposeDesensitizer implements Desensitizer, Closeable {

  private static final ThreadLocal<Tuple3<String, Boolean, String>> TUPLE_3_THREAD_LOCAL =
      new ThreadLocal<>();

  /** 脱敏器列表 */
  private final List<Desensitizer> desensitizers;

  /**
   * 构造方法
   *
   * @param desensitizers 脱敏器列表
   */
  public ComposeDesensitizer(Desensitizer... desensitizers) {
    if (desensitizers == null || desensitizers.length == 0) {
      throw new IllegalArgumentException("Requires at least one desensitizer.");
    }
    this.desensitizers = Arrays.stream(desensitizers).collect(Collectors.toList());
  }

  @Override
  public String desensitize(String msg) {
    LinkedList<Tuple3<String, Boolean, String>> list = null;
    do {
      String str = msg;
      list = list == null ? replace(desensitizers.parallelStream(), str) : replace(list, str);
      if (list.isEmpty()) {
        break;
      }
      // 取最长匹配
      msg = list.removeFirst().t1();
    } while (!list.isEmpty());
    return msg;
  }

  private LinkedList<Tuple3<String, Boolean, String>> replace(
      LinkedList<Tuple3<String, Boolean, String>> list, String str) {
    return replace(
        desensitizers.parallelStream()
            .filter(
                desensitizer ->
                    list.stream().anyMatch(t -> Objects.equals(t.t3(), desensitizer.id()))),
        str);
  }

  private LinkedList<Tuple3<String, Boolean, String>> replace(
      Stream<Desensitizer> stream, String str) {
    return stream
        .map(desensitizer -> desensitize(str, desensitizer))
        // 过滤掉未生效的脱敏器
        .filter(e -> !e.t2())
        .sorted(Comparator.comparingInt(e -> e.t1().length()))
        .collect(Collectors.toCollection(LinkedList::new));
  }

  private Tuple3<String, Boolean, String> desensitize(String str, Desensitizer desensitizer) {
    String ret = desensitizer.desensitize(str);
    Tuple3<String, Boolean, String> tuple3 = TUPLE_3_THREAD_LOCAL.get();
    if (tuple3 == null) {
      tuple3 =
          Tuple3.<String, Boolean, String>builder()
              .t1(ret)
              .t2(ret.equals(str))
              .t3(desensitizer.id())
              .build();
      TUPLE_3_THREAD_LOCAL.set(tuple3);
    } else {
      tuple3.t1(ret).t2(ret.equals(str)).t3(desensitizer.id());
    }
    return tuple3;
  }

  /** useless */
  @Override
  public void close() {
    TUPLE_3_THREAD_LOCAL.remove();
  }
}
