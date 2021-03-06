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

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;

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

  @Serial private static final long serialVersionUID = -7838316722959995558L;

  /** 默认分区数. */
  public static final int DEFAULT_PARTITION_SIZE = 1024;

  /** 最大分区数 */
  public static final int MAX_PARTITIONS_COUNT = 8192;

  /** 最小分区数 */
  public static final int MIN_PARTITIONS_COUNT = 1;

  /** 默认jgroups配置文件名 */
  public static final String DEFAULT_JGTCP_CONFIG_FILE = "default-tcp.xml";

  /** 默认jgroups配置文件名 */
  public static final String DEFAULT_K8S_CONFIG_FILE = "default-k8s.xml";

  /** 默认jgroups配置文件名 */
  public static final String DEFAULT_JGUDP_CONFIG_FILE = "default-udp.xml";

  /** 默认集群名 */
  public static final String DEFAULT_CLUSTER = "default-cluster";

  /** 默认集群视图变化事件处理队列长度 */
  public static final int DEFAULT_VIEW_CHANGED_EVENT_QUEUE_SIZE = 16;

  /** 默认消息事件处理队列长度 */
  public static final int DEFAULT_MESSAGE_EVENT_QUEUE_SIZE = 256;

  /** 消息事件队列数量 */
  public static final int DEFAULT_MESSAGE_EVENT_QUEUE_COUNT = 4;

  /** 默认分区同步线程数量 */
  public static final int DEFAULT_PARTITION_SYNC_THREAD_COUNT = 4;

  /** 实例名，不指定则随机生成一个uuid */
  @NotEmpty private String instanceName = UUID.randomUUID().toString();

  /** 集群名，节点join的集群名，相同集群名配置的节点才能组成一个集群，默认：default-cluster */
  @NotEmpty private String clusterName = DEFAULT_CLUSTER;

  /** jgroups配置文件，支持classpath，url，file默认： {@code DEFAULT_JGUDP_CONFIG_FILE} */
  @NotEmpty private String configFile = DEFAULT_JGUDP_CONFIG_FILE;

  /** 数据分区数量，为了获得良好性能和数据均匀分布，建议分区说远大于集群节点数量，并且是2的指数值，取值范围[1,8192]。默认：1024 */
  @Min(1)
  @Max(MAX_PARTITIONS_COUNT)
  private int partitionCount = DEFAULT_PARTITION_SIZE;

  /** 数据副本数量，默认：1 */
  @Positive private int backupNums = 1;

  /** 集群状态同步超时时间，默认：15000ms */
  @Positive private long clusterStateSyncTimeout = TimeUnit.SECONDS.toMillis(15);

  /** 集群视图变化事件处理队列长度，必须是2的指数。默认：16。 */
  @Positive private int viewChangedEventQueueSize = DEFAULT_VIEW_CHANGED_EVENT_QUEUE_SIZE;

  /** 集群消息处理队列长度，必须是2的指数。默认：256。 */
  @Positive private int messageEventQueueSize = DEFAULT_MESSAGE_EVENT_QUEUE_SIZE;

  /** 消息处理队列数量，默认：4 */
  @Positive private int messageEventQueueCount = DEFAULT_MESSAGE_EVENT_QUEUE_COUNT;

  /** 分区同步线程数量：默认：4 */
  @Positive private int partitionSyncThreadCount = DEFAULT_PARTITION_SYNC_THREAD_COUNT;

  /** 节点属性 */
  @Valid @NotNull
  private Map<@NotEmpty String, @NotEmpty String> clusterNodeAttributes = new LinkedHashMap<>();

  /** 持久化存储配置 */
  @Valid @NotNull private PersistStorageConfig persistStorageConfig = new PersistStorageConfig();

  /** 鉴权配置 */
  @Valid @NotNull private AuthTokenConfig authTokenConfig = new AuthTokenConfig();

  /** 任务调度器配置 */
  @Valid @NotEmpty
  private List<@NotNull @Valid ScheduledExecutorConfig> scheduledExecutorConfigs =
      new LinkedList<>();
}
