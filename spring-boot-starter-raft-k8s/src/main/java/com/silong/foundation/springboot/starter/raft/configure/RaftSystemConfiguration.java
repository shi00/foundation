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

package com.silong.foundation.springboot.starter.raft.configure;

import com.silong.foundation.springboot.starter.raft.configure.config.RaftConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;

/**
 * Raft系统配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-10-22 19:41
 */
@Configuration
@EnableWebFlux
@EnableConfigurationProperties(RaftConfig.class)
public class RaftSystemConfiguration {

  private RaftConfig raftConfig;

  @Autowired
  public void setRaftConfig(RaftConfig raftConfig) {
    this.raftConfig = raftConfig;
  }
}
