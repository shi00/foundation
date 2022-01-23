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
package com.silong.foundation.springboot.starter.simpleauth.configure.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Positive;
import java.util.Map;
import java.util.Set;

/**
 * 服务配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-03 11:31
 */
@Data
@Validated
@ConfigurationProperties(prefix = "simple-auth")
public class SimpleAuthProperties {

  /** 客户端和服务端可接受的时间差，默认：10000毫秒 */
  @Positive private int acceptableTimeDiffMills = 10000;

  /** 使用HmacSha256签名使用的密钥 */
  @NotEmpty private String workKey;

  /** 无需鉴权请求路径名单 */
  @Valid @NotEmpty private Set<@NotEmpty String> whiteList;

  /** 需鉴权请求路径名单 */
  @Valid @NotEmpty private Set<@NotEmpty String> authList;

  /** 用户到角色映射 */
  @Valid @NotEmpty
  private Map<@NotEmpty String, @Valid @NotEmpty Set<@NotEmpty String>> userRolesMappings;

  /** 角色到请求路径映射 */
  @Valid @NotEmpty
  private Map<@NotEmpty String, @NotEmpty @Valid Set<@NotEmpty String>> rolePathsMappings;
}
