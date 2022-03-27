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
package com.silong.foundation.springboot.starter.cjob.configure.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Positive;

/**
 * 任务框架配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-03 11:31
 */
@Data
@Validated
@ConfigurationProperties(prefix = "complex-jobs")
@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "只读初始配置")
public class ComplexJobsProperties {

  /** 线程池名 */
  public static final String COMPLEX_JOBS_EXECUTORS = "complex-jobs-executors";

  /** 集群名 */
  public static final String COMPLEX_JOBS_CLUSTER = "complex-jobs-cluster";

  /** 脑裂保护策略名 */
  public static final String PROBABILISTIC_SPLIT_BRAIN_PROTECTION =
      "probabilistic-split-brain-protection";

  /** ping错误检测配置 */
  @Data
  public static class IcmpFailureDetectorConfig {

    /** 是否开启icmp错误检测，默认：false */
    private boolean enabled;

    /**
     * Specifies whether the parallel ping detector is enabled; works separately from the other
     * detectors. Its default value is true.
     */
    private boolean parallelMode = true;

    /**
     * Number of milliseconds until a ping attempt is considered failed if there was no reply. Its
     * default value is 100 milliseconds.
     */
    @Positive private int timeoutMilliseconds = 100;

    /** Maximum number of hops the packets should go through. Its default value is 0. */
    @Min(0)
    @Max(255)
    private int ttl = 0;

    /**
     * Interval, in milliseconds, between each ping attempt. 1000ms (1 sec) is also the minimum
     * interval allowed. Its default value is 1000 milliseconds.
     */
    @Min(1000)
    private int intervalMilliseconds = 1000;

    /**
     * Specifies whether the cluster member fails to start if it is unable to action an ICMP ping
     * command when ICMP is enabled.Its default value is true.
     */
    private boolean failFastOnStartup = true;

    /**
     * Maximum number of ping attempts before the member/node gets suspected by the detector. Its
     * default value is 3.
     */
    @Positive private int maxAttempts = 3;
  }

  /** 线程池配置 */
  @Data
  public static class ExecutorsConfig {
    /** 是否开启统计，默认：false */
    private boolean statisticsEnabled = false;

    /** 线程池中线程数量，默认：部署host可用CPU数量 */
    @Positive private int poolSize = Runtime.getRuntime().availableProcessors();

    /** 任务队列容量，默认：128 */
    @Positive private int capacity = 128;

    /** 任务在集群中的副本数：默认：2 */
    @Positive private int durability = 2;
  }

  /** 集群配置 */
  @Data
  public static class NetworkConfig {
    /** 集群通信监听端口 */
    @Min(0)
    @Max(65535)
    private int port = 5771;

    /** 如果监听端口被占用，是否自增端口，默认：true */
    private boolean portAutoIncrementEnable = true;
  }

  /** 集群icmp错误探测配置 */
  @Valid @NestedConfigurationProperty
  private IcmpFailureDetectorConfig icmpFailureDetector = new IcmpFailureDetectorConfig();

  /** 集群通讯配置 */
  @Valid @NestedConfigurationProperty
  private ComplexJobsProperties.NetworkConfig network = new NetworkConfig();

  /** 线程池配置 */
  @Valid @NestedConfigurationProperty
  private ComplexJobsProperties.ExecutorsConfig executors = new ExecutorsConfig();
}
