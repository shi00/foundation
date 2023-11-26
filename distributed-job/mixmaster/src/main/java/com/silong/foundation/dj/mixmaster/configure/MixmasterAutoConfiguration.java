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

package com.silong.foundation.dj.mixmaster.configure;

import com.auth0.jwt.algorithms.Algorithm;
import com.silong.foundation.crypto.RootKey;
import com.silong.foundation.crypto.aes.AesGcmToolkit;
import com.silong.foundation.dj.hook.auth.JwtAuthenticator;
import com.silong.foundation.dj.hook.auth.SimpleJwtAuthenticator;
import com.silong.foundation.dj.hook.clock.HybridLogicalClock;
import com.silong.foundation.dj.hook.clock.LogicalClock;
import com.silong.foundation.dj.longhaul.RocksDbPersistStorage;
import com.silong.foundation.dj.longhaul.config.PersistStorageProperties;
import com.silong.foundation.dj.mixmaster.configure.config.MixmasterProperties;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.stream.IntStream;
import org.jctools.queues.atomic.MpscAtomicArrayQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 集群管理系统自动配置器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-24 19:00
 */
@Configuration
@EnableConfigurationProperties(MixmasterProperties.class)
public class MixmasterAutoConfiguration {

  static {
    RootKey.initialize();
  }

  /** 配置 */
  private MixmasterProperties properties;

  /**
   * jwt 鉴权处理器
   *
   * @return 鉴权处理器
   */
  @Bean
  @ConditionalOnMissingBean
  public JwtAuthenticator jwtAuthenticator() {
    return SimpleJwtAuthenticator.builder()
        // 设置签名密钥
        .signatureAlgorithm(
            Algorithm.HMAC256(
                AesGcmToolkit.decrypt(
                    properties.getAuth().getSignKey(), properties.getAuth().getWorkKey())))
        // 设置超期时间
        .period(properties.getAuth().getExpires())
        .build();
  }

  @Bean
  @ConditionalOnMissingBean
  public LogicalClock logicalClock() {
    return new HybridLogicalClock();
  }

  @Bean
  public MpscAtomicArrayQueue<ApplicationEvent> mixmasterEventQueue() {
    return new MpscAtomicArrayQueue<>(properties.getEventDispatchQueueSize());
  }

  /**
   * 根据分区号生成存储列族名称
   *
   * @param partitionNo 分区号
   * @return 保存任务数据使用的列族名
   */
  private String getPartitionCf(int partitionNo) {
    return String.format("%s-partition%d", properties.getClusterName(), partitionNo);
  }

  /**
   * 注册持久化存储
   *
   * @return 持久化存储
   */
  @Bean
  @ConditionalOnMissingBean
  public RocksDbPersistStorage mixmasterPersistStorage() {
    // 把分区都创建好column family
    PersistStorageProperties longHaul = properties.getLongHaul();
    Collection<String> columnFamilyNames = longHaul.getColumnFamilyNames();
    LinkedHashSet<String> cfs = new LinkedHashSet<>(columnFamilyNames);
    IntStream.range(0, properties.getPartitions())
        .forEach(partition -> cfs.add(getPartitionCf(partition)));
    longHaul.setColumnFamilyNames(cfs);
    return new RocksDbPersistStorage(longHaul);
  }

  @Autowired
  public void setProperties(MixmasterProperties properties) {
    this.properties = properties;
  }
}
