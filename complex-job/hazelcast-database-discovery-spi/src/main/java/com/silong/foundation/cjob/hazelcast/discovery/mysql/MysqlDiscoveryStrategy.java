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

import com.cronutils.model.Cron;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.hazelcast.cluster.Address;
import com.hazelcast.logging.ILogger;
import com.hazelcast.spi.discovery.AbstractDiscoveryStrategy;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.silong.foundation.cjob.hazelcast.discovery.mysql.utils.MysqlHelper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.cronutils.model.CronType.QUARTZ;
import static com.silong.foundation.cjob.hazelcast.discovery.mysql.config.MysqlProperties.*;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.StringUtils.EMPTY;

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

  /** 心跳超时时长 */
  private final int heartbeatTimeout;

  /** 心跳间隔时长 */
  private final int heartbeatInterval;

  /** 集群名 */
  private final String clusterName;

  /** 实例名 */
  private final String instanceName;

  /** 线程池 */
  private final ScheduledExecutorService scheduledExecutorService;

  /** 数据库工具 */
  @Getter private final MysqlHelper dbHelper;

  /** 下次执行时间 */
  private ZonedDateTime nextExecutionTime;

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
    this.hostName = getOrDefault(HOST_NAME, SystemUtils.getHostName());
    this.clusterName = getOrNull(CLUSTER_NAME);
    this.instanceName = getOrDefault(INSTANCE_NAME, EMPTY);
    this.heartbeatTimeout =
        getOrDefault(HEART_BEAT_TIMEOUT_MINUTES, DEFAULT_HEART_BEAT_TIMEOUT_MINUTES);
    this.heartbeatInterval =
        getOrDefault(HEART_BEAT_INTERVAL_SECONDS, DEFAULT_HEART_BEAT_interval_SECONDS);
    this.dbHelper =
        new MysqlHelper(
            getOrNull(DRIVER_CLASS),
            getOrNull(JDBC_URL),
            getOrNull(USER_NAME),
            getOrNull(PASSWORD));
    this.scheduledExecutorService =
        new ScheduledThreadPoolExecutor(1, r -> new Thread(r, "Hazelcast-Node-Heartbeat-Mysql"));
  }

  @Override
  public Iterable<DiscoveryNode> discoverNodes() {
    try {
      return dbHelper.selectActiveNodes(
          hostName, ipAddress, port, clusterName, instanceName, heartbeatTimeout);
    } finally {
      if (initialized.compareAndSet(false, true)) {
        scheduledExecutorService.scheduleAtFixedRate(
            () -> dbHelper.insertOrUpdateNode(hostName, clusterName, instanceName, ipAddress, port),
            0,
            heartbeatInterval,
            SECONDS);
      }
    }
  }

  @Override
  public void start() {
    if (getOrDefault(ENABLE_INACTIVE_NODES_CLEANUP, DEFAULT_ENABLE_INACTIVE_NODES_CLEANUP)) {
      initCleanupTask();
    }
  }

  private void initCleanupTask() {
    String cronExp = getOrDefault(INACTIVE_NODES_CLEANUP_CRON, DEFAULT_CLEANUP_INACTIVE_NODES_CRON);
    int threshold =
        getOrDefault(
            INACTIVE_NODES_TIMEOUT_THRESHOLD_HOURS, DEFAULT_INACTIVE_NODES_TIMEOUT_THRESHOLD_HOURS);

    CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(QUARTZ);
    CronParser parser = new CronParser(cronDefinition);
    Cron quartzCron = parser.parse(cronExp);
    ExecutionTime executionTime = ExecutionTime.forCron(quartzCron);
    nextExecutionTime = getNextExecutionTime(executionTime, now());
    scheduledExecutorService.scheduleAtFixedRate(
        () -> {
          ZonedDateTime now = now();
          if (ChronoUnit.SECONDS.between(nextExecutionTime, now) >= 0) {
            dbHelper.deleteInactiveNodes(threshold);
            nextExecutionTime = getNextExecutionTime(executionTime, now);
          }
        },
        0,
        3,
        SECONDS);
  }

  private ZonedDateTime now() {
    return ZonedDateTime.now(ZoneId.systemDefault());
  }

  private ZonedDateTime getNextExecutionTime(ExecutionTime executionTime, ZonedDateTime now) {
    return executionTime.nextExecution(now).orElseThrow(IllegalStateException::new);
  }

  @Override
  public void destroy() {
    if (dbHelper != null) {
      dbHelper.close();
    }
    if (scheduledExecutorService != null) {
      scheduledExecutorService.shutdown();
    }
  }
}
