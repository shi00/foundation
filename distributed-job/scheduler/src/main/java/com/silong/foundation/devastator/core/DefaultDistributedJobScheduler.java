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
import com.silong.foundation.devastator.model.Devastator.MsgPayload;
import com.silong.foundation.devastator.utils.KryoUtils;
import com.silong.foundation.devastator.utils.LambdaSerializable.SerializableRunnable;
import lombok.extern.slf4j.Slf4j;
import org.jgroups.Address;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import static com.silong.foundation.devastator.model.Devastator.JobClass.RUNNABLE;
import static com.silong.foundation.devastator.model.Devastator.JobState.INIT;
import static com.silong.foundation.devastator.model.Devastator.JobType.ONE_SHOT;
import static com.silong.foundation.devastator.model.Devastator.MsgType.JOB;
import static com.silong.foundation.devastator.model.Devastator.OperationType.CREATE;
import static com.silong.foundation.devastator.utils.TypeConverter.Long2Bytes.INSTANCE;
import static com.silong.foundation.devastator.utils.Utilities.xxhash64;

/**
 * ????????????????????????
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-07-02 22:31
 */
@Slf4j
class DefaultDistributedJobScheduler implements DistributedJobScheduler, AutoCloseable {

  /** ????????? */
  private ScheduledExecutorService executorService;

  /** ?????? */
  private DefaultDistributedEngine engine;

  /** ??????????????? */
  private final String name;

  /**
   * ????????????
   *
   * @param engine ???????????????
   * @param name ???????????????
   * @param executorService ?????????
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
   * ???????????????????????????
   *
   * @param command ??????
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

    // ?????????????????????Runnable
    SerializableRunnable runnable = command::run;
    byte[] bytes = KryoUtils.serialize(runnable);
    long jobId = xxhash64(bytes);

    // ??????????????????
    Job.Builder jobBuilder =
        Job.newBuilder()
            .setJobId(jobId)
            .setSchedulerName(name)
            .setJobType(ONE_SHOT)
            .setJobClass(RUNNABLE)
            .setJobState(INIT)
            .setJobBytes(ByteString.copyFrom(bytes));
    MsgPayload.Builder jobMsgPayloadBuilder =
        MsgPayload.newBuilder().setJob(jobBuilder).setMsgType(JOB).setOpType(CREATE);
    byte[] jobPayload = jobMsgPayloadBuilder.build().toByteArray();

    // ???????????????????????????
    int partition = engine.objectPartitionMapping.partition(jobId);

    // ?????????????????????????????????????????????
    List<ClusterNodeUUID> nodes = engine.metadata.getClusterNodes(partition);
    Address localAddress = engine.getLocalAddress();

    // ???????????????????????????????????????
    for (int i = 0; i < nodes.size(); i++) {
      Address dest = nodes.get(i).uuid();
      if (dest.equals(localAddress)) {
        byte[] jobKey = INSTANCE.to(jobId);
        String partCf = engine.getPartitionCf(partition);
        engine.persistStorage.put(partCf, jobKey, jobPayload);

        // ???????????????????????????????????????????????????????????????????????????
        if (i == 0) {
          executorService.execute(
              new WrapRunnable(
                  engine, partCf, jobKey, runnable, nodes, jobMsgPayloadBuilder, jobBuilder));
        }
      } else {
        engine.asyncSend(PooledBytesMessage.obtain().dest(dest).setArray(jobPayload));
      }
    }
  }
}
