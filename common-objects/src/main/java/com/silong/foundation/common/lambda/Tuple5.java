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

import lombok.*;
import lombok.experimental.Accessors;

/**
 * 五元组
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-03 20:20
 * @param <T1> 五元组类型
 * @param <T2> 五元组类型
 * @param <T3> 五元组类型
 * @param <T4> 五元组类型
 * @param <T5> 五元组类型
 */
@Getter
@Setter
@ToString(callSuper = true)
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
public class Tuple5<T1, T2, T3, T4, T5> extends Tuple4<T1, T2, T3, T4> {
  private T5 t5;

  @Builder(builderMethodName = "Tuple5Builder")
  public Tuple5(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
    super(t1, t2, t3, t4);
    this.t5 = t5;
  }
}
