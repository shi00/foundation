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
package com.silong.foundation.springboot.starter.cjob;

import com.silong.foundation.springboot.starter.cjob.lifecycle.Lifecycle;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.io.Serializable;

/**
 * job接口
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-03-02 18:07
 * @param <T> job上下文类型
 */
public interface ComplexJob<T extends Serializable> extends Lifecycle {

  /**
   * 获取job上下文
   *
   * @return job上下文
   */
  @NonNull
  T getContext();

  /**
   * 重建job
   *
   * @return 重建的任务
   * @throws Exception 异常
   */
  @NonNull
  ComplexJob<T> rebuild() throws Exception;
}
