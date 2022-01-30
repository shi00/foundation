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

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static com.silong.foundation.duuid.spi.MysqlWorkerIdAllocator.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.testcontainers.shaded.org.apache.commons.lang.SystemUtils.USER_NAME;

/**
 * 单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-28 22:56
 */
@Testcontainers
public class MysqlWorkerIdAllocatorTests {

  private static final String PASSWORD_TEST = "Abcd@1234";

  private static final String USER_TEST = "root";

  private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";

  private static final MysqlWorkerIdAllocator ALLOCATOR = new MysqlWorkerIdAllocator();

  @Container
  private static final MySQLContainer MYSQL_CONTAINER =
      new MySQLContainer("mysql:8.0.28")
          .withDatabaseName("test")
          .withUsername(USER_TEST)
          .withPassword(PASSWORD_TEST);

  private static Map<String, String> EXTRA_INFO;

  @BeforeAll
  @SneakyThrows
  static void init() {
    EXTRA_INFO =
        Map.of(
            JDBC_URL,
            MYSQL_CONTAINER.getJdbcUrl(),
            MysqlWorkerIdAllocator.JDBC_DRIVER,
            JDBC_DRIVER,
            PASSWORD,
            PASSWORD_TEST,
            USER,
            USER_TEST,
            HOST_NAME,
            USER_NAME);
  }

  @Test
  void test() {
    for (int i = 1; i <= 100; i++) {
      long allocate =
          ALLOCATOR.allocate(WorkerInfo.builder().extraInfo(EXTRA_INFO).name(USER_NAME).build());
      assertEquals(i, allocate);
    }
  }

  @AfterAll
  static void cleanUp() {
    if (MYSQL_CONTAINER != null) {
      MYSQL_CONTAINER.stop();
    }
  }
}
