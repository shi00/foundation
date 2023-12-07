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

import com.silong.foundation.common.lambda.Tuple2;

/**
 * Defines the interface for an Iterator which provides access to data one entry at a time. Multiple
 * implementations are provided by this library. In particular, iterators are provided to access the
 * contents of a DB and Write Batch.
 *
 * <p>Multiple threads can invoke const methods on an RocksDbIterator without external
 * synchronization, but if any of the threads may call a non-const method, all threads accessing the
 * same RocksDbIterator must use external synchronization.
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-12-06 22:34
 */
public interface RocksDbIterator extends AutoCloseable {

  /**
   * An iterator is either positioned at an entry, or not valid. This method returns true if the
   * iterator is valid.
   *
   * @return true if iterator is valid.
   */
  boolean isValid();

  /**
   * Position at the first entry in the source. The iterator is Valid() after this call if the
   * source is not empty.
   */
  void seekToFirst();

  /**
   * Position at the last entry in the source. The iterator is valid after this call if the source
   * is not empty.
   */
  void seekToLast();

  /**
   * Position at the first entry in the source whose key is at or past target.
   *
   * <p>The iterator is valid after this call if the source contains a key that comes at or past
   * target.
   *
   * @param target byte array describing a key or a key prefix to seek for.
   */
  void seek(byte[] target);

  /**
   * Position at the first entry in the source whose key is that or before target.
   *
   * <p>The iterator is valid after this call if the source contains a key that comes at or before
   * target.
   *
   * @param target byte array describing a key or a key prefix to seek for.
   */
  void seekForPrev(byte[] target);

  /**
   * Moves to the next entry in the source. After this call, Valid() is true if the iterator was not
   * positioned at the last entry in the source.
   *
   * <p>REQUIRES: {@link #isValid()}
   */
  void next();

  /**
   * Moves to the previous entry in the source. After this call, Valid() is true if the iterator was
   * not positioned at the first entry in source.
   *
   * <p>REQUIRES: {@link #isValid()}
   */
  void prev();

  /** 检测当前迭代器状态，如果状态错误则抛出异常 */
  void checkStatus() throws RocksDbException;

  /**
   * 返回iterator当前所在position对应的entry的kv值
   *
   * @return kv
   */
  Tuple2<byte[], byte[]> get();

  /**
   * 返回iterator当前所在position对应的entry的key
   *
   * @return key
   */
  byte[] getKey();

  /**
   * 返回iterator当前所在position对应的entry的value
   *
   * @return value
   */
  byte[] getValue();

  /** free resources */
  @Override
  void close();
}
