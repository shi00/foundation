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
package com.silong.foundation.cjob.hazelcast.discovery.database.config;

import lombok.Data;

/**
 * 数据库配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-03-30 21:09
 */
@Data
public class DatabaseProperties {
  /** 数据库访问用户名 */
  private String userName;
  /** 数据库访问密码 */
  private String password;
  /** jdbc驱动 */
  private String driverClass;
  /** 数据库访问url */
  private String jdbcUrl;
}
