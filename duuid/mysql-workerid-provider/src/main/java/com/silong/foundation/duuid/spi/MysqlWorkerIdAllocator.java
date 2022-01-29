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
package com.silong.foundation.duuid.spi;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.sql.*;
import java.util.Map;

import static java.sql.Statement.RETURN_GENERATED_KEYS;

/**
 * 基于ETCD v3的WorkerId分配器<br>
 * 通过key的多版本属性，以key的版本号作为workId
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-03 00:25
 */
@Slf4j
public class MysqlWorkerIdAllocator implements WorkerIdAllocator {

  /** 插入SQL */
  private static final String INSERT_SQL =
      "INSERT INTO WORKER_ID_ALLOCATOR ( HOST_NAME ) VALUES ( ? )";

  /** 建表SQL */
  private static final String CREATE_TABLE_SQL =
      "CREATE TABLE IF NOT EXISTS WORKER_ID_ALLOCATOR"
          + "(ID        BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'auto increment WORKER_ID',"
          + "HOST_NAME  VARCHAR(128) NOT NULL COMMENT 'host name',"
          + "CREATED    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'created time',"
          + "PRIMARY KEY (ID)) "
          + "COMMENT='Worker ID Allocator for DUUID Generator',ENGINE = INNODB DEFAULT CHARSET=utf8mb4;";

  /** jdbc url */
  public static final String JDBC_URL = "jdbc.url";

  /** 用户 */
  public static final String USER = "user";

  /** 用户密码 */
  public static final String PASSWORD = "password";

  /** jdbc驱动 */
  public static final String JDBC_DRIVER = "jdbc.driver";

  /** 主机名 */
  public static final String HOST_NAME = "host.name";

  @Override
  public long allocate(WorkerInfo info) {
    Map<String, String> extraInfo;
    if (info == null || (extraInfo = info.getExtraInfo()) == null) {
      throw new IllegalArgumentException("info and info.extraInfo must not be null.");
    }
    String password = getValueByKey(extraInfo, PASSWORD);
    String jdbcDriver = getValueByKey(extraInfo, JDBC_DRIVER);
    String userName = getValueByKey(extraInfo, USER);
    String jdbcUrl = getValueByKey(extraInfo, JDBC_URL);
    try (Connection connection = getConnection(jdbcDriver, jdbcUrl, userName, password);
        PreparedStatement preparedStatement =
            connection.prepareStatement(INSERT_SQL, RETURN_GENERATED_KEYS)) {
      createTable(connection);
      preparedStatement.setString(1, getValueByKey(extraInfo, HOST_NAME));
      preparedStatement.executeUpdate();
      try (ResultSet rs = preparedStatement.getGeneratedKeys()) {
        if (rs.next()) {
          return rs.getLong(1);
        }
        throw new IllegalStateException("Failed to get generated Key.");
      }
    } catch (SQLException | ClassNotFoundException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void createTable(Connection connection) throws IOException, SQLException {
    try (PreparedStatement preparedStatement = connection.prepareStatement(CREATE_TABLE_SQL)) {
      if (preparedStatement.execute()) {
        log.info("The table of WORKER_ID_ALLOCATOR was created successfully.");
      }
    }
  }

  private static Connection getConnection(
      String jdbcDriver, String jdbcUrl, String userName, String password)
      throws ClassNotFoundException, SQLException {
    Class.forName(jdbcDriver);
    return DriverManager.getConnection(jdbcUrl, userName, password);
  }

  private String getValueByKey(Map<String, String> extraInfo, String key) {
    String value = extraInfo.get(key);
    if (isEmpty(value)) {
      throw new IllegalArgumentException(
          String.format("%s must not be null or empty in extraInfo.", key));
    }
    return value;
  }

  private boolean isEmpty(String str) {
    return str == null || str.isEmpty();
  }
}
