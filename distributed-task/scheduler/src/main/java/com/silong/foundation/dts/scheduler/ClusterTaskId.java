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
package com.silong.foundation.dts.scheduler;

import com.silong.foundation.dts.protobuf.DTSModels;
import lombok.Builder;
import lombok.Data;
import org.jgroups.Address;
import org.jgroups.util.Streamable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * 集群任务id
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-05 20:05
 */
public class ClusterTaskId implements Streamable {

  /** 集群任务id */
  private long id;

  /** 主机地址 */
  private Address address;

  @Override
  public void writeTo(DataOutput out) throws IOException {
    DTSModels.ClusterTaskId taskId = DTSModels.ClusterTaskId.newBuilder().setId(id).build();
  }

  @Override
  public void readFrom(DataInput in) throws IOException, ClassNotFoundException {}
}
