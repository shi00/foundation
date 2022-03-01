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
package com.silong.foundation.springboot.starter.okhttp3.configure.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;

import static com.silong.foundation.springboot.starter.okhttp3.Constants.TLS_13;

/**
 * webclient SSL配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-02-01 14:40
 */
@Data
@Validated
@NoArgsConstructor
@Accessors(fluent = true)
@ConfigurationProperties(prefix = "okhttp3.webclient.ssl")
public class WebClientSslConfig {

  /** 是否信任所有证书，默认：true */
  private boolean trustAll = true;

  /** 启用的TLS版本列表，默认：TLSv1.3 */
  @NotEmpty private String protocol = TLS_13;

  /** keystore 路径 */
  private String keyStorePath;

  /** keystore 类型 */
  private String keyStoreType;

  /** keystore 密码 */
  @ToString.Exclude private String keyStorePassword;

  /** truststore 路径 */
  private String trustStorePath;

  /** truststore 类型 */
  private String trustStoreType;

  /** truststore 密码 */
  @ToString.Exclude private String trustStorePassword;
}
