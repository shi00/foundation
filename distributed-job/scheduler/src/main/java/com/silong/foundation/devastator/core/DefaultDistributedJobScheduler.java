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
import com.silong.foundation.devastator.model.ClusterNodeUUID;
import com.silong.foundation.devastator.model.Devastator.Job;
import com.silong.foundation.devastator.model.Devastator.JobMsgPayload;
import com.silong.foundation.devastator.utils.KryoUtils;
import com.silong.foundation.devastator.utils.LambdaSerializable.SerializableRunnable;
import lombok.extern.slf4j.Slf4j;
import org.jgroups.Address;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import static com.silong.foundation.devastator.model.Devastator.JobClass.RUNNABLE;
import static com.silong.foundation.devastator.model.Devastator.JobMsgType.CREATE_JOB;
import static com.silong.foundation.devastator.model.Devastator.JobState.INIT;
import static com.silong.foundation.devastator.model.Devastator.JobType.ONE_SHOT;
import static com.silong.foundation.devastator.utils.TypeConverter.Long2Bytes.INSTANCE;
import static com.silong.foundation.devastator.utils.Utilities.xxhash64;

/**
 * 分布式任务调度器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-07-02 22:31
 */
@Slf4j
class DefaultDistributedJobScheduler implements DistributedJobScheduler, AutoCloseable {

  /** 调度器 */
  private ScheduledExecutorService executorService;

  /** 引擎 */
  private DefaultDistributedEngine engine;

  /** 调度器名称 */
  private final String name;

  /**
   * 构造方法
   *
   * @param engine 分布式引擎
   * @param name 调度器名称
   * @param executorService 调度器
   */
  public DefaultDistributedJobScheduler(
      DefaultDistributedEngine engine, String name, ScheduledExecutorService executorService) {
    if (executorService == null) {
      throw new IllegalArgumentException("executorService must not be null.");
    }
    if (name == null) {
      throw new IllegalArgumentException("name must not be null.");
    }
    if (engine == null) {
      throw new IllegalArgumentException("engine must not be null.");
    }
    this.engine = engine;
    this.name = name;
    this.executorService = executorService;
  }

  @Override
  public void close() {
    if (executorService != null) {
      executorService.shutdown();
      executorService = null;
    }
    engine = null;
  }

  /**
   * 执行一次性立即任务
   *
   * @param command 任务
   */
  public void executeInternal(Runnable command) {
    if (command == null) {
      throw new IllegalArgumentException("command must not be null.");
    }
    executorService.execute(command);
  }

  @Override
  public void execute(Runnable command) throws Exception {
    if (command == null) {
      throw new IllegalArgumentException("command must not be null.");
    }

    // 包裹为可序列化Runnable
    SerializableRunnable runnable = command::run;
    byte[] bytes = KryoUtils.serialize(runnable);
    long jobId = xxhash64(bytes);

    Job.Builder jobBuilder =
        Job.newBuilder()
            .setJobId(jobId)
            .setSchedulerName(name)
            .setJobType(ONE_SHOT)
            .setJobClass(RUNNABLE)
            .setJobState(INIT)
            .setJobBytes(ByteString.copyFrom(bytes));
    JobMsgPayload.Builder jobMsgPayloadBuilder =
        JobMsgPayload.newBuilder().setJob(jobBuilder).setType(CREATE_JOB);
    byte[] jobPayload = jobMsgPayloadBuilder.build().toByteArray();

    // 任务映射的分区编号
    int partition = engine.objectPartitionMapping.partition(jobId);

    // 根据分区号获取其映射的节点列表
    List<ClusterNodeUUID> nodes = engine.metadata.getClusterNodes(partition);
    Address localAddress = engine.getLocalAddress();

    // 任务分发至分区对应的各节点
    for (int i = 0; i < nodes.size(); i++) {
      Address dest = nodes.get(i).uuid();
      if (dest.equals(localAddress)) {
        byte[] jobKey = INSTANCE.to(jobId);
        String partCf = engine.getPartitionCf(partition);
        engine.persistStorage.put(partCf, jobKey, jobPayload);

        // 如果是主分区，则持久化任务并触发执行，否则仅持久化
        if (i == 0) {
          executorService.execute(
              new WrapRunnable(
                  engine, partCf, jobKey, command, nodes, jobMsgPayloadBuilder, jobBuilder));
        }
      } else {
        engine.asyncSend(PooledBytesMessage.obtain().dest(dest).setArray(jobPayload));
      }
    }
  }
}
