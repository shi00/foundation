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
package com.silong.foundation.common.lambda;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 二元组
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-03 20:20
 * @param <T1> 二元组类型
 * @param <T2> 二元组类型
 */
@Data
@Accessors(fluent = true)
public class Tuple2<T1, T2> {
  private T1 t1;
  private T2 t2;

  @Builder(builderMethodName = "Tuple2Builder")
  public Tuple2(T1 t1, T2 t2) {
    this.t1 = t1;
    this.t2 = t2;
  }
}
