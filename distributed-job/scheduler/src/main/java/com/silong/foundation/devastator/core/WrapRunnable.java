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

import com.silong.foundation.devastator.message.PooledBytesMessage;
import com.silong.foundation.devastator.model.Devastator.Job;
import com.silong.foundation.devastator.model.Devastator.JobMsgPayload;
import com.silong.foundation.devastator.model.Devastator.JobMsgType;
import com.silong.foundation.devastator.model.Devastator.JobState;
import lombok.extern.slf4j.Slf4j;
import org.jgroups.Address;

import java.util.List;

import static com.silong.foundation.devastator.model.Devastator.JobMsgType.UPDATE_JOB;
import static com.silong.foundation.devastator.model.Devastator.JobState.*;

/**
 * 支持集群节点同步和持久化能力的Runnable
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-07-09 22:22
 */
@Slf4j
class WrapRunnable implements Runnable {
  /** 分布式引擎 */
  private final DefaultDistributedEngine engine;

  /** 任务映射的节点列表 */
  private final List<DefaultClusterNode> nodes;

  /** 任务消息构建器 */
  private final JobMsgPayload.Builder jobMsgPayloadBuilder;

  /** 任务构建器 */
  private final Job.Builder jobBuilder;

  /** 任务 */
  private final Runnable command;

  /** 任务持久化key */
  private final byte[] jobKey;

  /** 列族名 */
  private final String partCf;

  WrapRunnable(
          DefaultDistributedEngine engine,
          String partCf, byte[] jobKey, Runnable command, List<DefaultClusterNode> nodes,
          JobMsgPayload.Builder jobMsgPayloadBuilder,
          Job.Builder jobBuilder) {
    this.engine = engine;
    this.nodes = nodes;
    this.jobMsgPayloadBuilder = jobMsgPayloadBuilder;
    this.jobBuilder = jobBuilder;
    this.command = command;
    this.jobKey = jobKey;
    this.partCf = partCf;
  }

  byte[] buildJobMsgPayload(JobState state, JobMsgType jobMsgType) {
    return jobMsgPayloadBuilder
        .setJob(jobBuilder.setJobState(state))
        .setType(jobMsgType)
        .build()
        .toByteArray();
  }

  void syncJob(byte[] jobPayload) throws Exception {
    Address localAddress = engine.jChannel.getAddress();
    for (DefaultClusterNode node : nodes) {
      Address dest = node.uuid();
      // 本地节点持久化
      if (localAddress.equals(dest)) {
        engine.persistStorage.put(partCf, jobKey, jobPayload);
      } else {
        // 远程节点消息同步
        engine.send(PooledBytesMessage.obtain().dest(dest).setArray(jobPayload));
      }
    }
  }

  @Override
  public void run() {
    try {
      // 更新任务状态为运行中
      syncJob(buildJobMsgPayload(RUNNING, UPDATE_JOB));

      try {
        command.run();
      } catch (Throwable t) {
        log.error("Failed to execute {}.", command, t);
        syncJob(buildJobMsgPayload(EXCEPTION, UPDATE_JOB));
        return;
      }

      // 更新任务状态为正常完成
      syncJob(buildJobMsgPayload(FINISH, UPDATE_JOB));
    } catch (Exception e) {
      log.error("Failed to update state of {}.", command, e);
    }
  }
}
