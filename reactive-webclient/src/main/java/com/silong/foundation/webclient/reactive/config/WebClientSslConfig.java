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
package com.silong.foundation.webclient.reactive.config;

import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.Accessors;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * webclient SSL配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-02-01 14:40
 */
@Data
@NoArgsConstructor
@Accessors(fluent = true)
public class WebClientSslConfig {

  /** 默认协议协商配置 */
  public static final ApplicationProtocolConfig DEFAULT_APN =
      new ApplicationProtocolConfig(
          ApplicationProtocolConfig.Protocol.ALPN,
          ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
          ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
          ApplicationProtocolNames.HTTP_1_1,
          ApplicationProtocolNames.HTTP_2);

  /** 默认支持的SSL协议 */
  public static final String[] DEFAULT_SUPPORTED_PROTOCOLS = {"TLSv1.2", "TLSv1.3"};

  /** 默认支持的加密算法列表 */
  public static final List<String> DEFAULT_SUPPORTED_CIPHERS =
      List.of(
          "TLS_AES_256_GCM_SHA384",
          "TLS_AES_128_GCM_SHA256",
          "TLS_CHACHA20_POLY1305_SHA256",
          "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
          "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
          "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
          "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
          "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
          "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
          "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
          "TLS_DHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
          "TLS_DHE_DSS_WITH_AES_256_GCM_SHA384",
          "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
          "TLS_DHE_DSS_WITH_AES_128_GCM_SHA256",
          "TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384",
          "TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384",
          "TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256",
          "TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256",
          "TLS_RSA_WITH_AES_256_GCM_SHA384",
          "TLS_RSA_WITH_AES_128_GCM_SHA256");

  /** 是否信任所有证书，默认：true */
  private boolean trustAll = true;

  /** 握手超时，单位：毫秒，默认值：5000 */
  private long handshakeTimeoutMillis = 5000;

  /**
   * 是否启用ocsp，默认：false
   *
   * <pre>
   *     在线证书状态协议（OCSP）是一个互联网协议，用于获取符合X.509标准的数字证书的状态。
   *     该协议符合互联网标准规范，文档RFC6960对其进行了详细地描述。
   *     OCSP协议的产生是用于在公钥基础设施（PKI）体系中替代证书吊销列表（CRL）来查询数字证书的状态，
   *     OCSP克服了CRL的主要缺陷：必须经常在客户端下载以确保列表的更新。通过OCSP协议传输的消息使用ASN.1的语义进行编码。
   *     消息类型分为“请求消息”和“响应消息”，因此致OCSP服务器被称为OCSP响应端。
   *
   *     与CRL相比的优势：
   *     与CRL相比OCSP消息中信息内容更少，这能减少网络的负担和客户端的资源；
   *     由于OCSP响应端需要解析的信息更少，客户端提供的用于解析消息的库函数更简单；
   *     OCSP向发起响应方公开了一个特定的网络主机在特定时刻所使用的特定证书。由于OCSP并不强制加密该证书，因此信息可能被第三方拦截
   * </pre>
   */
  private boolean ocsp = false;

  /** 证书吊销列表路径 */
  private String crlPath;

  /** 是否开启startTls，默认：true<br> */
  private boolean startTls = true;

  /** 启用的TLS版本列表，默认：TLSv1.2，TLSv1.3 */
  @NotEmpty private String[] protocols = DEFAULT_SUPPORTED_PROTOCOLS;

  /** 支持的ciphers */
  @NotEmpty @Valid private List<@NotEmpty String> ciphers = DEFAULT_SUPPORTED_CIPHERS;

  /** keystore provider */
  private String keyStoreProvider;

  /** keystore 路径 */
  private String keyStorePath;

  /** keystore 类型 */
  private String keyStoreType;

  /** keystore 密码 */
  @ToString.Exclude private String keyStorePassword;

  /** truststore provider */
  private String trustStoreProvider;

  /** truststore 路径 */
  private String trustStorePath;

  /** truststore 类型 */
  private String trustStoreType;

  /** truststore 密码 */
  @ToString.Exclude private String trustStorePassword;

  /**
   * 构造方法
   *
   * @param config 配置
   */
  public WebClientSslConfig(@NonNull WebClientSslConfig config) {
    this.trustStorePath = config.trustStorePath;
    this.trustStoreType = config.trustStoreType;
    this.trustStoreProvider = config.trustStoreProvider;
    this.trustStorePassword = config.trustStorePassword;
    this.keyStorePassword = config.keyStorePassword;
    this.keyStoreType = config.keyStoreType;
    this.keyStorePath = config.keyStorePath;
    this.keyStoreProvider = config.keyStoreProvider;
    this.crlPath = config.crlPath;
    this.ciphers = config.ciphers;
    this.trustAll = config.trustAll;
    this.startTls = config.startTls;
    this.ocsp = config.ocsp;
    this.protocols = config.protocols;
    this.handshakeTimeoutMillis = config.handshakeTimeoutMillis;
  }
}
