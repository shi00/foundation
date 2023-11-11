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

import com.google.protobuf.ByteString;
import com.silong.foundation.common.utils.Converter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * 属性key
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-11 12:54
 * @param <R> 属性值类型
 */
@Data
@Builder
@AllArgsConstructor
public class AttributionKey<R> {
  /** 属性key */
  private String key;

  /** 类型转换器 */
  private Converter<ByteString, R> converter;
}
