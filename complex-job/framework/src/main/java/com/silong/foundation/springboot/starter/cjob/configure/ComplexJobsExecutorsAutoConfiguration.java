package com.silong.foundation.springboot.starter.cjob.configure;

import com.hazelcast.config.*;
import com.silong.foundation.springboot.starter.cjob.configure.config.ComplexJobsProperties;
import com.silong.foundation.springboot.starter.cjob.configure.config.ComplexJobsProperties.ExecutorsConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

import static com.silong.foundation.springboot.starter.cjob.configure.config.ComplexJobsProperties.COMPLEX_JOBS_CLUSTER;
import static com.silong.foundation.springboot.starter.cjob.configure.config.ComplexJobsProperties.COMPLEX_JOBS_EXECUTORS;

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

  /**
   * 如果当前spring上下文中没有hazelcast配置，则注入配置
   *
   * @return hazelcast配置
   */
  @Bean
  @ConditionalOnMissingBean
  Config complexJobsClusterConfig() {
    return new Config()
        .setClusterName(COMPLEX_JOBS_CLUSTER)
        .setInstanceName(UUID.randomUUID().toString());
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
  DurableExecutorConfig complexJobsExecutors(Config config) {
    DurableExecutorConfig durableExecutorConfig =
        new DurableExecutorConfig()
            .setSplitBrainProtectionName("")
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
  }
}
