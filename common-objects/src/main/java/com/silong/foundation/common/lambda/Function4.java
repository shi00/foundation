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

/**
 * Represents a function that accepts four arguments and produces a result. This is the four-arity
 * specialization of Function.
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-012 16:23
 * @param <T1> the type of the first argument to the function
 * @param <T2> the type of the second argument to the function
 * @param <T3> the type of the third argument to the function
 * @param <T4> the type of the fourth argument to the function
 * @param <R> the type of the result of the function
 */
public interface Function4<T1, T2, T3, T4, R> {
  /**
   * apply
   *
   * @param t1 param1
   * @param t2 param2
   * @param t3 param3
   * @param t4 param4
   * @return result
   */
  R apply(T1 t1, T2 t2, T3 t3, T4 t4);
}
