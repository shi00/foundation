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

package com.silong.foundation.dj.scrapper;

import com.silong.foundation.rocksdbffm.RocksDb;
import com.silong.foundation.rocksdbffm.RocksDbException;
import com.silong.foundation.rocksdbffm.config.RocksDbConfig;
import java.lang.reflect.InvocationTargetException;
import lombok.NonNull;
import lombok.SneakyThrows;

/**
 * KV存储实现
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-01-10 14:29
 */
class KVOpImpl implements KVOperation {

  private final RocksDb rocksDb;

  public KVOpImpl(RocksDbConfig config) {
    rocksDb = RocksDb.getInstance(config);
  }

  @Override
  public <K extends Serializer<K>, V extends Serializer<V>> void put(@NonNull K key, @NonNull V val)
      throws RocksDbException {
    rocksDb.put(key.toBytes(), val.toBytes());
  }

  @Override
  @SneakyThrows({
    NoSuchMethodException.class,
    SecurityException.class,
    InstantiationException.class,
    InvocationTargetException.class,
    IllegalAccessException.class
  })
  public <K extends Serializer<K>, V extends Serializer<V>> V get(
      @NonNull K key, @NonNull Class<V> vClass) throws RocksDbException {
    V value = vClass.getDeclaredConstructor().newInstance();
    return value.fromBytes(rocksDb.get(key.toBytes()));
  }
}
