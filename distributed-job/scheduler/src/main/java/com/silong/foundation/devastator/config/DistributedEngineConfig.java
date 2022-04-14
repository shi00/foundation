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

import lombok.Data;
import lombok.experimental.Accessors;

import static com.silong.foundation.devastator.ClusterDataAllocator.DEFAULT_PARTITION_SIZE;

/**
 * 分布式任务引擎配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-10 08:33
 */
@Data
@Accessors(fluent = true)
public class DistributedEngineConfig {
  /** 默认配置文件名 */
  public static final String DEFAULT_ENGINE_CONFIG_FILE = "default-tcp.xml";

  /** 集群名 */
  private String clusterName;

  /** 实例名 */
  private String instanceName;

  /** classpath配置文件，默认： {@code DistributedEngineConfig.DEFAULT_CONFIG_FILE_NAME} */
  private String configFile = DEFAULT_ENGINE_CONFIG_FILE;

  /** 数据分区数量，默认：1024 */
  private int partitionCount = DEFAULT_PARTITION_SIZE;
}
