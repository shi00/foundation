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

import com.silong.foundation.devastator.PersistStorage;
import com.silong.foundation.devastator.config.DevastatorConfig;
import com.silong.foundation.devastator.config.DevastatorProperties;
import com.silong.foundation.devastator.exception.DistributedEngineException;
import com.silong.foundation.devastator.model.Devastator;
import lombok.extern.slf4j.Slf4j;
import org.jgroups.Address;
import org.jgroups.Version;
import org.jgroups.stack.AddressGenerator;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import static com.silong.foundation.devastator.core.ClusterNodeUUID.deserialize;
import static com.silong.foundation.devastator.model.Devastator.ClusterNodeInfo.newBuilder;
import static com.silong.foundation.devastator.utils.TypeConverter.STRING_TO_BYTES;
import static org.apache.commons.lang3.SystemUtils.getHostName;

/**
 * 默认集群节点地址生成器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-05-01 09:16
 */
@Slf4j
public class DefaultAddressGenerator implements AddressGenerator {

  /** 持久化存储 */
  private final PersistStorage persistStorage;

  /** 节点配置 */
  private final DevastatorConfig config;

  /**
   * 构造方法
   *
   * @param config 配置
   * @param persistStorage 持久化存储
   */
  public DefaultAddressGenerator(DevastatorConfig config, PersistStorage persistStorage) {
    if (config == null) {
      throw new IllegalArgumentException("config must not be null.");
    }
    if (persistStorage == null) {
      throw new IllegalArgumentException("persistStorage must not be null.");
    }
    this.persistStorage = persistStorage;
    this.config = config;
  }

  /**
   * 构造节点持久化key
   *
   * @return key
   */
  private String buildClusterNodeUuidKey() {
    return String.format(
        "%s:%s:%s:node:uuid", config.clusterName(), getHostName(), config.instanceName());
  }

  private Devastator.ClusterNodeInfo buildClusterNodeInfo() throws SocketException {
    RuntimeMXBean mxbean = ManagementFactory.getRuntimeMXBean();
    return newBuilder()
        .setJgVersion(Version.version)
        .setPid(mxbean.getPid())
        .setClusterName(config.clusterName())
        .setStartTime(mxbean.getStartTime())
        .setJvmInfo(buildJvmInfo(mxbean))
        .setHardwareInfo(buildHardwareInfo())
        .setDevastatorVersion(DevastatorProperties.version().getVersion())
        .putAllAttributes(config.clusterNodeAttributes())
        .setInstanceName(config.instanceName())
        .setHostName(getHostName())
        .setRole(config.clusterNodeRole().getValue())
        .addAllIpAddresses(getLocalAllAddresses())
        .build();
  }

  private Devastator.HardwareInfo.Builder buildHardwareInfo() {
    Runtime runtime = Runtime.getRuntime();
    return Devastator.HardwareInfo.newBuilder()
        .setAvailableProcessors(runtime.availableProcessors())
        .setTotalMemory(runtime.totalMemory());
  }

  private Devastator.JvmInfo.Builder buildJvmInfo(RuntimeMXBean mxbean) {
    return Devastator.JvmInfo.newBuilder()
        .setVmVendor(mxbean.getVmVendor())
        .setVmVersion(mxbean.getVmVersion())
        .setVmName(mxbean.getVmName())
        .setClassPath(String.join(";", mxbean.getClassPath()))
        .setVmArgs(String.join(" ", mxbean.getInputArguments()));
  }

  private Collection<String> getLocalAllAddresses() throws SocketException {
    List<InetAddress> list = new LinkedList<>();
    Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
    while (interfaces.hasMoreElements()) {
      NetworkInterface networkInterface = interfaces.nextElement();
      Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
      while (inetAddresses.hasMoreElements()) {
        list.add(inetAddresses.nextElement());
      }
    }
    return list.stream().map(InetAddress::getHostAddress).toList();
  }

  /**
   * 优先从本地存储加载节点uuid，如果没有则生成，并附加节点信息
   *
   * @return 节点uuid
   */
  @Override
  public Address generateAddress() {
    try {
      ClusterNodeUUID uuid;
      byte[] key = STRING_TO_BYTES.to(buildClusterNodeUuidKey());
      byte[] value = persistStorage.get(key);
      if (value != null) {
        uuid = deserialize(value);
      } else {
        uuid = ClusterNodeUUID.random();

        // 保存uuid
        persistStorage.put(key, uuid.serialize());
      }
      // 更新节点附加信息
      return uuid.clusterNodeInfo(buildClusterNodeInfo());
    } catch (Exception e) {
      throw new DistributedEngineException(
          "Failed to generate ClusterNodeUUID for node"
              + String.format("(%s:%s)", getHostName(), config.instanceName()),
          e);
    }
  }
}
