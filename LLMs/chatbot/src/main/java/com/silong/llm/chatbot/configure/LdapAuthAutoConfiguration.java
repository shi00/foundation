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

package com.silong.llm.chatbot.configure;

import com.silong.llm.chatbot.configure.properties.LdapProperties;
import com.silong.llm.chatbot.provider.LdapUserProvider;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.extensions.StartTLSExtendedRequest;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置ldap鉴权
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-19 20:21
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(LdapProperties.class)
@ConditionalOnProperty(
    prefix = "ldap",
    value = {"urls"})
public class LdapAuthAutoConfiguration {

  private final LdapProperties ldapProperties;

  /**
   * 构造方法
   *
   * @param ldapProperties 配置
   */
  public LdapAuthAutoConfiguration(@NonNull LdapProperties ldapProperties) {
    this.ldapProperties = ldapProperties;
    SSLUtil.setEnabledSSLProtocols(Arrays.asList(ldapProperties.getTlsProtocols()));
    SSLUtil.setEnabledSSLCipherSuites(Arrays.asList(ldapProperties.getTlsCipherSuites()));
  }

  @Bean
  LdapUserProvider ldapUserProvider(LDAPConnectionPool ldapConnectionPool) {
    return new LdapUserProvider(ldapProperties, ldapConnectionPool);
  }

  @Bean(destroyMethod = "close")
  LDAPConnectionPool ldapConnectionPool() throws GeneralSecurityException, LDAPException {
    LdapProperties.Pool pool = ldapProperties.getPool();

    // 是否启用StartTLS
    if (ldapProperties.isUseStartTls()) {
      String[] urls = ldapProperties.getUrls();
      if (ldapProperties.isSecure()) {
        throw new IllegalArgumentException(
            String.format(
                "Failed to use startTLS, invalid ldap.urls: [%s].", String.join(", ", urls)));
      }

      // Configure an SSLUtil instance and use it to obtain an SSLContext.
      SSLUtil sslUtil = getTrustAllSslUtil();
      SSLContext sslContext = sslUtil.createSSLContext();
      LDAPConnectionOptions connectionOptions = buildLdapConnectionOptions();

      // 寻找可用的服务器地址建立连接
      for (var url : urls) {
        try {
          LDAPURL ldapurl = new LDAPURL(url);
          LDAPConnection connection =
              new LDAPConnection(connectionOptions, ldapurl.getHost(), ldapurl.getPort());

          // Use the StartTLS extended operation to secure the connection.
          StartTLSExtendedRequest startTLSExtendedRequest = new StartTLSExtendedRequest(sslContext);
          startTLSExtendedRequest.setResponseTimeoutMillis(pool.getResponseTimeout().toMillis());
          var startTLSResult = connection.processExtendedOperation(startTLSExtendedRequest);
          if (startTLSResult.getResultCode() != ResultCode.SUCCESS) {
            log.error("StartTLS operation failed: {}", startTLSResult.getDiagnosticMessage());
            continue;
          }

          // Create a connection pool that will secure its connections with StartTLS.
          var bindResult =
              connection.bind(ldapProperties.getUsername(), ldapProperties.getPassword());
          if (bindResult.getResultCode() != ResultCode.SUCCESS) {
            log.error("Bind operation failed: {}", bindResult.getDiagnosticMessage());
            continue;
          }

          return new LDAPConnectionPool(
              connection,
              pool.getMinConnections(),
              pool.getMaxConnections(),
              new StartTLSPostConnectProcessor(sslContext));
        } catch (LDAPException e) {
          log.error("Failed to create LDAPConnectionPool with {}.", url, e);
        }
      }

      throw new LDAPException(
          ResultCode.LOCAL_ERROR,
          String.format("Failed to connect to [%s] by using startTLS.", String.join(", ", urls)));
    }

    // 创建连接池
    return new LDAPConnectionPool(
        buildServerSet(),
        new SimpleBindRequest(ldapProperties.getUsername(), ldapProperties.getPassword()),
        pool.getMinConnections(),
        pool.getMaxConnections());
  }

  private static SSLUtil getTrustAllSslUtil() {
    return new SSLUtil(new TrustAllTrustManager());
  }

  private ServerSet buildServerSet() throws GeneralSecurityException, LDAPException {
    LDAPConnectionOptions options = buildLdapConnectionOptions();
    String[] urls = ldapProperties.getUrls();
    boolean secure = ldapProperties.isSecure();
    if (urls.length == 1) {
      if (secure) {
        SSLUtil sslUtil = getTrustAllSslUtil(); // 测试环境跳过证书验证
        SSLSocketFactory socketFactory = sslUtil.createSSLSocketFactory();
        return buildSingleServerSet(urls, socketFactory, options);
      } else {
        return buildSingleServerSet(urls, null, options);
      }
    } else {
      if (secure) {
        SSLUtil sslUtil = getTrustAllSslUtil(); // 测试环境跳过证书验证
        SSLSocketFactory socketFactory = sslUtil.createSSLSocketFactory();
        return buildServerSet(urls, socketFactory, options);
      } else {
        return buildServerSet(urls, null, options);
      }
    }
  }

  private SingleServerSet buildSingleServerSet(
      String[] urls, SocketFactory socketFactory, LDAPConnectionOptions options)
      throws LDAPException {
    LDAPURL ldapurl = new LDAPURL(urls[0]);
    if (socketFactory == null) {
      return new SingleServerSet(ldapurl.getHost(), ldapurl.getPort(), options);
    } else {
      return new SingleServerSet(ldapurl.getHost(), ldapurl.getPort(), socketFactory, options);
    }
  }

  private ServerSet buildServerSet(
      String[] urls, SocketFactory socketFactory, LDAPConnectionOptions options)
      throws LDAPException {
    String[] hosts = new String[urls.length];
    int[] ports = new int[urls.length];
    for (int i = 0; i < urls.length; i++) {
      LDAPURL ldapurl = new LDAPURL(urls[i]);
      hosts[i] = ldapurl.getHost();
      ports[i] = ldapurl.getPort();
    }
    if (socketFactory == null) {
      return new FastestConnectServerSet(hosts, ports, options);
    } else {
      return new FastestConnectServerSet(hosts, ports, socketFactory, options);
    }
  }

  private LDAPConnectionOptions buildLdapConnectionOptions() {
    LDAPConnectionOptions options = new LDAPConnectionOptions();
    options.setUseTCPNoDelay(true);
    if (ldapProperties.isReferralFollow()) {
      options.setFollowReferrals(true);
      options.setReferralHopLimit(ldapProperties.getReferralFollowLimit());
    }
    options.setAbandonOnTimeout(true);
    options.setConnectionLogger(
        new JSONLDAPConnectionLogger(
            new SLF4JBridgeHandler(), new JSONLDAPConnectionLoggerProperties()));
    LdapProperties.Pool pool = ldapProperties.getPool();
    options.setResponseTimeoutMillis(pool.getResponseTimeout().toMillis());
    options.setConnectTimeoutMillis((int) pool.getConnectionTimeout().toMillis());
    return options;
  }
}
