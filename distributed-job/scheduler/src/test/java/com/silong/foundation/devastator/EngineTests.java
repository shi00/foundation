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
package com.silong.foundation.devastator;

import com.github.javafaker.Animal;
import com.github.javafaker.Faker;
import com.silong.foundation.devastator.ClusterNode.ClusterNodeRole;
import com.silong.foundation.devastator.config.DevastatorConfig;
import com.silong.foundation.devastator.core.DefaultDistributedEngine;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static com.silong.foundation.devastator.ClusterNode.ClusterNodeRole.CLIENT;
import static com.silong.foundation.devastator.ClusterNode.ClusterNodeRole.WORKER;
import static com.silong.foundation.devastator.core.DefaultMembershipChangePolicy.CLUSTER_NODE_PERFORMANCE_RANK_ATTRIBUTE_KEY;

/**
 * 序列化测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-23 19:22
 */
public class EngineTests {

  private final String udpConfigFile =
      EngineTests.class.getClassLoader().getResource("fast.xml").getFile();

  private final Animal animal = new Faker().animal();

  @Test
  void test1() throws IOException {
    DistributedEngine distributedEngine1 =
        buildEngine(
            "test-cluster",
            animal.name(),
            CLIENT,
            udpConfigFile,
            Map.of(CLUSTER_NODE_PERFORMANCE_RANK_ATTRIBUTE_KEY, "1.0"));

    DistributedEngine distributedEngine2 =
        buildEngine(
            "test-cluster",
            animal.name(),
            WORKER,
            udpConfigFile,
            Map.of(CLUSTER_NODE_PERFORMANCE_RANK_ATTRIBUTE_KEY, "1.1"));

    distributedEngine1.close();
    distributedEngine2.close();
  }

  private DistributedEngine buildEngine(
      String clusterName,
      String instanceName,
      ClusterNodeRole role,
      String configFile,
      Map<String, String> attributes) {
    String absolutePath =
        SystemUtils.getJavaIoTmpDir().toPath().resolve(instanceName).toFile().getAbsolutePath();
    DevastatorConfig config =
        new DevastatorConfig()
            .configFile(configFile)
            .clusterNodeRole(role)
            .clusterName(clusterName)
            .instanceName(instanceName);
    config.persistStorageConfig().persistDataPath(absolutePath);
    config.clusterNodeAttributes().putAll(attributes);
    return new DefaultDistributedEngine(config);
  }
}
