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
 * 四元组
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-03 20:20
 * @param <T1> 四元组类型
 * @param <T2> 四元组类型
 * @param <T3> 四元组类型
 * @param <T4> 四元组类型
 */
@Getter
@Setter
@ToString(callSuper = true)
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
public class Tuple4<T1, T2, T3, T4> extends Tuple3<T1, T2, T3> {
  private T4 t4;

  @Builder(builderMethodName = "Tuple4Builder")
  public Tuple4(T1 t1, T2 t2, T3 t3, T4 t4) {
    super(t1, t2, t3);
    this.t4 = t4;
  }
}
