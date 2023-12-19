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

package com.silong.foundation.rocksdbffm;

import java.lang.foreign.MemorySegment;

/**
 * rocksdb快照相关操作接口
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-12-19 10:15
 */
public interface RocksDbSnapshot {

  /**
   * 创建快照，对应资源需自行释放
   *
   * @return 快照
   */
  MemorySegment createSnapshot();

  /**
   * 释放快照
   *
   * @param snapshot 快照
   */
  void releaseSnapshot(MemorySegment snapshot);
}
