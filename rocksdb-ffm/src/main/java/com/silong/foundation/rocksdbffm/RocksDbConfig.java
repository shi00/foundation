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

import static com.silong.foundation.rocksdbffm.RocksDbConfig.DataScale.SMALL;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.nio.file.Paths;
import java.util.Collection;
import lombok.Data;

/**
 * rocksdb配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-12-02 8:25
 */
@Data
public class RocksDbConfig {

  /** 数据规模，持久化存储会依据数据规模优化rocksdb参数 */
  public enum DataScale {
    // 大数据量
    VAST,

    // 中量数据
    MEDIUM,

    // 小数据量
    SMALL
  }

  /** 持久化数据保存路径 */
  @NotEmpty
  private String persistDataPath =
      Paths.get(System.getProperty("user.dir"))
          .resolve("target")
          .resolve("rocksdb-data")
          .toFile()
          .getAbsolutePath();

  /** 列族名列表，不指定则创建default列族 */
  @Valid private Collection<@NotEmpty String> columnFamilyNames;

  /** 持久化存储数据规模，默认：small */
  @NotNull private DataScale dataScale = SMALL;
}
