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
package com.silong.foundation.springboot.starter.okhttp3.configure;

import com.silong.foundation.springboot.starter.okhttp3.WebClients;
import com.silong.foundation.springboot.starter.okhttp3.configure.config.WebClientConfig;
import com.silong.foundation.springboot.starter.okhttp3.configure.config.WebClientConnectionPoolConfig;
import com.silong.foundation.springboot.starter.okhttp3.configure.config.WebClientProxyConfig;
import com.silong.foundation.springboot.starter.okhttp3.configure.config.WebClientSslConfig;
import okhttp3.Authenticator;
import okhttp3.Dns;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.HostnameVerifier;
import java.util.List;

import static com.silong.foundation.springboot.starter.okhttp3.Constants.NETWORK_INTERCEPTORS;
import static com.silong.foundation.springboot.starter.okhttp3.Constants.NORMAL_INTERCEPTORS;

/**
 * 自动配置OkHttp客户端
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-02-12 09:14
 */
@Configuration
@EnableConfigurationProperties({
  WebClientConfig.class,
  WebClientConnectionPoolConfig.class,
  WebClientSslConfig.class,
  WebClientProxyConfig.class
})
public class OkHttpAutoConfiguration {

  private WebClientConnectionPoolConfig webClientConnectionPoolConfig;

  private WebClientSslConfig webClientSslConfig;

  private WebClientProxyConfig webClientProxyConfig;

  private Authenticator authenticator;

  private HostnameVerifier hostnameVerifier;

  private List<Interceptor> networkInterceptors;

  private List<Interceptor> normalInterceptors;

  private Dns dns;

  @Bean
  @ConditionalOnProperty(prefix = "okhttp3.webclient", name = "enabled", havingValue = "true")
  OkHttpClient registerOkHttpClient(WebClientConfig webClientConfig) {
    return WebClients.create(
        webClientConfig,
        webClientConnectionPoolConfig,
        webClientSslConfig,
        webClientProxyConfig,
        hostnameVerifier,
        authenticator,
        normalInterceptors,
        networkInterceptors,
        dns);
  }

  @Autowired(required = false)
  public void setWebClientConnectionPoolConfig(
      WebClientConnectionPoolConfig webClientConnectionPoolConfig) {
    this.webClientConnectionPoolConfig = webClientConnectionPoolConfig;
  }

  @Autowired(required = false)
  public void setWebClientSslConfig(WebClientSslConfig webClientSslConfig) {
    this.webClientSslConfig = webClientSslConfig;
  }

  @Autowired(required = false)
  public void setWebClientProxyConfig(WebClientProxyConfig webClientProxyConfig) {
    this.webClientProxyConfig = webClientProxyConfig;
  }

  @Autowired(required = false)
  public void setDns(Dns dns) {
    this.dns = dns;
  }

  @Autowired(required = false)
  public void setAuthenticator(Authenticator authenticator) {
    this.authenticator = authenticator;
  }

  @Autowired(required = false)
  public void setHostnameVerifier(HostnameVerifier hostnameVerifier) {
    this.hostnameVerifier = hostnameVerifier;
  }

  @Autowired(required = false)
  @Qualifier(NETWORK_INTERCEPTORS)
  public void setNetworkInterceptors(List<Interceptor> networkInterceptors) {
    this.networkInterceptors = networkInterceptors;
  }

  @Autowired(required = false)
  @Qualifier(NORMAL_INTERCEPTORS)
  public void setNormalInterceptors(List<Interceptor> normalInterceptors) {
    this.normalInterceptors = normalInterceptors;
  }
}
