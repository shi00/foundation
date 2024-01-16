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

import com.silong.foundation.rocksdbffm.RocksDbComparator;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 列族配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-01-08 14:29
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ColumnFamilyConfig {

  /** 列族名 */
  @NotEmpty private String columnFamilyName;

  /**
   * ttl值为正时表示rocksdb会尽力保证过期(因为过期操作只能在压缩时进行，如果没有压缩操作被执行则无法淘汰过期kv)，
   * 但不能确保，因此可能读取到已过期的KV，不指定列族则创建default列族以及ttl=0,表明default列族中保存的kv永不过期，ttl单位为：秒
   * 注意：如果ttl配置很小，可能导致列族中的数据很快被淘汰
   */
  @NotNull private Duration ttl;

  /** 列族对应的比较器，如果为null则使用rocksdb默认的ByteWiseComparator */
  private Class<? extends RocksDbComparator> comparator;
}
