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
import com.hazelcast.spi.discovery.AbstractDiscoveryStrategy;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.SimpleDiscoveryNode;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.jooq.Record2;
import org.jooq.impl.DSL;
import org.jooq.types.DayToSecond;

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.silong.foundation.cjob.hazelcast.discovery.mysql.config.MysqlProperties.*;
import static com.silong.foundation.cjob.hazelcast.discovery.mysql.model.Tables.HAZELCAST_CLUSTER_NODES;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.jooq.impl.DSL.abs;
import static org.jooq.impl.DSL.localDateTimeDiff;

/**
 * Mysql节点发现策略
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-03-27 21:18
 */
@Slf4j
public class MysqlDiscoveryStrategy extends AbstractDiscoveryStrategy {

  /** 是否初始化 */
  private final AtomicBoolean initialized = new AtomicBoolean(false);

  /** 节点地址 */
  private final String ipAddress;

  /** 节点监听端口 */
  private final int port;

  /** 节点名 */
  private final String hostName;

  /** 集群名 */
  private final String clusterName;

  /** 实例名 */
  private final String instanceName;

  /** 数据库名称 */
  private final String database;

  /** 线程池 */
  private ScheduledExecutorService scheduledExecutorService;

  /** 数据源 */
  private HikariDataSource dataSource;

  /**
   * 构造方法
   *
   * @param address 节点地址
   * @param logger 日志打印
   * @param properties 属性
   */
  public MysqlDiscoveryStrategy(
      Address address, ILogger logger, Map<String, Comparable> properties) {
    super(logger, properties);
    this.ipAddress = address.getHost();
    this.port = address.getPort();
    this.hostName = SystemUtils.getHostName();
    this.clusterName = getOrNull(CLUSTER_NAME);
    this.instanceName = getOrDefault(INSTANCE_NAME, EMPTY);
    this.database = getOrNull(DATABASE);
  }

  private HikariDataSource initializeDataSource() {
    String jdbcDriver = getOrNull(DRIVER_CLASS);
    String jdbcUrl = getOrNull(JDBC_URL);
    String userName = getOrNull(USER_NAME);
    String password = getOrNull(PASSWORD);
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(jdbcUrl);
    config.setUsername(userName);
    config.setDriverClassName(jdbcDriver);
    config.setMaximumPoolSize(1);
    config.setPassword(password);
    config.addDataSourceProperty("cachePrepStmts", "true");
    config.addDataSourceProperty("prepStmtCacheSize", "250");
    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
    config.addDataSourceProperty("useServerPrepStmts", "true");
    config.addDataSourceProperty("useLocalSessionState", "true");
    config.addDataSourceProperty("rewriteBatchedStatements", "true");
    config.addDataSourceProperty("maintainTimeStats", "false");
    config.addDataSourceProperty("cacheResultSetMetadata", "true");
    config.addDataSourceProperty("cacheServerConfiguration", "true");
    config.addDataSourceProperty("elideSetAutoCommits", "true");
    return new HikariDataSource(config);
  }

  @Override
  public Iterable<DiscoveryNode> discoverNodes() {
    try {
      return selectActiveNodes();
    } finally {
      if (initialized.compareAndSet(false, true)) {
        scheduledExecutorService.scheduleAtFixedRate(this::insertOrUpdateNode, 0, 1, MINUTES);
      }
    }
  }

  private void insertOrUpdateNode() {
    try (Connection connection = dataSource.getConnection()) {
      DSL.using(connection)
          .transaction(
              ctx ->
                  DSL.using(ctx)
                      .insertInto(
                          HAZELCAST_CLUSTER_NODES,
                          HAZELCAST_CLUSTER_NODES.HOST_NAME,
                          HAZELCAST_CLUSTER_NODES.CLUSTER_NAME,
                          HAZELCAST_CLUSTER_NODES.INSTANCE_NAME,
                          HAZELCAST_CLUSTER_NODES.IP_ADDRESS,
                          HAZELCAST_CLUSTER_NODES.PORT)
                      .values(hostName, clusterName, instanceName, ipAddress, port)
                      .onDuplicateKeyUpdate()
                      .set(HAZELCAST_CLUSTER_NODES.CLUSTER_NAME, clusterName)
                      .set(HAZELCAST_CLUSTER_NODES.INSTANCE_NAME, instanceName)
                      .set(HAZELCAST_CLUSTER_NODES.IP_ADDRESS, ipAddress)
                      .set(HAZELCAST_CLUSTER_NODES.PORT, port)
                      .execute());
    } catch (SQLException e) {
      log.error(
          "Failed to insert or update node([{}:{}] {}:{}) to mysql.",
          clusterName,
          instanceName,
          ipAddress,
          port,
          e);
    }
  }

  /**
   * 查询所有活着的节点信息
   *
   * @return 活着节点列表
   */
  private List<DiscoveryNode> selectActiveNodes() {
    try (Connection connection = dataSource.getConnection()) {
      return DSL
          .using(connection)
          .select(HAZELCAST_CLUSTER_NODES.IP_ADDRESS, HAZELCAST_CLUSTER_NODES.PORT)
          .from(HAZELCAST_CLUSTER_NODES)
          .where(
              HAZELCAST_CLUSTER_NODES
                  .CLUSTER_NAME
                  .eq(clusterName)
                  .and(
                      abs(localDateTimeDiff(
                              DSL.currentLocalDateTime(), HAZELCAST_CLUSTER_NODES.UPDATED_TIME))
                          .lessOrEqual(
                              DayToSecond.valueOf(
                                  Duration.ofMinutes(
                                      getOrDefault(
                                          HEART_BEAT_TIMEOUT,
                                          DEFAULT_HEART_BEAT_TIMEOUT_MINUTES))))))
          .stream()
          .map(this::buildDiscoveryNode)
          .toList();
    } catch (SQLException e) {
      log.error(
          "Failed to fetch active nodes of hazelcast({}:{}), return empty.",
          clusterName,
          instanceName,
          e);
      return List.of();
    }
  }

  @SneakyThrows
  private DiscoveryNode buildDiscoveryNode(Record2<String, Integer> record2) {
    return new SimpleDiscoveryNode(
        new Address(InetAddress.getByName(record2.value1()), record2.value2()));
  }

  @Override
  public void start() {
    this.dataSource = initializeDataSource();
    this.scheduledExecutorService =
        new ScheduledThreadPoolExecutor(1, r -> new Thread(r, "Hazelcast-Node-Heartbeat-Mysql"));
  }

  @Override
  public void destroy() {
    if (dataSource != null) {
      dataSource.close();
    }
    if (scheduledExecutorService != null) {
      scheduledExecutorService.shutdown();
    }
  }
}
