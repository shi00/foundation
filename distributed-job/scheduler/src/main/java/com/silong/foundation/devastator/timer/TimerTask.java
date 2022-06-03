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
package com.silong.foundation.devastator.timer;

/**
 * 定时任务
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-05-21 08:41
 */
public interface TimerTask extends Runnable {

  /** 任务状态 */
  enum State {
    /** 初始状态 */
    INIT,
    /** 执行中 */
    EXECUTING,
    /** 正常执行完毕 */
    FINISH,
    /** 任务异常结束 */
    EXCEPTION,
    /** 取消状态 */
    CANCELLED
  }

  /**
   * 如果任务异常执行结束，返回任务异常
   *
   * @return 异常，如果任务未执行，或任务正常执行完毕，返回{@code null}
   */
  Throwable getException();

  /**
   * 获取任务状态
   *
   * @return 状态
   */
  State getState();

  /**
   * 取消任务，只有INIT状态的任务可以被取消
   *
   * @return {@code true} or {@code false}
   */
  boolean cancel();
}
