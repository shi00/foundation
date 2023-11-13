/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package com.silong.foundation.dj.mixmaster;

import com.github.javafaker.Faker;
import com.silong.foundation.dj.mixmaster.configure.config.MixmasterProperties;
import com.silong.foundation.dj.mixmaster.message.Messages.ClusterNodeInfo;
import com.silong.foundation.dj.mixmaster.vo.ClusterNodeUUID;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * 集成测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-19 21:36
 */
@Slf4j
@SpringBootTest(classes = MixmasterApp4Test.class)
@TestPropertySource(locations = "classpath:application.properties")
@ExtendWith(SpringExtension.class)
@ComponentScan
public class MixmasterTests {

  private static final Faker FAKER = new Faker();

  @Autowired private DistributedEngine distributedEngine;

  @Autowired private MixmasterProperties properties;

  @Autowired private Partition2NodesMapping<ClusterNodeUUID> mapping;

  @Test
  @DisplayName("partition2Nodes-1")
  public void test1() {
    int totalPartition = 8192;
    int backupNum = 0;
    List<ClusterNodeUUID> nodes = randomNodes(1);
    String instanceName = nodes.getFirst().clusterNodeInfo().getInstanceName();
    Map<String, AtomicInteger> count = new HashMap<>();

    for (int i = 0; i < totalPartition; i++) {
      SequencedCollection<ClusterNodeUUID> clusterNodeUUIDS =
          mapping.allocatePartition(i, backupNum, nodes, null);

      count.computeIfAbsent(instanceName, k -> new AtomicInteger()).getAndIncrement();

      System.out.println(
          clusterNodeUUIDS.stream()
              .map(u -> u.clusterNodeInfo().getInstanceName())
              .collect(Collectors.joining(",", "[", "]")));
    }
    Assertions.assertEquals(totalPartition, count.get(instanceName).get());
  }

  @Test
  @DisplayName("partition2Nodes-2")
  public void test2() {
    int totalPartition = 8192;
    int backupNum = 3;
    List<ClusterNodeUUID> nodes = randomNodes(89);

    Map<Integer, SequencedCollection<ClusterNodeUUID>> result1 = new HashMap<>();

    for (int i = 0; i < totalPartition; i++) {
      SequencedCollection<ClusterNodeUUID> clusterNodeUUIDS =
          mapping.allocatePartition(i, backupNum, nodes, null);

      result1.put(i, clusterNodeUUIDS);
    }

    Collections.shuffle(nodes); // 节点乱序

    Map<Integer, SequencedCollection<ClusterNodeUUID>> result2 = new HashMap<>();

    for (int i = 0; i < totalPartition; i++) {
      SequencedCollection<ClusterNodeUUID> clusterNodeUUIDS =
          mapping.allocatePartition(i, backupNum, nodes, null);

      result2.put(i, clusterNodeUUIDS);
    }

    Assertions.assertEquals(result2, result1);
  }

  private static List<ClusterNodeUUID> randomNodes(int count) {
    LinkedList<ClusterNodeUUID> res = new LinkedList<>();
    for (int i = 0; i < count; i++) {
      res.add(
          ClusterNodeUUID.random()
              .clusterNodeInfo(
                  ClusterNodeInfo.newBuilder()
                      .setInstanceName(FAKER.animal().name())
                      .setClusterName("test-cluster")
                      .setStartupTime(System.currentTimeMillis())
                      .build()));
    }
    return res;
  }
}
