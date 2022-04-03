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
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;

import static com.hazelcast.config.properties.PropertyTypeConverter.LONG;
import static com.hazelcast.config.properties.PropertyTypeConverter.STRING;

/**
 * Mysql配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-03-30 21:09
 */
public final class MysqlProperties {

    /**
     * 默认心跳超时10分钟
     */
    public static final int DEFAULT_HEART_BEAT_TIMEOUT_MINUTES = 10;

  /** 数据库访问用户名 */
  public static final PropertyDefinition USER_NAME =
      new SimplePropertyDefinition("user-name", false, STRING, value->{
        if (value instanceof String user && StringUtils.isNotEmpty(user)){
          return;
        }
        throw new ValidationException("user-name must not be null.");
      });

  /** 数据库访问密码 */
  public static final PropertyDefinition PASSWORD =
      new SimplePropertyDefinition("password", false, STRING, value->{
        if (value instanceof String pwd && StringUtils.isNotEmpty(pwd)){
          return;
        }
        throw new ValidationException("password must not be null.");
      });

  /** jdbc类全限定名 */
  public static final PropertyDefinition DRIVER_CLASS =
      new SimplePropertyDefinition("driver-class", false, STRING, value->{
          if (value instanceof String driverClass && StringUtils.isNotEmpty(driverClass)){
              return;
          }
          throw new ValidationException("driver-class must not be null.");
      });

  /** 主机名称 */
  public static final PropertyDefinition HOST_NAME =
      new SimplePropertyDefinition("host-name", true, STRING, value->{
          if (value instanceof String hostName && StringUtils.isNotEmpty(hostName)){
              return;
          }
          throw new ValidationException("host-name must not be null.");
      });

  /** jdbc url */
  public static final PropertyDefinition JDBC_URL =
      new SimplePropertyDefinition("jdbc-url", false, STRING, value->{
          if (value instanceof String jdbcUrl && StringUtils.isNotEmpty(jdbcUrl)){
              return;
          }
          throw new ValidationException("jdbc-url must not be null.");
      });

  /** 集群名 */
  public static final PropertyDefinition CLUSTER_NAME =
      new SimplePropertyDefinition("cluster", false, STRING, value->{
          if (value instanceof String cluster && StringUtils.isNotEmpty(cluster)){
              return;
          }
          throw new ValidationException("cluster must not be null.");
      });

  /** 实例名 */
  public static final PropertyDefinition INSTANCE_NAME =
      new SimplePropertyDefinition("instance", true, STRING);

  /** 节点心跳超时时间，单位：分钟 */
  public static final PropertyDefinition HEART_BEAT_TIMEOUT =
      new SimplePropertyDefinition("heart-beat-timeout", true, LONG, value->{
          if (value instanceof Number timeout && timeout.longValue()>0){
              return;
          }
          throw new ValidationException("heart-beat-timeout must not be null.");
      });


  private MysqlProperties() {}
}
