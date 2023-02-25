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
package com.silong.foundation.duuid.server;

import com.silong.foundation.crypto.aes.AesGcmToolkit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.*;
import java.util.Properties;

import static java.util.Objects.requireNonNull;

/**
 * 服务集成测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-11 20:49
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = DuuidServerApplication.class)
@TestPropertySource(
    locations = "classpath:application-mysql.properties",
    properties = {
      // 规避两个随机端口，无法获取第二个随机端口问题，此处指定管理端口
      "management.server.port=35672",
    })
@ActiveProfiles("mysql")
@ExtendWith(SpringExtension.class)
@Testcontainers
class DuuidServerApplicationMysqlTests extends AbstractIT {

  /** 测试用镜像 */
  public static final String MYSQL_8_0_28 = "mysql:8.0.31";

  public static final String APPLICATION_PROPERTIES = "application-mysql.properties";

  public static final String OUT_FILE =
      requireNonNull(
              DuuidServerApplicationMysqlTests.class
                  .getClassLoader()
                  .getResource(APPLICATION_PROPERTIES))
          .getFile();

  public static final File TEMPLATE_FILE =
      new File(OUT_FILE)
          .getParentFile()
          .getParentFile()
          .getParentFile()
          .toPath()
          .resolve("src")
          .resolve("test")
          .resolve("resources")
          .resolve(APPLICATION_PROPERTIES)
          .toFile();

  private static final String PASSWORD = "Test@1234";

  @Container
  private static final MySQLContainer MYSQL_CONTAINER =
      new MySQLContainer(MYSQL_8_0_28)
          .withDatabaseName("test_db")
          .withUsername("root")
          .withPassword(PASSWORD);

  /**
   * 初始化etcd容器，并更新服务配置
   *
   * @throws IOException 异常
   */
  @BeforeAll
  static void mysqlInit() throws IOException {
    MYSQL_CONTAINER.start();
    Properties applicationProperties = new Properties();
    try (Reader in = new FileReader(TEMPLATE_FILE)) {
      applicationProperties.load(in);
      applicationProperties.setProperty("simple-auth.work-key", workKey);
      applicationProperties.setProperty(
          "duuid.worker-id-provider.mysql.password", AesGcmToolkit.encrypt(PASSWORD, workKey));
      applicationProperties.setProperty(
          "duuid.worker-id-provider.mysql.jdbc-url", MYSQL_CONTAINER.getJdbcUrl());
    }
    try (Writer out = new FileWriter(OUT_FILE)) {
      applicationProperties.store(out, "For Integration Testing");
    }
  }

  @AfterAll
  static void cleanUp() {
    if (MYSQL_CONTAINER != null) {
      MYSQL_CONTAINER.stop();
    }
  }
}
