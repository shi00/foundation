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
package com.silong.foundation.lambda;

import lombok.*;
import lombok.experimental.Accessors;

/**
 * 三元组
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-03 20:20
 * @param <T1> 三元组类型
 * @param <T2> 三元组类型
 * @param <T3> 三元组类型
 */
@Getter
@Setter
@ToString(callSuper = true)
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
public class Tuple3<T1, T2, T3> extends Tuple2<T1, T2> {
  private T3 t3;

  @Builder(builderMethodName = "Tuple3Builder")
  public Tuple3(T1 t1, T2 t2, T3 t3) {
    super(t1, t2);
    this.t3 = t3;
  }
}
