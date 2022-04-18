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

import com.silong.foundation.devastator.ClusterNodeRole;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.silong.foundation.devastator.ClusterDataAllocator.DEFAULT_PARTITION_SIZE;
import static com.silong.foundation.devastator.ClusterDataAllocator.MAX_PARTITIONS_COUNT;
import static com.silong.foundation.devastator.ClusterNodeRole.WORKER;

/**
 * Devastator配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-15 22:12
 */
@Data
@Accessors(fluent = true)
public class DevastatorConfig implements Serializable {

  @Serial private static final long serialVersionUID = 0L;

  /** 默认jgroups配置文件名 */
  public static final String DEFAULT_JG_CONFIG_FILE = "default-tcp.xml";

  /** 实例名，不指定则随机生成一个uuid */
  @NotEmpty private String instanceName = UUID.randomUUID().toString();

  /** 集群名，节点join的集群名，相同集群名配置的节点才能组成一个集群 */
  @NotEmpty private String clusterName;

  /** jgroups配置文件，支持classpath，url，file默认： {@code DEFAULT_JG_CONFIG_FILE} */
  @NotEmpty private String configFile = DEFAULT_JG_CONFIG_FILE;

  /** 数据分区数量，为了获得良好性能和数据均匀分布，建议分区说远大于集群节点数量，并且是2的指数值，取值范围[1,8192]。默认：1024 */
  @Min(1)
  @Max(MAX_PARTITIONS_COUNT)
  private int partitionCount = DEFAULT_PARTITION_SIZE;

  /** 数据副本数量，默认：1 */
  @Positive private int backupNums = 1;

  /** 节点角色，默认: WORKER */
  @NotNull private ClusterNodeRole clusterNodeRole = WORKER;

  /** 节点属性 */
  @Valid @NotNull private Map<@NotEmpty String, @NotEmpty String> clusterNodeAttributes = Map.of();

  /** 持久化存储配置 */
  @Valid @NotEmpty
  private List<@NotNull @Valid PersistStorageConfig> persistStorageConfigs = new LinkedList<>();

  /** 任务调度器配置 */
  @Valid @NotEmpty
  private List<@NotNull @Valid ScheduledExecutorConfig> scheduledExecutorConfigs =
      new LinkedList<>();
}
