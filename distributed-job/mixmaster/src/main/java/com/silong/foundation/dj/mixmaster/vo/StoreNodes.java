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

package com.silong.foundation.dj.mixmaster.vo;

import jakarta.annotation.Nullable;
import java.util.Iterator;
import java.util.SequencedCollection;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

/**
 * 数据分区对应的存储节点列表，第一位为primary
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-13 15:23
 * @param <T> 节点类型
 */
@Data
@Builder
public class StoreNodes<T> implements Iterable<T> {

  /** 分区存储的节点列表 */
  @NonNull private SequencedCollection<T> primaryAndBackups;

  /**
   * 返回分区对应的主节点
   *
   * @return 主节点
   */
  @Nullable
  public T primary() {
    return primaryAndBackups.isEmpty() ? null : primaryAndBackups.getFirst();
  }

  @Override
  @NonNull
  public Iterator<T> iterator() {
    return primaryAndBackups.iterator();
  }
}
