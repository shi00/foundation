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

package com.silong.foundation.rocksdbffm.enu;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 读取数据所属层级
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-12-13 23:05
 */
@Getter
@AllArgsConstructor
public enum ReadTier {
  // data in memtable, block cache, OS cache or storage
  K_READ_ALL_TIER(0),
  // data in memtable or block cache
  K_BLOCK_CACHE_TIER(1),
  // persisted data.  When WAL is disabled, this option
  // will skip data in memtable.
  // Note that this ReadTier currently only supports
  // Get and MultiGet and does not support iterators.
  K_PERSISTED_TIER(2),
  // data in memtable. used for memtable-only iterators.
  K_MEMTABLE_TIER(3);

  final int val;
}
