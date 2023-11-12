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

import static java.lang.Short.MAX_VALUE;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.silong.foundation.dj.scrapper.config.PersistStorageProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Data;
import lombok.ToString;
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
@Validated
@ConfigurationProperties(prefix = "mixmaster")
public class MixmasterProperties {

  public static final int MAX_PARTITIONS_COUNT = 8192;

  public static final int MIN_PARTITIONS_COUNT = 1;

  /** 鉴权配置 */
  @NotNull @Valid @NestedConfigurationProperty private AuthProperties auth = new AuthProperties();

  /** 持久化存储配置 */
  @NotNull @Valid @NestedConfigurationProperty
  private PersistStorageProperties scrapper = new PersistStorageProperties();

  /** 服务管理平面地址 */
  @NotNull @Valid @NestedConfigurationProperty
  private ServiceAddress managerPlaneAddress = new ServiceAddress();

  /** 服务数据平面地址 */
  @NotNull @Valid @NestedConfigurationProperty
  private ServiceAddress dataPlaneAddress = new ServiceAddress();

  /** 数据副本数量，默认：2 */
  @Positive private int backupNum = 2;

  /** 数据分区数量，为了获得良好性能和数据均匀分布，建议分区说远大于集群节点数量，并且是2的指数值，取值范围[1,8192]。默认：1024 */
  @Max(MAX_PARTITIONS_COUNT)
  @Min(MIN_PARTITIONS_COUNT)
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

  /** 内部事件分发队列长度，取指定值最接近的2次方，默认：32 */
  @Min(1)
  @Max(MAX_VALUE)
  private int eventDispatchQueueSize = 32;

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

    /** token有效期，默认：1天 */
    @NotNull
    @DurationUnit(DAYS)
    private Duration expires = Duration.of(1, DAYS);
  }

  /** 服务地址 */
  @Data
  public static class ServiceAddress {
    /** ip地址 */
    @NotEmpty
    @Pattern(
        regexp =
            "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$")
    private String ipAddress;

    /** 监听端口 */
    @Min(1025)
    @Max(65535)
    private int port;
  }
}
