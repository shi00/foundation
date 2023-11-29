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

package com.silong.foundation.dj.hook.utils;

import java.util.concurrent.Callable;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;
import lombok.NonNull;

/**
 * StampedLock工具类
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-29 21:42
 */
public interface StampedLocks {

  /**
   * 加写锁操作
   *
   * @param lock 锁
   * @param action 加锁后执行动作
   * @return 值
   */
  static void writeLock(@NonNull StampedLock lock, @NonNull Runnable action) {
    long stamp = lock.writeLock();
    try {
      action.run();
    } finally {
      lock.unlockWrite(stamp);
    }
  }

  /**
   * 加写锁操作
   *
   * @param lock 锁
   * @param callable 加锁后执行动作
   * @return 值
   */
  static <T> T writeLock(@NonNull StampedLock lock, @NonNull Callable<T> callable)
      throws Exception {
    long stamp = lock.writeLock();
    try {
      return callable.call();
    } finally {
      lock.unlockWrite(stamp);
    }
  }

  /**
   * 加读锁读取
   *
   * @param lock 锁
   * @param supplier 值供应者
   * @return 值
   * @param <T> 值类型
   */
  static <T> T readLock(@NonNull StampedLock lock, @NonNull Supplier<T> supplier) {
    long stamp = lock.readLock();
    try {
      return supplier.get();
    } finally {
      lock.unlockRead(stamp);
    }
  }

  /**
   * 乐观读
   *
   * @param lock 锁
   * @param supplier 值提供者
   * @return 值
   * @param <T> 值类型
   */
  static <T> T tryOptimisticRead(@NonNull StampedLock lock, @NonNull Supplier<T> supplier) {
    long stamp = lock.tryOptimisticRead();
    T result = supplier.get();
    if (!lock.validate(stamp)) {
      result = readLock(lock, supplier);
    }
    return result;
  }
}
