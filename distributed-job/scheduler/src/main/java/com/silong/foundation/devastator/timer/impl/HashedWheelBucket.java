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
package com.silong.foundation.devastator.timer.impl;

import java.util.LinkedHashSet;

/**
 * 定时轮bucket
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-05-14 08:35
 */
class HashedWheelBucket {
  /** 任务列表 */
  private final LinkedHashSet<HashedTimerTask> hashedTimerTasks = new LinkedHashSet<>();

  /**
   * 添加任务
   *
   * @param hashedTimerTask 定时任务
   */
  public void add(HashedTimerTask hashedTimerTask) {
    assert hashedTimerTask != null;
    hashedTimerTasks.add(hashedTimerTask);
  }
}
