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

package com.silong.foundation.springboot.starter.tokenauth.configure;

import static com.hazelcast.config.EvictionPolicy.NONE;
import static com.hazelcast.config.InMemoryFormat.BINARY;
import static com.hazelcast.config.MaxSizePolicy.PER_NODE;
import static com.silong.foundation.springboot.starter.tokenauth.common.Constants.TOKEN_CACHE;
import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.REACTIVE;

import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spi.merge.PutIfAbsentMergePolicy;
import com.silong.foundation.springboot.starter.tokenauth.configure.config.SimpleAuthProperties;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * token缓存 配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-05-07 10:45
 */
@Configuration
@ConditionalOnProperty(prefix = "token-auth.hazelcast-config", value = "cluster-name")
@EnableConfigurationProperties(SimpleAuthProperties.class)
@ConditionalOnWebApplication(type = REACTIVE)
public class TokenCacheAutoConfiguration {

  @Bean
  Config registerHazelcastConfig(
      @Value("${spring.application.name}") String appName, SimpleAuthProperties properties) {
    SimpleAuthProperties.HazelcastConfig hazelcastConfig = properties.getHazelcastConfig();
    return new Config()
        .setClusterName(appName + "-token-cache-cluster")
        .addMapConfig(
            new MapConfig()
                .setInMemoryFormat(BINARY)
                .setName(TOKEN_CACHE)
                .setStatisticsEnabled(false)
                .setReadBackupData(true)
                .setMergePolicyConfig(
                    new MergePolicyConfig().setPolicy(PutIfAbsentMergePolicy.class.getName()))
                .setEvictionConfig(
                    new EvictionConfig()
                        .setEvictionPolicy(NONE)
                        .setMaxSizePolicy(PER_NODE)
                        .setSize(properties.getPerNodeMaxTokenSize()))
                .setTimeToLiveSeconds(properties.getTokenTimeout())
                .setBackupCount(1))
        .setInstanceName(
            String.format(
                "%s.%s:%d.hazelcast",
                appName, hazelcastConfig.getAddress(), hazelcastConfig.getPort()))
        .setNetworkConfig(
            new NetworkConfig()
                .setJoin(
                    new JoinConfig()
                        .setMulticastConfig(
                            new MulticastConfig()
                                .setMulticastTimeToLive(
                                    hazelcastConfig.getMulticastConfig().getTimeToLive())
                                .setMulticastTimeoutSeconds(
                                    hazelcastConfig.getMulticastConfig().getTimeout())
                                .setMulticastGroup(hazelcastConfig.getMulticastConfig().getGroup())
                                .setMulticastPort(hazelcastConfig.getMulticastConfig().getPort())
                                .setEnabled(hazelcastConfig.getMulticastConfig().isEnable())))
                .setPortAutoIncrement(false)
                .setReuseAddress(true)
                .setInterfaces(new InterfacesConfig().addInterface(hazelcastConfig.getAddress()))
                .setPort(hazelcastConfig.getPort()));
  }

  @Bean
  HazelcastInstance registerHazelcastInstance(Config config) {
    return Hazelcast.newHazelcastInstance(config);
  }

  @Bean(TOKEN_CACHE)
  Map<String, String> registerTokenCache(HazelcastInstance hazelcastInstance) {
    return hazelcastInstance.getMap(TOKEN_CACHE);
  }
}
