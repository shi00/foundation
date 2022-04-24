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
import com.silong.foundation.devastator.*;
import com.silong.foundation.devastator.config.DevastatorConfig;
import com.silong.foundation.devastator.exception.GeneralException;
import com.silong.foundation.devastator.model.Devastator.ClusterNodeInfo;
import com.silong.foundation.devastator.model.Devastator.ClusterState;
import com.silong.foundation.devastator.model.Devastator.IpAddressInfo;
import com.silong.foundation.devastator.utils.KryoUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.jgroups.*;
import org.jgroups.protocols.TP;
import org.jgroups.protocols.UDP;

import java.io.*;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

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
    implements Devastator, Receiver, ChannelListener, Serializable {

  @Serial private static final long serialVersionUID = 0L;

  /** 分区到节点映射关系，Collection中的第一个节点为primary，后续为backup */
  private final Map<Integer, Collection<ClusterNode>> partition2ClusterNodes =
      new ConcurrentHashMap<>();

  /** 集群通信信道 */
  private final JChannel jChannel;

  /** 配置 */
  private final DevastatorConfig config;

  /** 数据分配器 */
  private final RendezvousAllocator allocator;

  /** 持久化存储 */
  private final PersistStorage persistStorage;

  /** 集群状态 */
  private ClusterState clusterState;

  /** 集群节点列表 */
  private Collection<ClusterNode> clusterNodes;

  /**
   * 构造方法
   *
   * @param config 引擎配置
   */
  public DefaultDistributedEngine(DevastatorConfig config) {
    if (config == null) {
      throw new IllegalArgumentException("config must not be null.");
    }
    try (InputStream inputStream = requireNonNull(locateConfig(config.configFile())).openStream()) {
      this.config = config;
      this.allocator = new RendezvousAllocator(config.partitionCount());
      this.persistStorage = new RocksDbPersistStorage(config.persistStorageConfig());
      this.jChannel = new JChannel(inputStream);
      this.jChannel.setReceiver(this);
      this.jChannel.addAddressGenerator(this::buildClusterNodeInfo);
      this.jChannel.setName(config.instanceName());
      this.jChannel.setDiscardOwnMessages(true);
      this.jChannel.connect(config.clusterName());

      // 获取集群状态
      this.jChannel.getState();
    } catch (Exception e) {
      throw new GeneralException("Failed to start distributed engine.", e);
    }
  }

  private ClusterNodeUUID buildClusterNodeInfo() {
    TP transport = getTransport();
    return ClusterNodeUUID.random()
        .clusterNodeInfo(
            ClusterNodeInfo.newBuilder()
                .setJgVersion(Version.version)
                .putAllAttributes(convert(config.clusterNodeAttributes()))
                .setInstanceName(config.instanceName())
                .setHostName(SystemUtils.getHostName())
                .setRole(config.clusterNodeRole().getValue())
                .setBindPort(getBindPort(transport))
                .addAllAddresses(getLocalAllAddresses(transport))
                .build());
  }

  private TP getTransport() {
    return jChannel.getProtocolStack().getTransport();
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

  @SneakyThrows
  private Collection<IpAddressInfo> getLocalAllAddresses(TP transport) {
    List<InetAddress> list = new LinkedList<>();
    Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
    while (interfaces.hasMoreElements()) {
      NetworkInterface networkInterface = interfaces.nextElement();
      Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
      while (inetAddresses.hasMoreElements()) {
        list.add(inetAddresses.nextElement());
      }
    }
    InetAddress bindAddress = getBindAddress(transport);
    return list.stream()
        .map(
            inetAddress ->
                IpAddressInfo.newBuilder()
                    .setBind(inetAddress.equals(bindAddress))
                    .setIpAddress(ByteString.copyFrom(inetAddress.getAddress()))
                    .build())
        .toList();
  }

  private InetAddress getBindAddress(TP transport) {
    return transport.getClass() == UDP.class
        ? ((UDP) transport).getMulticastAddress()
        : transport.getBindAddress();
  }

  private int getBindPort(TP transport) {
    return transport.getClass() == UDP.class
        ? ((UDP) transport).getMulticastPort()
        : transport.getBindPort();
  }

  private Map<String, ByteString> convert(Map<String, Object> attributes) {
    Map<String, ByteString> ret = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : attributes.entrySet()) {
      ret.put(entry.getKey(), ByteString.copyFrom(KryoUtils.serialize(entry.getValue())));
    }
    return ret;
  }

  @Override
  public void receive(Message msg) {
    Receiver.super.receive(msg);
  }

  @Override
  public void viewAccepted(View newView) {
    if (newView instanceof MergeView) {

    } else {
      // 获取集群节点列表
      clusterNodes = newView.getMembers().stream().map(this::buildClusterNode).toList();
      // 根据集群节点调整分区分布
      IntStream.range(0, config.partitionCount())
          .parallel()
          .forEach(
              partitionNo ->
                  partition2ClusterNodes.put(
                      partitionNo,
                      allocator.allocatePartition(
                          partitionNo, config.backupNums(), clusterNodes, null)));
    }
  }

  private ClusterNode buildClusterNode(Address address) {
    TP transport = getTransport();
    return new DefaultClusterNode(
        (ClusterNodeUUID) address, getBindAddress(transport).getAddress(), getBindPort(transport));
  }

  @Override
  public void getState(OutputStream output) throws Exception {
    clusterState =
        ClusterState.newBuilder()
            .setPartitions(config.partitionCount())
            .setClusterName(config.clusterName())
            .build();
    clusterState.writeTo(output);
  }

  @Override
  public void setState(InputStream input) throws Exception {
    clusterState = ClusterState.parseFrom(input);
  }

  @Override
  public String name() {
    return config.instanceName();
  }

  @Override
  public DevastatorConfig config() {
    return config;
  }

  @Override
  public Cluster cluster() {
    return null;
  }

  @Override
  public DistributedJobScheduler scheduler(String name) {
    return null;
  }

  @Override
  public void close() {
    if (jChannel != null) {
      jChannel.close();
    }
    partition2ClusterNodes.clear();
  }
}
