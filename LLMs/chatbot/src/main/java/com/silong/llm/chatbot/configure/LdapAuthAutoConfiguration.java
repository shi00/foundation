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
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import java.security.GeneralSecurityException;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import lombok.NonNull;
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
  }

  @Bean
  LdapUserProvider ldapUserProvider(LDAPConnectionPool ldapConnectionPool) {
    return new LdapUserProvider(ldapProperties, ldapConnectionPool);
  }

  @Bean(destroyMethod = "close")
  LDAPConnectionPool ldapConnectionPool() throws LDAPException, GeneralSecurityException {
    // 创建连接池
    return new LDAPConnectionPool(
        buildServerSet(),
        new SimpleBindRequest(ldapProperties.getUsername(), ldapProperties.getPassword()),
        ldapProperties.getPool().getMinConnections(),
        ldapProperties.getPool().getMaxConnections());
  }

  private ServerSet buildServerSet() throws GeneralSecurityException, LDAPException {
    LDAPConnectionOptions options = buildLdapConnectionOptions();
    String[] urls = ldapProperties.getUrls();
    boolean secure = ldapProperties.isSecure();
    if (urls.length == 1) {
      if (secure) {
        SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager()); // 测试环境跳过证书验证
        SSLSocketFactory socketFactory = sslUtil.createSSLSocketFactory();
        return buildSingleServerSet(urls, socketFactory, options);
      } else {
        return buildSingleServerSet(urls, null, options);
      }
    } else {
      if (secure) {
        SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager()); // 测试环境跳过证书验证
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
    options.setAbandonOnTimeout(true);
    options.setConnectionLogger(
        new JSONLDAPConnectionLogger(
            new SLF4JBridgeHandler(), new JSONLDAPConnectionLoggerProperties()));
    options.setResponseTimeoutMillis(ldapProperties.getPool().getResponseTimeout().toMillis());
    options.setConnectTimeoutMillis(
        (int) ldapProperties.getPool().getConnectionTimeout().toMillis());
    return options;
  }
}
