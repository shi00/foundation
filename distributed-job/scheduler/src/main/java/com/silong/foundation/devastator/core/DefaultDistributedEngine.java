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
import com.silong.foundation.devastator.config.DistributedEngineConfig;
import com.silong.foundation.devastator.config.DistributedJobSchedulerConfig;
import com.silong.foundation.devastator.exception.GeneralException;
import com.silong.foundation.devastator.protobuf.Devastator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.Receiver;
import org.jgroups.View;
import org.jgroups.util.ExtendedUUID;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serial;
import java.net.MalformedURLException;
import java.net.URL;

import static com.silong.foundation.devastator.utils.ExtendedUUIDKeys.HOSTNAME_KEY;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.SystemUtils.getHostName;

/**
 * 基于jgroups的分布式任务引擎
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-10 00:30
 */
@Slf4j
@SuppressFBWarnings({"PATH_TRAVERSAL_IN", "URLCONNECTION_SSRF_FD"})
public class DefaultDistributedEngine implements DistributedEngine, Receiver {

  @Serial private static final long serialVersionUID = 0L;

  /** 集群通信信道 */
  private final JChannel jChannel;

  /** 配置 */
  private final DistributedEngineConfig config;

  /** 集群视图 */
  private View view;

  /**
   * 构造方法
   *
   * @param config 引擎配置
   */
  public DefaultDistributedEngine(@NonNull DistributedEngineConfig config) {
    this.config = config;
    try (InputStream inputStream = requireNonNull(locateConfig(config.configFile())).openStream()) {
      this.jChannel = new JChannel(inputStream);
      this.jChannel.setReceiver(this);
      this.jChannel.addAddressGenerator(
          () ->
              ClusterNodeUUID.random()
                  .clusterNodeInfo(
                      Devastator.ClusterNodeInfo.newBuilder()
                          .setHostName(SystemUtils.getHostName())
                          .build()));

      this.jChannel.setName(config.instanceName());
      this.jChannel.connect(config.clusterName());
    } catch (Exception e) {
      throw new GeneralException("Failed to start distributed engine.", e);
    }
  }

  private URL locateConfig(String confFile) throws Exception {
    try {
      return new URL(confFile);
    } catch (MalformedURLException e) {
      File file = new File(confFile);
      if (file.exists() && file.isFile()) {
        return file.toURI().toURL();
      } else {
        return getClass().getResource(confFile);
      }
    }
  }

  @Override
  public void receive(Message msg) {
    Receiver.super.receive(msg);
  }

  @Override
  public void viewAccepted(View newView) {}

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
