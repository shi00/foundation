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

package com.silong.foundation.springboot.starter.minio.configure;

import com.silong.foundation.springboot.starter.minio.configure.properties.MinioClientProperties;
import com.silong.foundation.springboot.starter.minio.handler.AsyncMinioHandler;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.minio.MinioAsyncClient;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import lombok.NonNull;
import okhttp3.*;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * minio client 配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-03-29 6:42
 */
@AutoConfiguration
@EnableConfigurationProperties({MinioClientProperties.class})
@ConditionalOnClass(MinioAsyncClient.class)
@SuppressFBWarnings(
    value = "WEAK_TRUST_MANAGER",
    justification = "Intranet environment is used to control risks")
public class MinioClientAutoConfiguration {

  private static final HostnameVerifier NO_HOSTNAME_VERIFIER = (v1, v2) -> true;

  private static final TrustManager[] TRUST_ALL_CERTS =
      new TrustManager[] {TrustAllTrustManager.INSTANCE};

  private static class TrustAllTrustManager implements X509TrustManager {

    public static TrustAllTrustManager INSTANCE = new TrustAllTrustManager();

    private TrustAllTrustManager() {}

    @Override
    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {}

    @Override
    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {}

    @Override
    public X509Certificate[] getAcceptedIssuers() {
      return new X509Certificate[0];
    }
  }

  @Bean
  MinioAsyncClient minioAsyncClient(@NonNull MinioClientProperties minioClientProperties)
      throws Exception {

    MinioClientProperties.Pool pool = minioClientProperties.getConnectionPool();

    // 配置连接池参数
    ConnectionPool connectionPool =
        new ConnectionPool(
            pool.getMaxIdleConnections(), // 最大空闲连接数
            pool.getKeepAliveDuration().toSeconds(), // 连接保持活跃的时间（秒）
            TimeUnit.SECONDS);

    // 构建 OkHttpClient 并设置连接池
    OkHttpClient.Builder builder =
        new OkHttpClient.Builder()
            .connectionPool(connectionPool)
            .connectTimeout(
                minioClientProperties.getConnectionTimeout().toSeconds(),
                TimeUnit.SECONDS) // 连接超时时间
            .readTimeout(
                minioClientProperties.getReadTimeout().toSeconds(), TimeUnit.SECONDS) // 读取超时时间
            .writeTimeout(
                minioClientProperties.getWriteTimeout().toSeconds(), TimeUnit.SECONDS); // 写入超时时间

    if (minioClientProperties.isSecure()) {
      ConnectionSpec customSpec =
          new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
              .tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2) // 仅启用 TLS 1.3/1.2
              .build();
      builder.connectionSpecs(List.of(customSpec));

      builder.hostnameVerifier(NO_HOSTNAME_VERIFIER);
      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null, TRUST_ALL_CERTS, new SecureRandom());
      builder.sslSocketFactory(sslContext.getSocketFactory(), TrustAllTrustManager.INSTANCE);
    }

    return MinioAsyncClient.builder()
        .httpClient(builder.build())
        .endpoint(minioClientProperties.getEndpoint())
        .region(minioClientProperties.getRegion())
        .credentials(minioClientProperties.getAccessKey(), minioClientProperties.getSecretKey())
        .build();
  }

  @Bean
  AsyncMinioHandler registerAsyncMinioHandler(
      @NonNull MinioAsyncClient client, @NonNull MinioClientProperties minioClientProperties) {
    return new AsyncMinioHandler(client, minioClientProperties);
  }
}
