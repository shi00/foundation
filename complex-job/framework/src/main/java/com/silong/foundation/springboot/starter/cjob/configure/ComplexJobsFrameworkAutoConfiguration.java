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

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.durableexecutor.DurableExecutorService;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.silong.foundation.springboot.starter.cjob.configure.config.ComplexJobsProperties.COMPLEX_JOBS_EXECUTORS;

/**
 * 自动配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-17 13:50
 */
@Configuration
@AutoConfigureAfter(ComplexJobsExecutorsAutoConfiguration.class)
public class ComplexJobsFrameworkAutoConfiguration {

  /**
   * Hazelcast实例
   *
   * @param config 配置
   * @return Hazelcast实例
   */
  @Bean(destroyMethod = "shutdown")
  @ConditionalOnMissingBean
  HazelcastInstance complexJobsHazelcast(Config config) {
    return Hazelcast.newHazelcastInstance(config);
  }

  /**
   * 分布式线程池
   *
   * @param instance hazelcast实例
   * @return 分布式线程池
   */
  @Bean(name = COMPLEX_JOBS_EXECUTORS, destroyMethod = "shutdown")
  DurableExecutorService complexJobsExecutors(HazelcastInstance instance) {
    return instance.getDurableExecutorService(COMPLEX_JOBS_EXECUTORS);
  }
}
