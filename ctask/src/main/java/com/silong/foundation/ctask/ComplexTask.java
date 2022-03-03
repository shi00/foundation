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
package com.silong.foundation.ctask;

/**
 * 复杂任务接口，定义任务通用操作
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-03-02 18:07
 * @param <T> 任务上下文类型
 */
public interface ComplexTask<T> {

  /**
   * 获取任务上下文
   *
   * @return 上下文
   */
  T getContext();

  /** 任务启动 */
  void start();

  /** 任务取消 */
  void cancel();

  /** 任务暂停 */
  void pause();

  /** 任务暂停恢复 */
  void resume();

  /** 任务重建 */
  void rebuild();
}
