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

package com.silong.foundation.rocksdbffm.config;

import static com.silong.foundation.rocksdbffm.enu.InfoLogLevel.INFO_LEVEL;

import com.silong.foundation.rocksdbffm.enu.InfoLogLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import lombok.Data;

/**
 * rocksdb配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-12-02 8:25
 */
@Data
public class RocksDbConfig implements Serializable {

  @Serial private static final long serialVersionUID = -7_905_257_664_231_390_906L;

  /** 持久化数据保存路径 */
  @NotEmpty
  private String persistDataPath =
      Paths.get(System.getProperty("user.dir"))
          .resolve("target")
          .resolve("rocksdb-data")
          .toFile()
          .getAbsolutePath();

  /** 列族配置 */
  @Valid private List<@NotNull @Valid ColumnFamilyConfig> columnFamilyConfigs;

  /** 日志级别 */
  @NotNull private InfoLogLevel infoLogLevel = INFO_LEVEL;

  /** 块缓存大小，单位：MB，默认：32MB */
  @Positive private int blockCacheSize = 32;

  /** 是否启用数据统计，默认：false */
  private boolean enableStatistics;

  /** 默认列族TTL，单位：秒，在未指定列族TTL时使用，默认：0。当此值小于等于0时表示永不过期 */
  @NotNull private Duration defaultColumnFamilyTTL = Duration.ZERO;

  /** 如果数据库不存在是否创建数据库，默认：true */
  private boolean createIfMissing = true;

  /** 是否自动创建列族，默认：true */
  private boolean createMissingColumnFamilies = true;
}
