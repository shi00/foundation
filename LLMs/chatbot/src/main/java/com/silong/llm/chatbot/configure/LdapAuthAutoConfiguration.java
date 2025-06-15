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

import org.springframework.boot.autoconfigure.ldap.LdapProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.pool2.factory.PoolConfig;
import org.springframework.ldap.pool2.factory.PooledContextSource;
import org.springframework.ldap.pool2.validation.DefaultDirContextValidator;
import org.springframework.ldap.transaction.compensating.manager.TransactionAwareContextSourceProxy;

/**
 * 配置ldap鉴权
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-19 20:21
 */
@Configuration
@EnableConfigurationProperties(LdapProperties.class)
// @ConditionalOnProperty()
public class LdapAuthAutoConfiguration {

  private final LdapProperties ldapProperties;

  public LdapAuthAutoConfiguration(LdapProperties ldapProperties) {
    this.ldapProperties = ldapProperties;
  }

  @Bean
  public LdapContextSource contextSource() {
    LdapContextSource contextSource = new LdapContextSource();
    contextSource.setUrls(ldapProperties.getUrls());
    contextSource.setBase(ldapProperties.getBase());
    contextSource.setUserDn(ldapProperties.getUsername());
    contextSource.setPassword(ldapProperties.getPassword());
    return contextSource;
  }

  @Bean(destroyMethod = "destroy")
  public PooledContextSource poolingContextSource() {
    // 连接池配置
    PoolConfig config = new PoolConfig();
    config.setMaxTotal(20);
    config.setFairness(true);
    config.setLifo(true);
    config.setBlockWhenExhausted(false);
    config.setEvictionPolicyClassName("org.apache.commons.pool2.impl.DefaultEvictionPolicy");
    config.setMaxWaitMillis(5000);
    config.setTestOnBorrow(true);
    config.setTestWhileIdle(true);
    config.setTimeBetweenEvictionRunsMillis(30000);
    PooledContextSource pooledContextSource = new PooledContextSource(config);
    pooledContextSource.setContextSource(contextSource());
    pooledContextSource.setDirContextValidator(new DefaultDirContextValidator());
    return pooledContextSource;
  }

  @Bean
  public TransactionAwareContextSourceProxy transactionAwareContextSourceProxy() {
    return new TransactionAwareContextSourceProxy(poolingContextSource());
  }

  @Bean
  public LdapTemplate ldapTemplate() {
    return new LdapTemplate(transactionAwareContextSourceProxy());
  }
}
