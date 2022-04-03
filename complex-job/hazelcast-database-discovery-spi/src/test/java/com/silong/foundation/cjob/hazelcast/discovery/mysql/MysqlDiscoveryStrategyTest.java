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

import com.hazelcast.cluster.Address;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.NoLogFactory;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.DiscoveryStrategy;
import com.hazelcast.spi.discovery.SimpleDiscoveryNode;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomUtils;
import org.junit.ClassRule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import static com.silong.foundation.cjob.hazelcast.discovery.mysql.config.MysqlProperties.*;

/**
 * 单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-03 19:25
 */
public class MysqlDiscoveryStrategyTest {

  private static final ILogger LOGGER = new NoLogFactory().getLogger("no");

  @ClassRule
  public static final MySQLContainer<?> MYSQL =
      new MySQLContainer<>(DockerImageName.parse("mysql:8.0.28"))
          .withReuse(true)
          .withInitScript("hazelcast-cluster-nodes.sql");

  public static Map<String, Comparable> PROPERTIES;

  private static final DiscoveryNode LOCAL_NODE = getSimpleDiscoveryNode();

  MysqlDiscoveryStrategyFactory strategyFactory = new MysqlDiscoveryStrategyFactory();

  @BeforeAll
  static void init() {
    MYSQL.start();
    PROPERTIES =
        Map.of(
            CLUSTER_NAME.key(),
            "cluster-test1",
            DRIVER_CLASS.key(),
            "com.mysql.cj.jdbc.Driver",
            INSTANCE_NAME.key(),
            "inst1",
            HEART_BEAT_TIMEOUT.key(),
            1000,
            JDBC_URL.key(),
            MYSQL.getJdbcUrl(),
            PASSWORD.key(),
            MYSQL.getPassword(),
            USER_NAME.key(),
            MYSQL.getUsername());
  }

  @AfterAll
  static void cleanup() {
    MYSQL.stop();
  }

  @Test
  void test1() {
    DiscoveryStrategy discoveryStrategy =
        strategyFactory.newDiscoveryStrategy(LOCAL_NODE, LOGGER, PROPERTIES);
    discoveryStrategy.start();
    Iterable<DiscoveryNode> discoveryNodes = discoveryStrategy.discoverNodes();
    discoveryStrategy.destroy();
    Assertions.assertEquals(discoveryNodes, List.of());
  }

  @SneakyThrows
  private static SimpleDiscoveryNode getSimpleDiscoveryNode() {
    return new SimpleDiscoveryNode(
        new Address(InetAddress.getLocalHost(), RandomUtils.nextInt(1025, 65536)));
  }
}
