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
import com.silong.foundation.devastator.model.Devastator.Job;
import com.silong.foundation.devastator.model.Devastator.JobMsgPayload;
import com.silong.foundation.devastator.model.Devastator.JobMsgType;
import com.silong.foundation.devastator.model.Devastator.JobState;
import com.silong.foundation.devastator.utils.KryoUtils;
import com.silong.foundation.devastator.utils.LambdaSerializable.SerializableRunnable;
import lombok.extern.slf4j.Slf4j;
import net.jpountz.xxhash.XXHash64;
import net.jpountz.xxhash.XXHashFactory;
import org.jgroups.Address;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import static com.silong.foundation.devastator.model.Devastator.JobClass.RUNNABLE;
import static com.silong.foundation.devastator.model.Devastator.JobMsgType.CREATE_JOB;
import static com.silong.foundation.devastator.model.Devastator.JobMsgType.UPDATE_JOB;
import static com.silong.foundation.devastator.model.Devastator.JobState.*;
import static com.silong.foundation.devastator.model.Devastator.JobType.ONE_SHOT;
import static com.silong.foundation.devastator.utils.TypeConverter.Long2Bytes.INSTANCE;

/**
 * 分布式任务调度器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-07-02 22:31
 */
@Slf4j
class DefaultDistributedJobScheduler implements DistributedJobScheduler, AutoCloseable {

  /** xxhash64 */
  private static final XXHash64 XX_HASH_64 = XXHashFactory.fastestInstance().hash64();

  /** xxhash64 seed */
  private static final long XX_HASH_64_SEED =
      Long.parseLong(System.getProperty("devastator.xxhash64.seed", "0xcafebabe"));

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

  private long xxhash64(byte[] val) {
    return XX_HASH_64.hash(val, 0, val.length, XX_HASH_64_SEED);
  }

  @Override
  public void close() {
    if (executorService != null) {
      executorService.shutdown();
      executorService = null;
    }
    engine = null;
  }

  @Override
  public void execute(Runnable command) throws Exception {
    if (command == null) {
      throw new IllegalArgumentException("command must not be null.");
    }

    // 包裹为可序列化Runnable
    SerializableRunnable runnable = command::run;
    byte[] bytes = KryoUtils.serialize(runnable);
    long key = xxhash64(bytes);

    Job.Builder jobBuilder =
        Job.newBuilder()
            .setJobType(ONE_SHOT)
            .setJobClass(RUNNABLE)
            .setJobState(INIT)
            .setJobBytes(ByteString.copyFrom(bytes));
    JobMsgPayload.Builder jobMsgPayloadBuilder =
        JobMsgPayload.newBuilder().setJob(jobBuilder).setType(CREATE_JOB);
    byte[] jobPayload = jobMsgPayloadBuilder.build().toByteArray();

    // 任务映射的分区编号
    int partition = engine.objectPartitionMapping.partition(key);

    // 根据分区号获取其映射的节点列表
    List<DefaultClusterNode> nodes = engine.partition2ClusterNodes.get(partition);
    Address localAddress = engine.jChannel.address();

    // 任务分发至分区对应的各节点
    for (int i = 0; i < nodes.size(); i++) {
      Address dest = nodes.get(i).uuid();
      if (localAddress.equals(dest)) {
        byte[] jobKey = INSTANCE.to(key);
        String part = String.valueOf(partition);
        engine.persistStorage.put(part, jobKey, jobPayload);

        // 如果是主分区，则持久化任务并触发执行，否则仅持久化
        if (i == 0) {
          executorService.execute(
              () -> {
                try {
                  // 更新任务状态为运行中
                  syncJob(
                      part,
                      jobKey,
                      buildJobMsgPayload(jobBuilder, jobMsgPayloadBuilder, RUNNING, UPDATE_JOB),
                      localAddress,
                      nodes);

                  try {
                    command.run();
                  } catch (Throwable t) {
                    log.error("Failed to execute {}.", command, t);
                    syncJob(
                        part,
                        jobKey,
                        buildJobMsgPayload(jobBuilder, jobMsgPayloadBuilder, EXCEPTION, UPDATE_JOB),
                        localAddress,
                        nodes);
                    return;
                  }

                  // 更新任务状态为正常完成
                  syncJob(
                      part,
                      jobKey,
                      buildJobMsgPayload(jobBuilder, jobMsgPayloadBuilder, FINISH, UPDATE_JOB),
                      localAddress,
                      nodes);
                } catch (Exception e) {
                  log.error("Failed to update state of {}.", command, e);
                }
              });
        }
      } else {
        engine.send(PooledBytesMessage.obtain().dest(dest).setArray(jobPayload));
      }
    }
  }

  private byte[] buildJobMsgPayload(
      Job.Builder jobBuilder,
      JobMsgPayload.Builder jobMsgPayloadBuilder,
      JobState state,
      JobMsgType jobMsgType) {
    return jobMsgPayloadBuilder
        .setJob(jobBuilder.setJobState(state))
        .setType(jobMsgType)
        .build()
        .toByteArray();
  }

  private void syncJob(
      String columnFamilyName,
      byte[] jobKey,
      byte[] jobPayload,
      Address localAddress,
      List<DefaultClusterNode> nodes)
      throws Exception {
    for (DefaultClusterNode node : nodes) {
      Address dest = node.uuid();
      // 本地节点持久化
      if (localAddress.equals(dest)) {
        engine.persistStorage.put(columnFamilyName, jobKey, jobPayload);
      } else {
        // 远程节点消息同步
        engine.send(PooledBytesMessage.obtain().dest(dest).setArray(jobPayload));
      }
    }
  }
}
