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
package com.silong.foundation.plugins.maven.mysql;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.testcontainers.containers.MySQLContainer;

import java.util.Map;
import java.util.Properties;

/**
 * 容器启动
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-23 15:15
 */
@Mojo(name = "start", defaultPhase = LifecyclePhase.NONE)
public class StartMysqlMojo extends AbstractMysqlMojo {

  @Override
  public void execute() {
    if (skip) {
      getLog().info("Skipping mysql...");
      return;
    }
    Properties properties = null;
    if (project != null) {
      properties = project.getProperties();
    }
    MySQLContainer mySqlContainer =
        new MySQLContainer(image)
            .withDatabaseName(database)
            .withUsername(userName)
            .withPassword(password);
    mySqlContainer.start();

    Map pluginContext = getPluginContext();
    pluginContext.put(MYSQL_CONTEXT_KEY, mySqlContainer);
    if (properties != null) {
      properties.setProperty(jdbcUrl, mySqlContainer.getJdbcUrl());
      getLog().info(String.format("${%s}=%s", jdbcUrl, mySqlContainer.getJdbcUrl()));
    }
    getLog().info("Start mysql successfully.");
  }
}
