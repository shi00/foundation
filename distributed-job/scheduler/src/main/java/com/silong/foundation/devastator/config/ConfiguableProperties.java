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
package com.silong.foundation.devastator.config;

/**
 * 可配置项集合
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-23 17:19
 */
public interface ConfiguableProperties {

  /** kryo对象池容量，默认：8 */
  int KRYO_POOL_CAPACITY = Integer.parseInt(System.getProperty("kryo.pool.capacity", "8"));

  /** kryo Input对象池容量，默认：16 */
  int KRYO_INPUT_POOL_CAPACITY =
      Integer.parseInt(System.getProperty("kryo.input.pool.capacity", "16"));

  /** kryo Output对象池容量，默认：16 */
  int KRYO_OUTPUT_POOL_CAPACITY =
      Integer.parseInt(System.getProperty("kryo.output.pool.capacity", "16"));

  /** 集群视图堆栈最大容量，默认：8 */
  int CLUSTER_VIEW_STACK_CAPACITY =
      Integer.parseInt(System.getProperty("cluster-view.stack.capacity", "8"));
}
