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
import com.hazelcast.core.TypeConverter;

import static com.hazelcast.config.properties.PropertyTypeConverter.STRING;

/**
 * Mysql配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-03-30 21:09
 */
public final class MysqlProperties {

  /** 数据库访问用户名 */
  public static final PropertyDefinition USER_NAME = property("user-name", STRING);

  /** 数据库访问密码 */
  public static final PropertyDefinition PASSWORD = property("password", STRING);

  /** jdbc类全限定名 */
  public static final PropertyDefinition DRIVER_CLASS = property("driver-class", STRING);

  /** jdbc url */
  public static final PropertyDefinition JDBC_URL = property("jdbc-url", STRING);

  private static PropertyDefinition property(String key, TypeConverter typeConverter) {
    return new SimplePropertyDefinition(key, true, typeConverter);
  }

  private MysqlProperties() {}
}
