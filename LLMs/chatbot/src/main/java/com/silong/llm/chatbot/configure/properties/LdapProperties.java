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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
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

  /** 连接池配置 */
  @Valid @NestedConfigurationProperty private Pool pool = new Pool();

  /**
   * Whether read-only operations should use an anonymous environment. Disabled by default unless a
   * username is set.
   */
  private boolean anonymousReadOnly;

  /**
   * Specify how referrals encountered by the service provider are to be processed. If not
   * specified, the default is determined by the provider.
   */
  private Referral referral;

  /** Define the methods to handle referrals. */
  public enum Referral {

    /** Follow referrals automatically. */
    FOLLOW,

    /** Ignore referrals. */
    IGNORE,

    /** Throw when a referral is encountered. */
    THROW
  }

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
    return Arrays.stream(urls).allMatch(url -> url.toLowerCase(Locale.ROOT).startsWith("ldaps"));
  }
}
