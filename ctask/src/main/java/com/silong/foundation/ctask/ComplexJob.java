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

import edu.umd.cs.findbugs.annotations.NonNull;

import java.io.Serializable;

/**
 * 复杂工作任务接口
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-03-02 18:07
 * @param <T> 任务上下文类型
 */
public interface ComplexJob<T extends Serializable> {

  /**
   * 获取工作上下文
   *
   * @return 任务上下文
   */
  @NonNull
  T getContext();

  /**
   * 启动工作
   *
   * @throws Exception 异常
   */
  void start() throws Exception;

  /**
   * 工作取消
   *
   * @throws Exception 异常
   */
  void cancel() throws Exception;

  /**
   * 工作暂停
   *
   * @throws Exception 异常
   */
  void pause() throws Exception;

  /**
   * 暂停工作恢复
   *
   * @throws Exception 异常
   */
  void resume() throws Exception;

  /**
   * 重建工作
   *
   * @throws Exception 异常
   */
  void rebuild() throws Exception;
}
