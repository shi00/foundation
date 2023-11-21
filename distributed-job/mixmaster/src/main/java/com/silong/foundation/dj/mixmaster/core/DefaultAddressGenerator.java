/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */
package com.silong.foundation.dj.mixmaster.core;

import static com.google.protobuf.UnsafeByteOperations.unsafeWrap;
import static com.silong.foundation.dj.mixmaster.utils.SystemInfo.*;
import static com.silong.foundation.dj.scrapper.utils.String2Bytes.INSTANCE;

import com.google.protobuf.ByteString;
import com.silong.foundation.dj.mixmaster.configure.config.MixmasterProperties;
import com.silong.foundation.dj.mixmaster.exception.DistributedEngineException;
import com.silong.foundation.dj.mixmaster.generated.Messages.ClusterNodeInfo;
import com.silong.foundation.dj.mixmaster.generated.Messages.Host;
import com.silong.foundation.dj.mixmaster.generated.Messages.IpAddress;
import com.silong.foundation.dj.mixmaster.vo.ClusterNodeUUID;
import com.silong.foundation.dj.scrapper.PersistStorage;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jgroups.Address;
import org.jgroups.stack.AddressGenerator;
import org.springframework.stereotype.Component;

/**
 * 默认集群节点地址生成器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-05-01 09:16
 */
@Slf4j
@Component
class DefaultAddressGenerator implements AddressGenerator {

  /** 节点配置 */
  private final MixmasterProperties properties;

  /** 持久化存储 */
  private final PersistStorage persistStorage;

  /**
   * 构造方法
   *
   * @param properties 配置
   */
  public DefaultAddressGenerator(
      @NonNull MixmasterProperties properties, @NonNull PersistStorage persistStorage) {
    this.properties = properties;
    this.persistStorage = persistStorage;
  }

  /**
   * 构造节点持久化key
   *
   * @return key
   */
  private String buildClusterNodeUuidKey() {
    return String.format("%s:node:uuid", HARDWARE_UUID);
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
      byte[] key = INSTANCE.to(buildClusterNodeUuidKey());
      byte[] value = persistStorage.get(key);
      if (value != null) {
        uuid = ClusterNodeUUID.deserialize(value);
        log.info("Found the uuid({}) of the local node.", uuid);
      } else {
        // 保存uuid
        uuid = ClusterNodeUUID.random();
        persistStorage.put(key, uuid.serialize());
        log.info("Unable to find uuid of local node, generate a new one. generatedUUID: {}", uuid);
      }

      // 更新节点附加信息
      return uuid.clusterNodeInfo(buildClusterNodeInfo());
    } catch (Exception e) {
      throw new DistributedEngineException(
          "Failed to generate ClusterNodeUUID for node"
              + String.format("(%s:%s)", HOST_NAME, properties.getInstanceName()),
          e);
    }
  }

  private ClusterNodeInfo buildClusterNodeInfo() {
    return ClusterNodeInfo.newBuilder()
        .setStartupTime(ManagementFactory.getRuntimeMXBean().getStartTime())
        .setInstanceName(properties.getInstanceName())
        .setClusterName(properties.getClusterName())
        .putAllAttributes(getNodeAttributes())
        .setHost(
            Host.newBuilder()
                .setName(HOST_NAME)
                .setOsName(OS_NAME)
                .setTotalMemory(Runtime.getRuntime().totalMemory())
                .setAvailableProcessors(Runtime.getRuntime().availableProcessors())
                .setPid(ManagementFactory.getRuntimeMXBean().getPid())
                .setDataPlaneAddress(
                    IpAddress.newBuilder()
                        .setPort(properties.getDataPlaneAddress().getPort())
                        .setIpAddress(properties.getDataPlaneAddress().getIpAddress()))
                .setManagementPlaneAddress(
                    IpAddress.newBuilder()
                        .setPort(properties.getManagerPlaneAddress().getPort())
                        .setIpAddress(properties.getManagerPlaneAddress().getIpAddress())))
        .build();
  }

  private Map<String, ByteString> getNodeAttributes() {
    return properties.getClusterNodeAttributes().entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> unsafeWrap(INSTANCE.to(e.getValue()))));
  }
}
