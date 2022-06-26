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

import java.io.Closeable;

/**
 * 延时任务接口
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-06-24 21:53
 */
public interface DelayedTask extends Closeable {

  /** 延时任务状态 */
  enum State {
    /** 就绪，等待执行 */
    READY,

    /** 执行中 */
    RUNNING,

    /** 已取消 */
    CANCELLED,

    /** 任务正常执行完毕 */
    FINISH,

    /** 任务执行期间异常 */
    EXCEPTION
  }

  /**
   * 取消任务执行，只有State.READY状态的任务可以被取消
   *
   * @return true or false
   */
  boolean cancel();

  /**
   * 获取任务执行异常，如果有异常的话<br>
   * 本方法会阻塞至任务执行完毕
   *
   * @return 异常，可能为null
   * @throws InterruptedException 线程被中断
   */
  Exception getException() throws InterruptedException;

  /**
   * 任务执行结果，Runnable任务返回null，Callable任务返回值依赖其自身的业务逻辑<br>
   * 本方法会阻塞至任务执行完毕
   *
   * @return 任务执行结果
   * @param <R> 任务执行结果
   * @throws InterruptedException 线程被中断
   */
  <R> R getResult() throws InterruptedException;

  /**
   * 任务状态
   *
   * @return 任务状态
   */
  State getState();

  /**
   * 任务名
   *
   * @return 任务名
   */
  String getName();
}
