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
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;
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

  /**
   * 列族名与其对应的ttl，ttl值为正时表示rocksdb会尽力保证过期(因为过期操作只能在压缩时进行，如果没有压缩操作被执行则无法淘汰过期kv)，
   * 但不能确保，因此可能读取到已过期的KV，不指定列族则创建default列族以及ttl=0,表明default列族中保存的kv永不过期，ttl单位为：秒
   * 注意：如果ttl配置很小，可能导致列族中的数据很快被淘汰
   */
  @Valid private Map<@NotEmpty String, @NotNull Duration> columnFamilyNameWithTTL;

  /** 持久化存储数据规模，默认：small */
  @NotNull private DataScale dataScale = SMALL;

  /** 默认列族TTL，单位：秒，在未指定列族TTL时使用，默认：0。当此值小于等于0时表示永不过期 */
  private int defaultColumnFamilyTTL = 0;

  /** 如果数据库不存在是否创建数据库，默认：true */
  private boolean createIfMissing = true;

  /** 是否自动创建列族，默认：true */
  private boolean createMissingColumnFamilies = true;
}
