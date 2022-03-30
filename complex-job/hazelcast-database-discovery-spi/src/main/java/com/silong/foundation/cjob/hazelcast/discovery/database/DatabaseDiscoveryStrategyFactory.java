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
package com.silong.foundation.cjob.hazelcast.discovery.database;

import com.hazelcast.config.properties.PropertyDefinition;
import com.hazelcast.logging.ILogger;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.DiscoveryStrategy;
import com.hazelcast.spi.discovery.DiscoveryStrategyFactory;

import java.util.Collection;
import java.util.Map;

import static com.hazelcast.spi.discovery.DiscoveryStrategyFactory.DiscoveryStrategyLevel.CUSTOM;

/**
 * Configuration class of the Hazelcast Discovery Plugin for Database.
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-03-27 20:55
 */
public class DatabaseDiscoveryStrategyFactory implements DiscoveryStrategyFactory {
  @Override
  public Class<? extends DiscoveryStrategy> getDiscoveryStrategyType() {
    return DatabaseDiscoveryStrategy.class;
  }

  @Override
  public DiscoveryStrategy newDiscoveryStrategy(
      DiscoveryNode discoveryNode, ILogger logger, Map<String, Comparable> properties) {
    return new DatabaseDiscoveryStrategy(discoveryNode.getPrivateAddress(), logger, properties);
  }

  @Override
  public Collection<PropertyDefinition> getConfigurationProperties() {
    return null;
  }

  @Override
  public DiscoveryStrategyLevel discoveryStrategyLevel() {
    return CUSTOM;
  }
}
