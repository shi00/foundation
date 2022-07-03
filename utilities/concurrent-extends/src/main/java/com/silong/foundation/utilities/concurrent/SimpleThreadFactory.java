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

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 使用给定线程名前缀线程工厂，线程名格式：给定线程名前缀+数字
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-07-03 16:56
 */
public class SimpleThreadFactory implements ThreadFactory {

  /** 数字编号 */
  private static final AtomicInteger NUM = new AtomicInteger(0);

  /** 线程名前缀 */
  private final String prefix;

  /**
   * 构造方法
   *
   * @param prefix 线程名前缀
   */
  public SimpleThreadFactory(String prefix) {
    if (prefix == null || prefix.isEmpty()) {
      throw new IllegalArgumentException("prefix must not be null or empty.");
    }
    this.prefix = trim(prefix.trim()) + "-";
  }

  private String trim(String prefix) {
    return prefix.endsWith("-")
            || prefix.endsWith("_")
            || prefix.endsWith("@")
            || prefix.endsWith("$")
            || prefix.endsWith("|")
        ? prefix.substring(0, prefix.length() - 1)
        : prefix;
  }

  @Override
  public Thread newThread(Runnable r) {
    return new Thread(r, prefix + NUM.getAndIncrement());
  }

  /** 重置线程编号 */
  static void reset() {
    NUM.set(0);
  }
}
