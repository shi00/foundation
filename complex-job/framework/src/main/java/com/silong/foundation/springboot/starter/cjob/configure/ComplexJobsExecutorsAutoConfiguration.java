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
package com.silong.foundation.springboot.starter.cjob.configure;

import com.hazelcast.cluster.Member;
import com.hazelcast.cluster.MembershipEvent;
import com.hazelcast.cluster.MembershipListener;
import com.hazelcast.config.IcmpFailureDetectorConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.*;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.core.LifecycleEvent;
import com.hazelcast.core.LifecycleListener;
import com.hazelcast.splitbrainprotection.SplitBrainProtectionOn;
import com.silong.foundation.springboot.starter.cjob.configure.config.ComplexJobsProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

import static com.silong.foundation.springboot.starter.cjob.configure.config.ComplexJobsProperties.*;

/**
 * 自动装配
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-03-20 17:02
 */
@Configuration
@EnableConfigurationProperties(ComplexJobsProperties.class)
public class ComplexJobsExecutorsAutoConfiguration {

  private ComplexJobsProperties.NetworkConfig networkConfig;

  private ComplexJobsProperties.ExecutorsConfig executorsConfig;

  private ComplexJobsProperties.IcmpFailureDetectorConfig icmpFailureDetectorConfig;

  private ComplexJobsProperties.ProbabilisticSplitBrainProtectionConfig
      probabilisticSplitBrainProtectionConfig;

  /** 集群成员增减监听器 */
  @Slf4j
  public static class HazelcastCusterMembersListener implements MembershipListener {

    @Override
    public void memberAdded(MembershipEvent membershipEvent) {
      log.info("{}", membershipEvent);
    }

    @Override
    public void memberRemoved(MembershipEvent membershipEvent) {
      log.info("{}", membershipEvent);
    }
  }

  /**
   * 节点生命周期监听器
   */@Slf4j
  public static class NodeLifecycleListener implements LifecycleListener, HazelcastInstanceAware{

     private HazelcastInstance hazelcastInstance;

    @Override
    public void setHazelcastInstance(HazelcastInstance hazelcastInstance) {
      this.hazelcastInstance=hazelcastInstance;
    }

    @Override
    public void stateChanged(LifecycleEvent event) {
      Member localMember = hazelcastInstance.getCluster().getLocalMember();
      switch (event.getState())
      {
        case STARTING -> ;
      }
    }
  }

  /**
   * 如果当前spring上下文中没有hazelcast配置，则注入配置
   *
   * @return hazelcast配置
   */
  @Bean
  @ConditionalOnMissingBean
  Config complexJobsClusterConfig() {
    return new Config()
        .addListenerConfig(new ListenerConfig(new HazelcastCusterMembersListener()))
        .setClusterName(COMPLEX_JOBS_CLUSTER)
        .setInstanceName(String.format("%s:%s", SystemUtils.getHostName(), UUID.randomUUID()));
  }

  /**
   * 集群脑裂保护
   *
   * @param config hazelcast配置
   * @return 脑裂保护配置
   */
  @Bean
  SplitBrainProtectionConfig complexJobsProbabilisticSplitBrainProtectionConfig(Config config) {
    SplitBrainProtectionConfig splitBrainProtectionConfig =
        SplitBrainProtectionConfig.newProbabilisticSplitBrainProtectionConfigBuilder(
                COMPLEX_JOBS_PROBABILISTIC_SPLIT_BRAIN_PROTECTION,
                probabilisticSplitBrainProtectionConfig.getMinimumClusterSize())
            .withMinStdDeviationMillis(
                probabilisticSplitBrainProtectionConfig.getMinStdDeviationMillis())
            .withAcceptableHeartbeatPauseMillis(
                probabilisticSplitBrainProtectionConfig.getAcceptableHeartbeatPauseMillis())
            .withHeartbeatIntervalMillis(
                probabilisticSplitBrainProtectionConfig.getHeartbeatIntervalMillis())
            .withMaxSampleSize(probabilisticSplitBrainProtectionConfig.getMaxSampleSize())
            .withSuspicionThreshold(probabilisticSplitBrainProtectionConfig.getSuspicionThreshold())
            .build();
    splitBrainProtectionConfig.setProtectOn(SplitBrainProtectionOn.READ_WRITE);
    config.addSplitBrainProtectionConfig(splitBrainProtectionConfig);
    return splitBrainProtectionConfig;
  }

  /**
   * 注册集群网络配置
   *
   * @param config 配置
   * @return 集群网络配置
   */
  @Bean
  @ConditionalOnMissingBean
  NetworkConfig complexJobsNetworkConfig(Config config) {
    NetworkConfig networkConfig = config.getNetworkConfig();
    JoinConfig join = networkConfig.getJoin();
    networkConfig
        .setIcmpFailureDetectorConfig(
            new IcmpFailureDetectorConfig()
                .setEnabled(icmpFailureDetectorConfig.isEnabled())
                .setFailFastOnStartup(icmpFailureDetectorConfig.isFailFastOnStartup())
                .setMaxAttempts(icmpFailureDetectorConfig.getMaxAttempts())
                .setTimeoutMilliseconds(icmpFailureDetectorConfig.getTimeoutMilliseconds())
                .setIntervalMilliseconds(icmpFailureDetectorConfig.getIntervalMilliseconds())
                .setTtl(icmpFailureDetectorConfig.getTtl())
                .setParallelMode(icmpFailureDetectorConfig.isParallelMode()))
        .setPort(this.networkConfig.getPort())
        .setPortAutoIncrement(this.networkConfig.isPortAutoIncrementEnable());
    return networkConfig;
  }

  @Bean
  DurableExecutorConfig complexJobsExecutorsConfig(Config config) {
    DurableExecutorConfig durableExecutorConfig =
        new DurableExecutorConfig()
            .setSplitBrainProtectionName(COMPLEX_JOBS_PROBABILISTIC_SPLIT_BRAIN_PROTECTION)
            .setName(COMPLEX_JOBS_EXECUTORS)
            .setDurability(executorsConfig.getDurability())
            .setCapacity(executorsConfig.getCapacity())
            .setPoolSize(executorsConfig.getPoolSize())
            .setStatisticsEnabled(executorsConfig.isStatisticsEnabled());
    config.addDurableExecutorConfig(durableExecutorConfig);
    return durableExecutorConfig;
  }

  @Autowired
  public void setComplexJobsProperties(ComplexJobsProperties properties) {
    this.executorsConfig = properties.getExecutors();
    this.networkConfig = properties.getNetwork();
    this.icmpFailureDetectorConfig = properties.getIcmpFailureDetector();
    this.probabilisticSplitBrainProtectionConfig =
        properties.getProbabilisticSplitBrainProtectionConfig();
  }
}
