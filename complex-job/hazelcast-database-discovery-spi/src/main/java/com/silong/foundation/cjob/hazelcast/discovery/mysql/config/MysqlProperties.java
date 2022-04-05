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
package com.silong.foundation.cjob.hazelcast.discovery.mysql.config;

import com.hazelcast.config.properties.PropertyDefinition;
import com.hazelcast.config.properties.SimplePropertyDefinition;
import com.hazelcast.config.properties.ValidationException;

import java.time.Duration;

import static com.hazelcast.config.properties.PropertyTypeConverter.INTEGER;
import static com.hazelcast.config.properties.PropertyTypeConverter.STRING;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Mysql配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-03-30 21:09
 */
public interface MysqlProperties {

    /**
     * 默认心跳超时30秒
     */
    int DEFAULT_HEART_BEAT_INTERVAL_SECONDS = 30;

    /**
     * 默认心跳超时10分钟
     */
    int DEFAULT_HEART_BEAT_TIMEOUT_MINUTES = 10;

    /**
     * 默认开启去激活节点删除
     */
    boolean DEFAULT_ENABLE_INACTIVE_NODES_CLEANUP = true;

    /**
     * 默认去激活节点记录超时时间
     */
    int DEFAULT_INACTIVE_NODES_TIMEOUT_THRESHOLD_HOURS = (int) Duration.ofDays(7).toHours();

    /**
     * 每月最后一天凌晨2点开启清理去激活节点记录
     */
    String DEFAULT_CLEANUP_INACTIVE_NODES_CRON = "0 0 2 L 1/1 ? *";

  /** 数据库访问用户名 */
  PropertyDefinition USER_NAME =
      new SimplePropertyDefinition("user-name", false, STRING, value->{
        if (value instanceof String user && isNotEmpty(user)){
          return;
        }
        throw new ValidationException("user-name must not be null.");
      });

  /** 数据库访问密码 */
  PropertyDefinition PASSWORD =
      new SimplePropertyDefinition("password", false, STRING, value->{
        if (value instanceof String pwd && isNotEmpty(pwd)){
          return;
        }
        throw new ValidationException("password must not be null.");
      });

  /** jdbc类全限定名 */
  PropertyDefinition DRIVER_CLASS =
      new SimplePropertyDefinition("driver-class", false, STRING, value->{
          if (value instanceof String driverClass && isNotEmpty(driverClass)){
              return;
          }
          throw new ValidationException("driver-class must not be null.");
      });

  /** 主机名称 */
  PropertyDefinition HOST_NAME =
      new SimplePropertyDefinition("host-name", true, STRING, value->{
          if (value instanceof String hostName && isNotEmpty(hostName)){
              return;
          }
          throw new ValidationException("host-name must not be null.");
      });

  /** jdbc url */
  PropertyDefinition JDBC_URL =
      new SimplePropertyDefinition("jdbc-url", false, STRING, value->{
          if (value instanceof String jdbcUrl && isNotEmpty(jdbcUrl)){
              return;
          }
          throw new ValidationException("jdbc-url must not be null.");
      });

  /** 集群名 */
  PropertyDefinition CLUSTER_NAME =
      new SimplePropertyDefinition("cluster", false, STRING, value->{
          if (value instanceof String cluster && isNotEmpty(cluster)){
              return;
          }
          throw new ValidationException("cluster must not be null.");
      });

  /** 实例名 */
  PropertyDefinition INSTANCE_NAME =
      new SimplePropertyDefinition("instance", true, STRING);

  /** 节点心跳超时时间，单位：分钟 */
  PropertyDefinition HEART_BEAT_TIMEOUT_MINUTES =
      new SimplePropertyDefinition("heart-beat-timeout", true, INTEGER, value->{
          if (value instanceof Number timeout && timeout.intValue()>0){
              return;
          }
          throw new ValidationException("heart-beat-timeout must not be null.");
      });

    /** 节点心跳间隔时间，单位：秒 */
    PropertyDefinition HEART_BEAT_INTERVAL_SECONDS =
            new SimplePropertyDefinition("heart-beat-interval", true, INTEGER, value->{
                if (value instanceof Number interval && interval.intValue()>0){
                    return;
                }
                throw new ValidationException("heart-beat-interval must not be null.");
            });

    /** 清理去激活节点超时时间阈值，单位：小时 */
    PropertyDefinition INACTIVE_NODES_TIMEOUT_THRESHOLD_HOURS =
            new SimplePropertyDefinition("inactive-nodes-timeout-threshold-hours", true, INTEGER, value->{
                if (value instanceof Number threshold && threshold.intValue()>0){
                    return;
                }
                throw new ValidationException("inactive-nodes-timeout-threshold-hours must not be null.");
            });
}
