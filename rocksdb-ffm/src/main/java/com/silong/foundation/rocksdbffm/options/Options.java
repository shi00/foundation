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

package com.silong.foundation.rocksdbffm.options;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;

/**
 * Options接口
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-12-13 11:04
 */
public sealed interface Options permits WriteOptions {

  /**
   * 获取Options对应的native memoryLayout
   *
   * @return 内存布局
   */
  MemoryLayout layout();

  /**
   * Rocksdb相关Options转换为java Options
   *
   * @param options options
   * @return this
   */
  Options from(MemorySegment options);

  /**
   * java Options转为Rocksdb native options
   *
   * @return native options
   */
  MemorySegment to();
}
