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
package com.silong.foundation.cjob.hazelcast.discovery.mysql.utils;

import com.hazelcast.cluster.Address;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.SimpleDiscoveryNode;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Record2;
import org.jooq.impl.DSL;
import org.jooq.types.DayToSecond;

import java.io.Closeable;
import java.net.InetAddress;
import java.sql.Connection;
import java.time.Duration;
import java.util.List;

import static com.silong.foundation.cjob.hazelcast.discovery.mysql.model.Tables.HAZELCAST_CLUSTER_NODES;
import static org.jooq.impl.DSL.abs;
import static org.jooq.impl.DSL.localDateTimeDiff;

/**
 * 数据库工具类
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-04 09:16
 */
@Slf4j
public final class MysqlHelper implements Closeable {

  /** 数据源 */
  private final HikariDataSource dataSource;

  /**
   * 构造方法
   *
   * @param jdbcDriver 驱动类全限定名
   * @param jdbcUrl jdbc url
   * @param userName 用户名
   * @param password 密码
   */
  public MysqlHelper(String jdbcDriver, String jdbcUrl, String userName, String password) {
    this.dataSource = initializeDataSource(jdbcDriver, jdbcUrl, userName, password);
  }

  private HikariDataSource initializeDataSource(
      String jdbcDriver, String jdbcUrl, String userName, String password) {
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

  /** 删除所有记录 */
  public void deleteAll() {
    try (Connection connection = dataSource.getConnection()) {
      DSL.using(connection)
          .transaction(ctx -> DSL.using(ctx).truncateTable(HAZELCAST_CLUSTER_NODES).execute());
    } catch (Exception e) {
      log.error("Failed to delete all nodes.", e);
    }
  }

  /**
   * 删除所有节点心跳超时超过指定时长的节点记录
   *
   * @param timeoutThreshold 超时阈值
   */
  public void deleteInactiveNodes(Duration timeoutThreshold) {
    try (Connection connection = dataSource.getConnection()) {
      DSL.using(connection)
          .transaction(
              ctx ->
                  DSL.using(ctx)
                      .deleteFrom(HAZELCAST_CLUSTER_NODES)
                      .where(
                          abs(localDateTimeDiff(
                                  DSL.currentLocalDateTime(), HAZELCAST_CLUSTER_NODES.UPDATED_TIME))
                              .greaterOrEqual(DayToSecond.valueOf(timeoutThreshold)))
                      .execute());
    } catch (Exception e) {
      log.error(
          "Failed to delete all nodes wiht timeoutThreshold:{}s.", timeoutThreshold.toSeconds(), e);
    }
  }

  /**
   * 更新或插入记录
   *
   * @param hostName 主机名
   * @param clusterName 集群名
   * @param instanceName 实例名
   * @param ipAddress ip地址
   * @param port 端口号
   */
  public void insertOrUpdateNode(
      String hostName, String clusterName, String instanceName, String ipAddress, int port) {
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
                      .set(HAZELCAST_CLUSTER_NODES.UPDATED_TIME, DSL.currentLocalDateTime())
                      .execute());
    } catch (Exception e) {
      log.error(
          "Failed to insert or update node([{}:{}] {}:{}) to database.",
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
   * @param clusterName 集群名
   * @param instanceName 实例名
   * @param heartbeatTimeout 心跳超时时长
   * @return 活着节点列表
   */
  public List<DiscoveryNode> selectActiveNodes(
      String clusterName, String instanceName, int heartbeatTimeout) {
    try (Connection connection = dataSource.getConnection()) {
      return DSL
          .using(connection)
          .select(HAZELCAST_CLUSTER_NODES.IP_ADDRESS, HAZELCAST_CLUSTER_NODES.PORT)
          .from(HAZELCAST_CLUSTER_NODES)
          .where(
              HAZELCAST_CLUSTER_NODES
                  .CLUSTER_NAME
                  .eq(clusterName)
                  .and(HAZELCAST_CLUSTER_NODES.INSTANCE_NAME.eq(instanceName))
                  .and(
                      abs(localDateTimeDiff(
                              DSL.currentLocalDateTime(), HAZELCAST_CLUSTER_NODES.UPDATED_TIME))
                          .lessOrEqual(DayToSecond.valueOf(Duration.ofMinutes(heartbeatTimeout)))))
          .orderBy(HAZELCAST_CLUSTER_NODES.UPDATED_TIME.desc())
          .stream()
          .map(this::buildDiscoveryNode)
          .toList();
    } catch (Exception e) {
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
  public void close() {
    dataSource.close();
  }
}
