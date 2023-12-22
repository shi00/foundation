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

/**
 * rocksdb 内部table使用的索引类型
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-12-22 9:30
 */
public enum IndexType {
  // A space efficient index block that is optimized for
  // binary-search-based index.
  K_BINARY_SEARCH,

  // The hash index, if enabled, will do the hash lookup when
  // `Options.prefix_extractor` is provided.
  K_HASH_SEARCH,

  // A two-level index implementation. Both levels are binary search indexes.
  // Second level index blocks ("partitions") use block cache even when
  // cache_index_and_filter_blocks=false.
  K_TWO_LEVEL_INDEX_SEARCH,

  // Like kBinarySearch, but index also contains first key of each block.
  // This allows iterators to defer reading the block until it's actually
  // needed. May significantly reduce read amplification of short range scans.
  // Without it, iterator seek usually reads one block from each level-0 file
  // and from each level, which may be expensive.
  // Works best in combination with:
  //  - IndexShorteningMode::kNoShortening,
  //  - custom FlushBlockPolicy to cut blocks at some meaningful boundaries,
  //    e.g. when prefix changes.
  // Makes the index significantly bigger (2x or more), especially when keys
  // are long.
  K_BINARY_SEARCH_WITH_FIRST_KEY
}
