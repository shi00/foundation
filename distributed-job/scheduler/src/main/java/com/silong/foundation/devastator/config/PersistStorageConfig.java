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
package com.silong.foundation.devastator.config;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Positive;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import static org.rocksdb.util.SizeUnit.MB;

/**
 * Devastator持久化存储配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-15 22:12
 */
@Data
@Accessors(fluent = true)
public class PersistStorageConfig implements Serializable {

  @Serial private static final long serialVersionUID = 0L;

  /** 默认持久化数据保存路径 */
  public static final String DEFAULT_PERSIST_DATA_PATH = "./devastator-data/";

  /** 持久化数据保存路径 */
  @NotEmpty private String persistDataPath = DEFAULT_PERSIST_DATA_PATH;

  /** 列族名列表 */
  @NotEmpty @Valid private List<@NotEmpty String> columnFamilyNames;

  /** memtable memory budget. Default: 32MB */
  @Positive private long memtableMemoryBudget = 32 * MB;

  /** 写入缓存大小，默认：1MB */
  @Positive private long writeBufferSize = MB;

  /** 最大写缓存数量，默认：4 */
  @Positive private int maxWriteBufferNumber = 4;
}
