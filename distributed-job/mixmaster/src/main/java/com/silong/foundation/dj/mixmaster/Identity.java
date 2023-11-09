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
package com.silong.foundation.dj.mixmaster;

/**
 * 唯一标识接口，提供一个可比较的唯一标识作为集群成员的唯一标识
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-07-02 15:43
 * @param <T> 唯一标识类型
 */
public interface Identity<T extends Comparable<T>> {
  /**
   * 对象唯一标识
   *
   * @return 唯一标识
   */
  T uuid();
}
