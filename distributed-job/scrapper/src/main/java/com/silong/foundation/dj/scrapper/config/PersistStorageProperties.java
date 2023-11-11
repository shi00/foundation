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
package com.silong.foundation.dj.scrapper.config;

import static com.silong.foundation.dj.scrapper.PersistStorage.DEFAULT_COLUMN_FAMILY_NAME;
import static org.rocksdb.util.SizeUnit.MB;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import lombok.Data;

/**
 * 持久化存储配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-15 22:12
 */
@Data
public class PersistStorageProperties {

  /** 持久化数据保存路径 */
  @NotEmpty
  private String persistDataPath =
      Paths.get(System.getProperty("user.dir")).resolve("scrapper-data").toFile().getAbsolutePath();

  /** 列族名列表，不指定则只有default列族 */
  @Valid @NotEmpty
  private Collection<@NotEmpty String> columnFamilyNames = List.of(DEFAULT_COLUMN_FAMILY_NAME);

  /** memtable memory budget. Default: 32MB */
  @Positive private long memtableMemoryBudget = 32 * MB;

  /** 列族写入缓存大小，默认：32MB */
  @Positive private long columnFamilyWriteBufferSize = 32 * MB;

  /** db写入缓存大小，默认：128MB */
  @Positive private long dbWriteBufferSize = 128 * MB;

  /** 最大写缓存数量，默认：4 */
  @Positive private int maxWriteBufferNumber = 4;

  /** 最大后台任务数，默认：6 */
  @Positive private int maxBackgroundJobs = 6;

  /**
   * Allows OS to incrementally sync files to disk while they are being written, asynchronously, in
   * the background.Default: 1MB
   */
  @Positive private long bytesPerSync = MB;
}
