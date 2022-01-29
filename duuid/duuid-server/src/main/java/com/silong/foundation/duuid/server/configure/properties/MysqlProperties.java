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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * 基于mysql的workerId分配器配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-11 22:42
 */
@Data
@Validated
@ConfigurationProperties(prefix = "duuid.worker-id-provider.mysql")
@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "只读初始配置")
public class MysqlProperties {
  /** 是否启用 */
  private boolean enabled;

  /** jdbc url */
  private String jdbcUrl = EMPTY;

  /** jdbc驱动 */
  private String jdbcDriver = EMPTY;

  /** 用户名 */
  private String userName = EMPTY;

  /** 密码 */
  private String password = EMPTY;
}
