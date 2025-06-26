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

package com.silong.llm.chatbot.configure.properties;

import static java.util.Locale.ROOT;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

/**
 * Ldap配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-19 20:24
 */
@Data
@Validated
@ConfigurationProperties(prefix = "ldap")
public class LdapProperties {

  /** 对接的服务器类型 */
  @NotNull private Type type;

  /** LDAP URLs of the server. */
  @NotNull @NotEmpty private String[] urls;

  /** Base suffix from which all operations should originate. */
  @NotEmpty private String baseDn;

  /** Login username of the server. */
  @NotEmpty private String username;

  /** Login password of the server. */
  @NotEmpty private String password;

  /** 是否启用startTLS，默认：false */
  private boolean useStartTls;

  /**
   * Specify how referrals encountered by the service provider are to be processed. If not
   * specified, the default is true.
   */
  private boolean referralFollow = true;

  /** referrals follow 跳转最大次数，默认：3 */
  @Positive private int referralFollowLimit = 3;

  /** 开启StartTLS或LDAPS时启用的协议，默认：TLSv1.3, TLSv1.2 */
  @NotNull private String[] tlsProtocols = {"TLSv1.3", "TLSv1.2"};

  /**
   * 开启StartTLS或LDAPS时启用的加密算法， 默认：TLS_AES_256_GCM_SHA384, TLS_AES_128_GCM_SHA256,
   * TLS_CHACHA20_POLY1305_SHA256, TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,
   * TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256, TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
   * TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
   */
  @NotNull
  private String[] tlsCipherSuites = {
    // TLS 1.3 套件（优先使用）
    "TLS_AES_256_GCM_SHA384",
    "TLS_AES_128_GCM_SHA256",
    "TLS_CHACHA20_POLY1305_SHA256",

    // TLS 1.2 套件（向后兼容）
    "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
    "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
    "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
    "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"
  };

  /** 连接池配置 */
  @Valid @NestedConfigurationProperty private Pool pool = new Pool();

  public enum Type {
    ACTIVE_DIRECTORY,
    LDAP
  }

  /** 连接池配置 */
  @Data
  public static class Pool {
    /** 最大连接数 */
    @Positive private int maxConnections;

    /** 最小连接数 */
    @Positive private int minConnections;

    /** 连接超时时长 */
    @NotNull private Duration connectionTimeout;

    /** 响应超时时长 */
    @NotNull private Duration responseTimeout;
  }

  /**
   * 是否启用ldaps
   *
   * @return true or false
   */
  public boolean isSecure() {
    if (urls == null || urls.length == 0) {
      throw new IllegalArgumentException("urls is null or empty.");
    }
    boolean ldap = Arrays.stream(urls).allMatch(url -> url.toLowerCase(ROOT).startsWith("ldap"));
    if (!ldap) {
      long count =
          Arrays.stream(urls).filter(url -> url.toLowerCase(ROOT).startsWith("ldap")).count();
      if (count == 0) {
        return true;
      } else {
        throw new IllegalArgumentException(
            "It is not allowed to include both ldap and ldaps in urls. urls: "
                + Arrays.stream(urls).collect(Collectors.joining(", ", "[", "]")));
      }
    }
    return false;
  }
}
