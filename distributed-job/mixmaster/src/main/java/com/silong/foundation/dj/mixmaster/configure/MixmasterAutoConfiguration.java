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

import com.silong.foundation.dj.mixmaster.configure.config.MixmasterProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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

  /** 配置 */
  private MixmasterProperties properties;

  @Autowired
  public void setProperties(MixmasterProperties properties) {
    this.properties = properties;
  }
}