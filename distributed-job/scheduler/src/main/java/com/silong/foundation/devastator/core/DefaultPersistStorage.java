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
package com.silong.foundation.devastator.core;

import com.silong.foundation.devastator.PersistStorage;
import com.silong.foundation.devastator.config.PersistStorageConfig;
import com.silong.foundation.devastator.exception.GeneralException;
import org.rocksdb.*;

import java.io.Serial;

import static org.rocksdb.TxnDBWritePolicy.WRITE_COMMITTED;

/**
 * 基于RocksDB的持久化存储
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-10 17:52
 */
public class DefaultPersistStorage implements PersistStorage {

  @Serial private static final long serialVersionUID = 0L;

  static {
    RocksDB.loadLibrary();
  }

  /** 事件数据库 */
  private final TransactionDB transactionDB;

  /** 数据库事务配置 */
  private final TransactionDBOptions transactionDBOptions;

  /** 数据库配置 */
  private final Options options;

  /**
   * 构造方法
   *
   * @param config 持久化存储配置
   */
  public DefaultPersistStorage(PersistStorageConfig config) {
    if (config == null) {
      throw new IllegalArgumentException("config must not be null.");
    }
    this.options =
        new Options().setCreateIfMissing(true).setCompressionType(CompressionType.ZSTD_COMPRESSION);
    this.transactionDBOptions = new TransactionDBOptions().setWritePolicy(WRITE_COMMITTED);
    try {
      this.transactionDB =
          TransactionDB.open(options, transactionDBOptions, config.persistDataPath());
    } catch (RocksDBException e) {
      throw new GeneralException("Failed to initialize RocksDB.", e);
    }
  }

  public void put(byte[] key, byte[] value) {
    try (WriteOptions writeOptions = new WriteOptions();
        Transaction txn = transactionDB.beginTransaction(writeOptions)) {
      //      transactionDB.put(key,value);
    }
  }

  @Override
  public void close() {
    if (transactionDB != null) {
      transactionDB.close();
    }
    if (transactionDBOptions != null) {
      transactionDBOptions.close();
    }
    if (options != null) {
      options.close();
    }
  }
}
