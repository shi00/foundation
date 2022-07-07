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
package com.silong.foundation.devastator.core;

import com.google.protobuf.ByteString;
import com.silong.foundation.devastator.DistributedJobScheduler;
import com.silong.foundation.devastator.message.PooledBytesMessage;
import com.silong.foundation.devastator.model.Devastator.ClusterMsgPayload;
import com.silong.foundation.devastator.model.SimpleClusterNode;
import com.silong.foundation.devastator.utils.KryoUtils;
import com.silong.foundation.devastator.utils.LambdaSerializable.SerializableRunnable;
import lombok.SneakyThrows;
import org.jgroups.Address;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import static com.silong.foundation.devastator.model.Devastator.ClusterMsgType.JOB;

/**
 * 分布式任务调度器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-07-02 22:31
 */
class DefaultDistributedJobScheduler implements DistributedJobScheduler, AutoCloseable {

  /** sha256 */
  private static final ThreadLocal<MessageDigest> SHA256 =
      ThreadLocal.withInitial(DefaultDistributedJobScheduler::newSha256Digest);

  /** 调度器 */
  private ScheduledExecutorService executorService;

  /** 引擎 */
  private DefaultDistributedEngine engine;

  /**
   * 构造方法
   *
   * @param engine 分布式引擎
   * @param executorService 调度器
   */
  public DefaultDistributedJobScheduler(
      DefaultDistributedEngine engine, ScheduledExecutorService executorService) {
    if (executorService == null) {
      throw new IllegalArgumentException("executorService must not be null.");
    }
    if (engine == null) {
      throw new IllegalArgumentException("engine must not be null.");
    }
    this.engine = engine;
    this.executorService = executorService;
  }

  @SneakyThrows
  private static MessageDigest newSha256Digest() {
    return MessageDigest.getInstance("SHA-256");
  }

  @Override
  public void close() {
    if (executorService != null) {
      executorService.shutdown();
      executorService = null;
    }
    engine = null;
    SHA256.remove();
  }

  @Override
  public void execute(Runnable command) throws Exception {
    if (command == null) {
      throw new IllegalArgumentException("command must not be null.");
    }

    // 包裹为可序列化Runnable
    SerializableRunnable runnable = command::run;
    byte[] bytes = KryoUtils.serialize(runnable);
    byte[] key = SHA256.get().digest(bytes);

    // 任务映射的分区编号
    int partition = engine.objectPartitionMapping.partition(Arrays.hashCode(key));

    // 根据分区号获取其映射的节点列表
    List<SimpleClusterNode> nodes = engine.partition2ClusterNodes.get(partition);

    ClusterMsgPayload msgPayload =
        ClusterMsgPayload.newBuilder().setJobData(ByteString.copyFrom(bytes)).setType(JOB).build();
    byte[] payload = msgPayload.toByteArray();

    Address localAddress = getLocalAddress();

    // 任务分发至分区对应的各节点
    for (SimpleClusterNode node : nodes) {
      Address dest = node.address();
      if (localAddress.equals(dest)) {
        continue;
      }
      engine.send(PooledBytesMessage.obtain().dest(dest).setArray(payload));
    }

    // 如果本地节点为主分区则持久化，并触发任务执行
    if (nodes.get(0).address().equals(localAddress)) {
      engine.persistStorage.put(String.valueOf(partition), key, bytes);
      executorService.execute(runnable);
    }
  }

  private Address getLocalAddress() {
    return ((DefaultClusterNode) engine.getLocalNode()).getLocalAddress();
  }
}
