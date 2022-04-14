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

import com.silong.foundation.devastator.Cluster;
import com.silong.foundation.devastator.DistributedEngine;
import com.silong.foundation.devastator.DistributedJobScheduler;
import com.silong.foundation.devastator.allocator.RendezvousAllocator;
import com.silong.foundation.devastator.config.DistributedEngineConfig;
import com.silong.foundation.devastator.config.DistributedJobSchedulerConfig;
import com.silong.foundation.devastator.exception.GeneralException;
import com.silong.foundation.devastator.protobuf.Devastator;
import com.silong.foundation.devastator.protobuf.Devastator.ClusterNodeInfo;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.jgroups.*;
import org.jgroups.protocols.TP;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

import static java.util.Objects.requireNonNull;

/**
 * 基于jgroups的分布式任务引擎
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-10 00:30
 */
@Slf4j
@SuppressFBWarnings({"PATH_TRAVERSAL_IN", "URLCONNECTION_SSRF_FD"})
public class DefaultDistributedEngine
    implements DistributedEngine, Receiver, ChannelListener, Serializable {

  @Serial private static final long serialVersionUID = 0L;

  /** 集群通信信道 */
  private final JChannel jChannel;

  /** 配置 */
  private final DistributedJobSchedulerConfig config;

  /** 数据分配器 */
  private final RendezvousAllocator allocator;

  /** 集群视图 */
  private View lastView;

  /**
   * 构造方法
   *
   * @param config 引擎配置
   */
  public DefaultDistributedEngine(@NonNull DistributedJobSchedulerConfig config) {
    try (InputStream inputStream = requireNonNull(locateConfig(config.distributedEngineConfig().configFile())).openStream()) {
      this.config = config;
      this.allocator = new RendezvousAllocator(config.partitionCount());
      this.jChannel = new JChannel(inputStream);
      this.jChannel.setReceiver(this);
      this.jChannel.addAddressGenerator(() -> buildClusterNodeInfo(config));
      this.jChannel.setName(config.instanceName());
      this.jChannel.connect(config.clusterName());
      this.jChannel.getState(null,);
    } catch (Exception e) {
      throw new GeneralException("Failed to start distributed engine.", e);
    }
  }

  private ClusterNodeUUID buildClusterNodeInfo(DistributedEngineConfig config) {
    TP transport = jChannel.getProtocolStack().getTransport();
    return ClusterNodeUUID.random()
        .clusterNodeInfo(
            ClusterNodeInfo.newBuilder()
                .setJgroupsVersion(Version.version)
                .setPort(transport.getBindPort())
                .setIpAddress(transport.getBindAddress().getHostAddress())
                .setHostName(SystemUtils.getHostName())
                .build());
  }

  private URL locateConfig(String confFile) throws Exception {
    try {
      return new URL(confFile);
    } catch (MalformedURLException e) {
      File file = new File(confFile);
      if (file.exists() && file.isFile()) {
        return file.toURI().toURL();
      } else {
        return getClass().getClassLoader().getResource(confFile);
      }
    }
  }

  @Override
  public void receive(Message msg) {
    Receiver.super.receive(msg);
  }

  @Override
  public void viewAccepted(View newView) {
    if (lastView == null) {

      newView.getMembers().stream().filter(address -> ((ClusterNodeUUID)address).clusterNodeInfo().getRole()& Devastator.ClusterNodeRole.WORKER==0)

    } else {

    }
    lastView = newView;
  }

  @Override
  public void getState(OutputStream output) throws Exception {
    Receiver.super.getState(output);
  }

  @Override
  public void setState(InputStream input) throws Exception {
    Receiver.super.setState(input);
  }

  @Override
  public String name() {
    return config.instanceName();
  }

  @Override
  public DistributedEngineConfig config() {
    return config;
  }

  @Override
  public Cluster cluster() {
    return null;
  }

  @Override
  public DistributedJobScheduler scheduler(DistributedJobSchedulerConfig config) {
    return null;
  }

  @Override
  public void close() {
    if (jChannel != null) {
      jChannel.close();
    }
  }
}
