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
package com.silong.foundation.duuid.server.configure.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;

import static org.apache.commons.lang3.SystemUtils.getHostName;

/**
 * 普罗米修斯配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-02-01 11:16
 */
@Data
@Validated
@ConfigurationProperties(prefix = "duuid.server.prometheus")
public class PrometheusProperties {
  /** 定制百分位 */
  @NotEmpty private double[] percentiles;

  /** slo定制，单位：纳秒 */
  @NotEmpty private double[] slo;

  /** 服务部署区域 */
  @NotEmpty private String region = "default-region";

  /** 服务部署数据中心 */
  @NotEmpty private String dataCenter = "default-dc";

  /** 云服务提供商 */
  @NotEmpty private String cloudProvider = "default";

  /** 主机名 */
  @NotEmpty private String host = getHostName();
}
