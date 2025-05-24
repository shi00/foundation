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
 * IO 活动，rocksdb内部使用
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-12-13 22:37
 */
@Getter
@AllArgsConstructor
public enum IOActivity {
  K_FLUSH(0),
  K_COMPACTION(1),
  K_DB_OPEN(2),
  K_GET(3),
  K_MULTI_GET(4),
  K_DB_ITERATOR(5),
  K_VERIFY_DB_CHECKSUM(6),
  K_VERIFY_FILE_CHECKSUMS(7),
  K_GET_ENTITY(8),
  K_MULTI_GET_ENTITY(9),
  K_UNKNOWN(10); // Keep last for easy array of non-unknowns

  final int val;
}
