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
package com.silong.foundation.plugins.maven.mysql.tests;

import com.silong.foundation.plugins.maven.mysql.StartMysqlMojo;
import com.silong.foundation.plugins.maven.mysql.StopMysqlMojo;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.testcontainers.containers.MySQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.silong.foundation.plugins.maven.mysql.AbstractMysqlMojo.MYSQL_CONTEXT_KEY;

/**
 * 单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-23 19:52
 */
public class TestMysqlMojoTest extends AbstractMojoTestCase {

  /** 测试pom */
  public static final String FORKED_POM_FILE = "src/test/resources/unit/pom.xml";

  private static final Map PLUGIN_CONTEXT = new ConcurrentHashMap();

  public void test() throws Exception {
    StartMysqlMojo startMysqlMojo = (StartMysqlMojo) lookupMojo("start", FORKED_POM_FILE);
    startMysqlMojo.setPluginContext(PLUGIN_CONTEXT);
    assertNotNull(startMysqlMojo);
    startMysqlMojo.execute();
    MySQLContainer mySqlContainer =
        (MySQLContainer) startMysqlMojo.getPluginContext().get(MYSQL_CONTEXT_KEY);

    String version = "";
    String jdbcDriver = startMysqlMojo.getJdbcDriver();
    Class.forName(jdbcDriver);
    try (Connection connection =
            DriverManager.getConnection(
                mySqlContainer.getJdbcUrl(),
                startMysqlMojo.getUserName(),
                startMysqlMojo.getPassword());
        ResultSet resultSet = connection.createStatement().executeQuery("select version()")) {
      while (resultSet.next()) {
        System.out.println(version = resultSet.getString(1));
      }
    }

    StopMysqlMojo stopMysqlMojo = (StopMysqlMojo) lookupMojo("stop", FORKED_POM_FILE);
    stopMysqlMojo.setPluginContext(PLUGIN_CONTEXT);
    stopMysqlMojo.execute();
    assertTrue(startMysqlMojo.getImage().contains(version));
  }
}
