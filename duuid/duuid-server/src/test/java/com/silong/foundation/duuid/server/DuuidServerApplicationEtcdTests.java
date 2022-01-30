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

import io.etcd.jetcd.launcher.EtcdContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.*;
import java.util.List;
import java.util.Properties;

import static java.util.Objects.requireNonNull;
import static org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils.randomAlphabetic;

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
    locations = "classpath:application-etcd.properties",
    properties = {
      // 规避两个随机端口，无法获取第二个随机端口问题，此处指定管理端口
      "management.server.port=35671",
    })
@ActiveProfiles("etcd")
@ExtendWith(SpringExtension.class)
class DuuidServerApplicationEtcdTests extends AbstractIT {

  /** 测试用镜像 */
  public static final String QUAY_IO_COREOS_ETCD_V_3_5_0 = "quay.io/coreos/etcd:v3.5.0";

  public static final String APPLICATION_PROPERTIES = "application-etcd.properties";

  public static final String OUT_FILE =
      requireNonNull(
              DuuidServerApplicationEtcdTests.class
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

  /** etcd容器 */
  private static EtcdContainer container;

  /**
   * 初始化etcd容器，并更新服务配置
   *
   * @throws IOException 异常
   */
  @BeforeAll
  static void etcdInit() throws IOException {
    container = new EtcdContainer(QUAY_IO_COREOS_ETCD_V_3_5_0, randomAlphabetic(10), List.of());
    container.start();
    Properties applicationProperties = new Properties();
    try (Reader in = new FileReader(TEMPLATE_FILE)) {
      applicationProperties.load(in);
      applicationProperties.setProperty(
          "duuid.worker-id-provider.etcdv3.server-addresses",
          container.clientEndpoint().toString());
    }
    try (Writer out = new FileWriter(OUT_FILE)) {
      applicationProperties.store(out, "For Integration Testing");
    }
  }

  @AfterAll
  static void cleanUp() {
    if (container != null) {
      container.close();
    }
  }
}
