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

import static com.silong.foundation.dj.mixmaster.configure.config.MixmasterProperties.MAX_PARTITIONS_COUNT;

import com.github.javafaker.Faker;
import com.silong.foundation.dj.mixmaster.message.Messages.ClusterNodeInfo;
import com.silong.foundation.dj.mixmaster.vo.ClusterNodeUUID;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomUtils;
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
 * 测试
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
public class Partition2NodesTests {

  private static final Faker FAKER = new Faker();

  @Autowired private Partition2NodesMapping<ClusterNodeUUID> mapping;

  @Test
  public void test10() {
    Assertions.assertThrowsExactly(
        IllegalArgumentException.class,
        () -> mapping.allocatePartition(0, -1, List.of(randomNode()), null));
  }

  @Test
  public void test11() {
    Assertions.assertThrowsExactly(
        IllegalArgumentException.class,
        () -> mapping.allocatePartition(-1, 0, List.of(randomNode()), null));
  }

  @Test
  public void test12() {
    Assertions.assertThrowsExactly(
        IllegalArgumentException.class, () -> mapping.allocatePartition(0, 0, List.of(), null));
  }

  /** 单节点映射 */
  @Test
  @DisplayName("partition2Nodes-1")
  public void test1() {
    int totalPartition = MAX_PARTITIONS_COUNT;
    int backupNum = 0;
    List<ClusterNodeUUID> nodes = randomNodes(1);

    Map<String, AtomicInteger> count = new HashMap<>();

    for (int i = 0; i < totalPartition; i++) {
      SequencedCollection<ClusterNodeUUID> clusterNodeUUIDS =
          mapping.allocatePartition(i, backupNum, nodes, null);
      String instanceName = clusterNodeUUIDS.getFirst().clusterNodeInfo().getInstanceName();
      count.computeIfAbsent(instanceName, k -> new AtomicInteger()).getAndIncrement();
    }
    Assertions.assertEquals(
        totalPartition, count.get(nodes.getFirst().clusterNodeInfo().getInstanceName()).get());
  }

  /** 同样的节点乱序 */
  @Test
  @DisplayName("partition2Nodes-2")
  public void test2() {
    int totalPartition = MAX_PARTITIONS_COUNT;
    int backupNum = 1;
    List<ClusterNodeUUID> nodes = randomNodes(9);

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

  /** 集群节点脱离 */
  @Test
  @DisplayName("partition2Nodes-3")
  public void test3() {
    int totalPartition = 1024;
    int backupNum = 9;
    List<ClusterNodeUUID> nodes = randomNodes(10);

    Map<Integer, SequencedCollection<ClusterNodeUUID>> result1 = new HashMap<>();

    for (int i = 0; i < totalPartition; i++) {
      SequencedCollection<ClusterNodeUUID> clusterNodeUUIDS =
          mapping.allocatePartition(i, backupNum, nodes, null);

      result1.put(i, clusterNodeUUIDS);
    }

    // 随机删除一个节点
    ClusterNodeUUID clusterNodeUUID = nodes.remove(RandomUtils.nextInt(0, nodes.size()));

    Map<Integer, SequencedCollection<ClusterNodeUUID>> result2 = new HashMap<>();

    for (int i = 0; i < totalPartition; i++) {
      SequencedCollection<ClusterNodeUUID> clusterNodeUUIDS =
          mapping.allocatePartition(i, backupNum, nodes, null);
      result2.put(i, clusterNodeUUIDS);
    }

    result1.values().forEach(ns -> ns.remove(clusterNodeUUID));

    Assertions.assertEquals(result2, result1);
  }

  /** 集群节点脱离 */
  @Test
  @DisplayName("partition2Nodes-4")
  public void test4() {
    int totalPartition = 1024;
    int backupNum = 1;
    List<ClusterNodeUUID> nodes = randomNodes(5);

    Map<Integer, SequencedCollection<ClusterNodeUUID>> result1 = new HashMap<>();

    for (int i = 0; i < totalPartition; i++) {
      SequencedCollection<ClusterNodeUUID> clusterNodeUUIDS =
          mapping.allocatePartition(i, backupNum, nodes, null);

      result1.put(i, clusterNodeUUIDS);
    }

    // 随机删除一个节点
    ClusterNodeUUID clusterNodeUUID = nodes.remove(RandomUtils.nextInt(0, nodes.size()));

    Map<Integer, SequencedCollection<ClusterNodeUUID>> result2 = new HashMap<>();

    for (int i = 0; i < totalPartition; i++) {
      SequencedCollection<ClusterNodeUUID> clusterNodeUUIDS =
          mapping.allocatePartition(i, backupNum, nodes, null);
      result2.put(i, clusterNodeUUIDS);
    }

    for (int i = 0; i < totalPartition; i++) {
      SequencedCollection<ClusterNodeUUID> clusterNodeUUIDS = result1.get(i);
      if (clusterNodeUUIDS.contains(clusterNodeUUID)) {
        clusterNodeUUIDS.remove(clusterNodeUUID);
        Assertions.assertEquals(
            clusterNodeUUIDS, CollectionUtils.intersection(clusterNodeUUIDS, result2.get(i)));
      } else {
        Assertions.assertEquals(clusterNodeUUIDS, result2.get(i));
      }
    }
  }

  private static List<ClusterNodeUUID> randomNodes(int count) {
    LinkedList<ClusterNodeUUID> res = new LinkedList<>();
    for (int i = 0; i < count; i++) {
      res.add(randomNode());
    }
    return res;
  }

  private static ClusterNodeUUID randomNode() {
    return ClusterNodeUUID.random()
        .clusterNodeInfo(
            ClusterNodeInfo.newBuilder()
                .setInstanceName(FAKER.animal().name())
                .setClusterName("test-cluster")
                .setStartupTime(System.currentTimeMillis())
                .build());
  }
}
