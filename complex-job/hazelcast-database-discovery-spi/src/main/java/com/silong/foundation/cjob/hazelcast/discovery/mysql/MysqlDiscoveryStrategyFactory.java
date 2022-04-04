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
package com.silong.foundation.cjob.hazelcast.discovery.mysql;

import com.hazelcast.config.properties.PropertyDefinition;
import com.hazelcast.logging.ILogger;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.DiscoveryStrategy;
import com.hazelcast.spi.discovery.DiscoveryStrategyFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.hazelcast.spi.discovery.DiscoveryStrategyFactory.DiscoveryStrategyLevel.CUSTOM;
import static com.silong.foundation.cjob.hazelcast.discovery.mysql.config.MysqlProperties.*;

/**
 * Configuration class of the Hazelcast Discovery Plugin for Mysql.
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-03-27 20:55
 */
public class MysqlDiscoveryStrategyFactory implements DiscoveryStrategyFactory {

  private static final Collection<PropertyDefinition> PROPERTY_DEFINITIONS =
      List.of(
          HEART_BEAT_TIMEOUT_MINUTES,
          HEART_BEAT_INTERVAL_SECONDS,
          DRIVER_CLASS,
          JDBC_URL,
          PASSWORD,
          USER_NAME,
          HOST_NAME);

  @Override
  public Class<? extends DiscoveryStrategy> getDiscoveryStrategyType() {
    return MysqlDiscoveryStrategy.class;
  }

  @Override
  public DiscoveryStrategy newDiscoveryStrategy(
      DiscoveryNode discoveryNode, ILogger logger, Map<String, Comparable> properties) {
    return new MysqlDiscoveryStrategy(discoveryNode.getPrivateAddress(), logger, properties);
  }

  @Override
  public Collection<PropertyDefinition> getConfigurationProperties() {
    return PROPERTY_DEFINITIONS;
  }

  @Override
  public DiscoveryStrategyLevel discoveryStrategyLevel() {
    return CUSTOM;
  }
}
