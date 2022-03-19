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
package com.silong.foundation.cjob;

/**
 * 复杂任务接口
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-03-02 18:07
 */
public interface ComplexTask extends Runnable {

  /**
   * 任务是否支持并行，默认：false
   *
   * @return 任务是否支持并行
   */
  default boolean parallelizable() {
    return false;
  }

  /**
   * 启动任务
   *
   * @throws Exception 异常
   */
  void start() throws Exception;

  /**
   * 任务回滚<br>
   * 如果工作执行过程中的某任务执行失败后，需要整个工作执行回滚操作，每个已执行成功的任务都需要执行回滚
   *
   * @throws Exception 异常
   */
  void rollback() throws Exception;

  /**
   * 任务取消执行
   *
   * @throws Exception 异常
   */
  void cancel() throws Exception;

  /**
   * 任务暂停执行
   *
   * @throws Exception 异常
   */
  void pause() throws Exception;

  /**
   * 任务从暂停状态恢复
   *
   * @throws Exception 异常
   */
  void resume() throws Exception;
}
