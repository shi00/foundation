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

import com.google.common.collect.ImmutableList;
import io.etcd.jetcd.launcher.EtcdContainer;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.io.File;

import static com.silong.foundation.duuid.spi.Etcdv3WorkerIdAllocator.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-07 21:33
 */
public class Etcdv3WorkerIdAllocatorTests {

  /** 测试用镜像 */
  public static final String QUAY_IO_COREOS_ETCD_V_3_5_0 = "quay.io/coreos/etcd:v3.5.0";

  static Etcdv3WorkerIdAllocator allocator = new Etcdv3WorkerIdAllocator();

  EtcdContainer container;

  @BeforeEach
  void init() {
    container = new EtcdContainer(QUAY_IO_COREOS_ETCD_V_3_5_0, "server", ImmutableList.of());
  }

  @AfterEach
  void cleanUp() {
    if (container != null) {
      container.close();
    }
  }

  private void test(WorkerInfo info) {
    long allocate = -1;
    int i = 0;
    for (; i < 100; i++) {
      allocate = allocator.allocate(info);
    }
    assertEquals(i - 1, allocate);
  }

  @Test
  @DisplayName("SingleNode-Http-without-credentials")
  void test1() {
    container.start();
    test(
        WorkerInfo.builder()
            .name(SystemUtils.getHostName())
            .extraInfo(ImmutableMap.of(ETCDV3_ENDPOINTS, container.clientEndpoint().toString()))
            .build());
  }

  @Test
  @DisplayName("SingleNode-Https(oneway)-without-credentials")
  void test2() {
    container.withSll(true).start();
    test(
        WorkerInfo.builder()
            .name(SystemUtils.getHostName())
            .extraInfo(ImmutableMap.of(ETCDV3_ENDPOINTS, container.clientEndpoint().toString()))
            .build());
  }

  @Test
  @DisplayName("SingleNode-Https(oneway+client.key)-without-credentials")
  void test3() {
    container.withSll(true).start();
    test(
        WorkerInfo.builder()
            .name(SystemUtils.getHostName())
            .extraInfo(
                ImmutableMap.of(
                    ETCDV3_ENDPOINTS,
                    container.clientEndpoint().toString(),
                    ETCDV3_KEY_CERT_CHAIN_FILE,
                    new File("src/test/resources/ssl/cert/client.pem").getAbsolutePath(),
                    ETCDV3_KEY_FILE,
                    new File("src/test/resources/ssl/cert/client.key").getAbsolutePath()))
            .build());
  }

  @Test
  @DisplayName("SingleNode-Https(twoway)-without-credentials")
  void test4() {
    container.withSll(true).start();
    test(
        WorkerInfo.builder()
            .name(SystemUtils.getHostName())
            .extraInfo(
                ImmutableMap.of(
                    ETCDV3_ENDPOINTS,
                    container.clientEndpoint().toString(),
                    ETCDV3_TRUST_CERT_COLLECTION_FILE,
                    new File("src/test/resources/ssl/cert/ca.pem").getAbsolutePath(),
                    ETCDV3_KEY_CERT_CHAIN_FILE,
                    new File("src/test/resources/ssl/cert/client.pem").getAbsolutePath(),
                    ETCDV3_KEY_FILE,
                    new File("src/test/resources/ssl/cert/client.key").getAbsolutePath()))
            .build());
  }
}
