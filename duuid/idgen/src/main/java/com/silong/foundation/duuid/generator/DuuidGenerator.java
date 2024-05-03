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

package com.silong.foundation.duuid.generator;

import java.io.Closeable;

/**
 * 分布式ID生成器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2021-12-28 22:25
 */
public interface DuuidGenerator extends Closeable {

  /** 释放资源 */
  @Override
  default void close() {}

  /**
   * 生成uuid
   *
   * @return 生成id
   */
  Long nextId();
}
