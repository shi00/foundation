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

package com.silong.foundation.dj.mixmaster.configure.config;

import static java.time.temporal.ChronoUnit.SECONDS;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

/**
 * 服务配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-24 19:01
 */
@Data
@Accessors(fluent = true)
@Validated
@ConfigurationProperties(prefix = "mixmaster")
public class MixmasterProperties {

  /** 鉴权配置 */
  @Valid @NestedConfigurationProperty private AuthProperties auth = new AuthProperties();

  /** 数据副本数量，默认：3 */
  @Positive private int backupNums = 3;

  /** 数据分区数量，为了获得良好性能和数据均匀分布，建议分区说远大于集群节点数量，并且是2的指数值，取值范围[1,8192]。默认：1024 */
  @Max(8192)
  @Min(1)
  private int partitions = 1024;

  /** jgroups配置文件 */
  @NotEmpty private String configFile;

  /** 实例名，不指定则随机生成一个uuid */
  @NotEmpty private String instanceName = UUID.randomUUID().toString();

  /** 集群名，节点join的集群名，相同集群名配置的节点才能组成一个集群，默认：default-cluster */
  @NotEmpty private String clusterName = "default-cluster";

  /** 集群状态同步超时时间，默认：15s */
  @NotNull
  @DurationUnit(SECONDS)
  private Duration clusterStateSyncTimeout = Duration.ofSeconds(15);

  /** 节点属性 */
  @Valid @NotNull
  private Map<@NotEmpty String, @NotEmpty String> clusterNodeAttributes = new LinkedHashMap<>();

  /**
   * 鉴权配置
   *
   * @author louis sin
   * @version 1.0.0
   * @since 2023-10-23 22:30
   */
  @Data
  public static class AuthProperties {
    /** hmac sha256密钥长度必须为32字节 */
    @NotEmpty @ToString.Exclude private String signKey;

    /** 工作密钥 */
    @NotEmpty @ToString.Exclude private String workKey;
  }
}
