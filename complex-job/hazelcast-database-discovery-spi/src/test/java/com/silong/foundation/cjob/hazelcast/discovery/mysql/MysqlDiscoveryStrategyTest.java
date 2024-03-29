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

import static com.silong.foundation.cjob.hazelcast.discovery.mysql.config.MysqlProperties.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hazelcast.cluster.Address;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.NoLogFactory;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.DiscoveryStrategy;
import com.hazelcast.spi.discovery.SimpleDiscoveryNode;
import com.silong.foundation.cjob.hazelcast.discovery.mysql.utils.MysqlHelper;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import lombok.SneakyThrows;
import net.datafaker.Faker;
import org.apache.commons.lang3.RandomUtils;
import org.junit.ClassRule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

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
      new MySQLContainer<>(DockerImageName.parse("mysql:8.0.31"))
          .withReuse(true)
          .withInitScript("hazelcast-cluster-nodes.sql");

  public static Map<String, Comparable> properties;

  private final MysqlDiscoveryStrategyFactory strategyFactory = new MysqlDiscoveryStrategyFactory();

  private static final Faker FAKER = new Faker();

  private final MysqlHelper dbHelper =
      ((MysqlDiscoveryStrategy)
              strategyFactory.newDiscoveryStrategy(randomSimpleDiscoveryNode(), LOGGER, properties))
          .getDbHelper();

  @BeforeAll
  static void init() {
    MYSQL.start();
    properties =
        Map.of(
            CLUSTER_NAME.key(),
            "cluster1",
            HOST_NAME.key(),
            FAKER.name().username(),
            DRIVER_CLASS.key(),
            "com.mysql.cj.jdbc.Driver",
            INSTANCE_NAME.key(),
            "inst1",
            HEART_BEAT_TIMEOUT_MINUTES.key(),
            60,
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

  @BeforeEach
  void truncateTable() {
    dbHelper.deleteAll();
  }

  @Test
  void test1() {
    DiscoveryStrategy discoveryStrategy =
        strategyFactory.newDiscoveryStrategy(randomSimpleDiscoveryNode(), LOGGER, properties);
    discoveryStrategy.start();
    Iterable<DiscoveryNode> discoveryNodes = discoveryStrategy.discoverNodes();
    discoveryStrategy.destroy();
    assertEquals(discoveryNodes, List.of());
  }

  @Test
  void test2() throws InterruptedException {
    SimpleDiscoveryNode discoveryNode1 = randomSimpleDiscoveryNode();
    DiscoveryStrategy discoveryStrategy1 =
        strategyFactory.newDiscoveryStrategy(discoveryNode1, LOGGER, properties);
    discoveryStrategy1.start();
    Iterable<DiscoveryNode> discoveryNodes1 = discoveryStrategy1.discoverNodes();
    assertEquals(List.of(), discoveryNodes1);

    Thread.sleep(1000);

    SimpleDiscoveryNode discoveryNode2 = randomSimpleDiscoveryNode();
    DiscoveryStrategy discoveryStrategy2 =
        strategyFactory.newDiscoveryStrategy(discoveryNode2, LOGGER, properties);
    discoveryStrategy2.start();
    Iterable<DiscoveryNode> discoveryNodes2 = discoveryStrategy2.discoverNodes();

    assertEquals(1, StreamSupport.stream(discoveryNodes2.spliterator(), false).count());
    assertTrue(
        compare(
            discoveryNode1,
            StreamSupport.stream(discoveryNodes2.spliterator(), false).findFirst().get()));
    discoveryStrategy1.destroy();
    discoveryStrategy2.destroy();
  }

  @Test
  void test3() throws InterruptedException {
    SimpleDiscoveryNode discoveryNode1 = randomSimpleDiscoveryNode();
    DiscoveryStrategy discoveryStrategy1 =
        strategyFactory.newDiscoveryStrategy(discoveryNode1, LOGGER, properties);
    discoveryStrategy1.start();
    Iterable<DiscoveryNode> discoveryNodes1 = discoveryStrategy1.discoverNodes();

    SimpleDiscoveryNode discoveryNode2 = randomSimpleDiscoveryNode();
    DiscoveryStrategy discoveryStrategy2 =
        strategyFactory.newDiscoveryStrategy(discoveryNode2, LOGGER, properties);
    discoveryStrategy2.start();
    Iterable<DiscoveryNode> discoveryNodes2 = discoveryStrategy2.discoverNodes();

    Thread.sleep(1000);

    SimpleDiscoveryNode discoveryNode3 = randomSimpleDiscoveryNode();
    DiscoveryStrategy discoveryStrategy3 =
        strategyFactory.newDiscoveryStrategy(discoveryNode3, LOGGER, properties);
    discoveryStrategy3.start();
    Iterable<DiscoveryNode> discoveryNodes3 = discoveryStrategy3.discoverNodes();

    assertEquals(2, StreamSupport.stream(discoveryNodes3.spliterator(), false).count());

    for (DiscoveryNode discoveryNode : discoveryNodes3) {
      assertTrue(compare(discoveryNode1, discoveryNode) || compare(discoveryNode2, discoveryNode));
    }

    discoveryStrategy1.destroy();
    discoveryStrategy2.destroy();
    discoveryStrategy3.destroy();
  }

  @Test
  void test4() throws InterruptedException {
    SimpleDiscoveryNode discoveryNode1 = randomSimpleDiscoveryNode();
    DiscoveryStrategy discoveryStrategy1 =
        strategyFactory.newDiscoveryStrategy(discoveryNode1, LOGGER, properties);
    discoveryStrategy1.start();
    Iterable<DiscoveryNode> discoveryNodes1 = discoveryStrategy1.discoverNodes();

    SimpleDiscoveryNode discoveryNode2 = randomSimpleDiscoveryNode();
    DiscoveryStrategy discoveryStrategy2 =
        strategyFactory.newDiscoveryStrategy(discoveryNode2, LOGGER, properties);
    discoveryStrategy2.start();
    Iterable<DiscoveryNode> discoveryNodes2 = discoveryStrategy2.discoverNodes();

    SimpleDiscoveryNode discoveryNode3 = randomSimpleDiscoveryNode();
    DiscoveryStrategy discoveryStrategy3 =
        strategyFactory.newDiscoveryStrategy(discoveryNode3, LOGGER, properties);
    discoveryStrategy3.start();
    Iterable<DiscoveryNode> discoveryNodes3 = discoveryStrategy3.discoverNodes();

    discoveryStrategy1.destroy();
    discoveryStrategy2.destroy();
    discoveryStrategy3.destroy();

    Thread.sleep(2000);

    dbHelper.deleteInactiveNodes(1);
    assertTrue(dbHelper.hasRecords());
  }

  private boolean compare(DiscoveryNode node1, DiscoveryNode node2) {
    return node2.getPublicAddress().equals(node1.getPublicAddress())
        && node1.getProperties().equals(node2.getProperties())
        && node2.getPrivateAddress().equals(node1.getPrivateAddress());
  }

  @SneakyThrows
  private SimpleDiscoveryNode randomSimpleDiscoveryNode() {
    return new SimpleDiscoveryNode(
        new Address(
            RandomUtils.nextBoolean()
                ? FAKER.internet().ipV4Address()
                : FAKER.internet().ipV6Address(),
            RandomUtils.nextInt(0, 65536)));
  }
}
