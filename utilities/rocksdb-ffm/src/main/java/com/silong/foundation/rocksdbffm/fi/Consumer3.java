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

package com.silong.foundation.rocksdbffm.fi;

/**
 * consumer with 3 parameters
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-04 20:28
 */
@FunctionalInterface
public interface Consumer3<T1, T2, T3> {
  /**
   * Performs this operation on the given argument.
   *
   * @param t1 the input argument
   * @param t2 the input argument
   * @param t3 the input argument
   */
  void accept(T1 t1, T2 t2, T3 t3);
}
