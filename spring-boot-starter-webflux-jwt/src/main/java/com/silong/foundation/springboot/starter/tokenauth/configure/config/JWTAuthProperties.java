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

package com.silong.foundation.springboot.starter.tokenauth.configure.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.Data;
import org.hibernate.validator.constraints.Range;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

/**
 * JWT鉴权配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-03 11:31
 */
@Data
@Validated
@ConfigurationProperties(prefix = "jwt-auth")
@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "Read-only initial configuration")
public class JWTAuthProperties {

  /** token超时时长，默认：3600秒，如果为0表示永不超时 */
  @PositiveOrZero private int tokenTimeout = (int) TimeUnit.HOURS.toSeconds(1);

  /** 单个节点可容纳的最大token数量 */
  @Positive private int perNodeMaxTokenSize;

  /** 使用HmacSha256签名使用的密钥 */
  @NotEmpty private String workKey;

  /** token签名密钥 */
  @NotEmpty private String signKey;

  /** 无需鉴权请求路径名单 */
  @Valid @NotEmpty private Set<@NotEmpty String> whiteList;

  /** 需鉴权请求路径名单 */
  @Valid @NotEmpty private Set<@NotEmpty String> authList;

  /** 用户到角色映射 */
  @Valid @NotEmpty
  private Map<@NotEmpty String, @NotEmpty @Valid Set<@NotEmpty String>> userRolesMappings;

  /** 角色到请求路径映射 */
  @Valid @NotEmpty
  private Map<@NotEmpty String, @NotEmpty @Valid Set<@NotEmpty String>> rolePathsMappings;

  /** hazelcast配置 */
  @Valid @NestedConfigurationProperty
  private HazelcastConfig hazelcastConfig = new HazelcastConfig();

  @Data
  public static class HazelcastConfig {

    /** hazelcast监听地址 */
    @NotEmpty private String address;

    /** hazelcast监听端口号，取值：[1025，41951] */
    @Range(min = 1025, max = 41951)
    private int port;

    @NotEmpty private String clusterName;

    @NestedConfigurationProperty @Valid
    private MulticastConfig multicastConfig = new MulticastConfig();

    @Data
    public static class MulticastConfig {
      /** 是否启用组播查找集群成员 */
      private boolean enable;

      /** 组播组 */
      @NotEmpty private String group;

      /** 组播发现成员超时时间，单位：秒 */
      @Positive private int timeout;

      /** 组播包存活时间，取值：[0,255] */
      @Range(min = 0, max = 255)
      private int timeToLive;

      /** 组播端口号，取值：[1025，41951] */
      @Range(min = 1025, max = 41951)
      private int port;
    }
  }
}
