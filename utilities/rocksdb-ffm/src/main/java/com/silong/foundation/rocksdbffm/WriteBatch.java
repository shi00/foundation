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
import javax.annotation.concurrent.NotThreadSafe;

/**
 * WriteBatch holds a collection of updates to apply atomically to a DB.
 *
 * <p>The updates are applied in the order in which they are added to the WriteBatch. For example,
 * the value of "key" will be "v3" after the following batch is written:
 *
 * <p>batch.put("key", "v1"); batch.remove("key"); batch.put("key", "v2"); batch.put("key", "v3");
 *
 * <p>Multiple threads can invoke const methods on a WriteBatch without external synchronization,
 * but if any of the threads may call a non-const method, all threads accessing the same WriteBatch
 * must use external sync
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-12-08 14:31
 */
@NotThreadSafe
public interface WriteBatch extends AutoCloseable {

  /**
   * 写入kv键值对
   *
   * @param columnFamilyHandle 列族handle
   * @param key key bytes
   * @param keyOffset key offset
   * @param keyLength key length
   * @param value value bytes
   * @param valueOffset value offset
   * @param valueLength value length
   */
  void put(
      MemorySegment columnFamilyHandle,
      byte[] key,
      int keyOffset,
      int keyLength,
      byte[] value,
      int valueOffset,
      int valueLength);

  /**
   * 在指定列族删除key
   *
   * @param columnFamilyHandle 列族handle
   * @param key key bytes
   * @param keyOffset key offset
   * @param keyLength key length
   */
  void delete(MemorySegment columnFamilyHandle, byte[] key, int keyOffset, int keyLength);

  /**
   * 删除指定范围内的key
   *
   * @param columnFamilyHandle 列族handle
   * @param startKey 起始key
   * @param startKeyOffset offset
   * @param startKeyLength length
   * @param endKey 结束key
   * @param endKeyOffset offset
   * @param endKeyLength length
   */
  void deleteRange(
      MemorySegment columnFamilyHandle,
      byte[] startKey,
      int startKeyOffset,
      int startKeyLength,
      byte[] endKey,
      int endKeyOffset,
      int endKeyLength);

  @Override
  void close();
}
