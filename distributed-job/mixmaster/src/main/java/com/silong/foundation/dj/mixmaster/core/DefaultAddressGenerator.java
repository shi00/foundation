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

import com.silong.foundation.dj.mixmaster.configure.config.MixmasterProperties;
import com.silong.foundation.dj.mixmaster.exception.DistributedEngineException;
import com.silong.foundation.dj.mixmaster.message.Messages.MemberInfo;
import com.silong.foundation.dj.mixmaster.vo.ClusterMemberUUID;
import java.lang.management.ManagementFactory;
import lombok.extern.slf4j.Slf4j;
import org.jgroups.Address;
import org.jgroups.stack.AddressGenerator;
import org.springframework.stereotype.Component;
import oshi.SystemInfo;

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

  /** jvm启动时间 */
  private static final long STARTUP_TIME = ManagementFactory.getRuntimeMXBean().getStartTime();

  private static final SystemInfo SYSTEM_INFO = new SystemInfo();

  private static final String HOST_NAME =
      SYSTEM_INFO.getOperatingSystem().getNetworkParams().getHostName();

  /** 节点配置 */
  private final MixmasterProperties properties;

  /**
   * 构造方法
   *
   * @param properties 配置
   */
  public DefaultAddressGenerator(MixmasterProperties properties) {
    this.properties = properties;
  }

  /**
   * 优先从本地存储加载节点uuid，如果没有则生成，并附加节点信息
   *
   * @return 节点uuid
   */
  @Override
  public Address generateAddress() {
    try {
      // 更新节点附加信息
      return ClusterMemberUUID.random().memberInfo(buildMemberInfo());
    } catch (Exception e) {
      throw new DistributedEngineException(
          String.format(
              "Failed to generate uuid for member[%s:%s] of cluster.",
              HOST_NAME, properties.instanceName()),
          e);
    }
  }

  private MemberInfo buildMemberInfo() {
    return MemberInfo.newBuilder()
        .setClusterName(properties.clusterName())
        .setStartupTime(STARTUP_TIME)
        .setInstanceName(properties.instanceName())
        .build();
  }
}
