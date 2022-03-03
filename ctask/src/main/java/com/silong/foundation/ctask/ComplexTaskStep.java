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
 * 复杂任务步骤接口，定义任务通用操作
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-03-02 18:07
 * @param <T> 任务上下文类型
 */
public interface ComplexTaskStep<T> {

  /**
   * 获取步骤执行上下文
   *
   * @return 上下文
   */
  T getContext();

  /**
   * 执行步骤
   *
   * @param context 上下文
   */
  void run(T context) throws Exception;

  /** 步骤取消执行 */
  void cancel() throws Exception;

  /**
   * 步骤回滚<br>
   * 如果任务执行过程中的某步执行失败后，需要整个任务执行回滚操作，每个已执行成功的步骤都需要执行回滚
   */
  void rollback() throws Exception;

  /** 步骤暂停 */
  void pause() throws Exception;

  /** 步骤暂停恢复 */
  void resume() throws Exception;
}
