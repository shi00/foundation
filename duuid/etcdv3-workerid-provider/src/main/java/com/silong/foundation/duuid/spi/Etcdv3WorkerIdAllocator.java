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
package com.silong.foundation.duuid.spi;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.ClientBuilder;
import io.etcd.jetcd.kv.PutResponse;
import io.etcd.jetcd.options.PutOption;
import io.grpc.netty.GrpcSslContexts;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLException;
import java.io.File;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * 基于ETCD v3的WorkerId分配器<br>
 * 通过key的多版本属性，以key的版本号作为workId
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-03 00:25
 */
@Slf4j
public class Etcdv3WorkerIdAllocator implements WorkerIdAllocator {

  /** etcd服务器端点地址列表，多个地址用逗号(,)分隔 */
  public static final String ETCDV3_ENDPOINTS = "etcdv3.endpoints";

  /** etcd用户 */
  public static final String ETCDV3_USER = "etcdv3.user";

  /** etcd用户密码 */
  public static final String ETCDV3_PASSWORD = "etcdv3.password";

  /**
   * Trusted certificates for verifying the remote endpoint's certificate. The file should contain
   * an X.509 certificate collection in PEM format.
   */
  public static final String ETCDV3_TRUST_CERT_COLLECTION_FILE = "etcdv3.trustCertCollectionFile";

  /** an X.509 certificate chain file in PEM format */
  public static final String ETCDV3_KEY_CERT_CHAIN_FILE = "etcdv3.keyCertChainFile";

  /** a PKCS#8 private key file in PEM format */
  public static final String ETCDV3_KEY_FILE = "etcdv3.keyFile";

  private static final String HTTPS_PREFIX = "https://";
  private static final ByteSequence KEY = ByteSequence.from("duuid/worker-id".getBytes(UTF_8));
  private static final PutOption PUT_OPTION =
      PutOption.newBuilder().withLeaseId(0).withPrevKV().build();
  private static final Duration TIMEOUT = Duration.of(10, ChronoUnit.SECONDS);

  @Override
  public long allocate(WorkerInfo info) {
    try (Client client = getClient(info)) {
      SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      String value = String.format("%s---%s", info.getName(), simpleDateFormat.format(new Date()));
      PutResponse putResponse =
          client
              .getKVClient()
              .put(KEY, ByteSequence.from(value, UTF_8), PUT_OPTION)
              .get(TIMEOUT.getSeconds(), SECONDS);
      return putResponse.hasPrevKv() ? putResponse.getPrevKv().getVersion() : 0;
    } catch (SSLException | TimeoutException | ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private boolean isEmpty(String str) {
    return str == null || str.isEmpty();
  }

  @NonNull
  @SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "配置的证书文件路径，无可避免")
  private Client getClient(WorkerInfo info) throws SSLException {
    Map<String, String> extraInfo;
    if (info == null || (extraInfo = info.getExtraInfo()) == null) {
      throw new IllegalArgumentException("info and info.extraInfo must not be null.");
    }

    String endpointsStr = extraInfo.get(ETCDV3_ENDPOINTS);
    if (isEmpty(endpointsStr)) {
      throw new IllegalArgumentException(
          String.format("%s must not be null or empty in extraInfo.", ETCDV3_ENDPOINTS));
    }
    String[] endpoints =
        Arrays.stream(endpointsStr.split(","))
            .filter(endpoint -> !endpoint.trim().isEmpty())
            .toArray(String[]::new);

    ClientBuilder builder =
        Client.builder().endpoints(endpoints).connectTimeout(TIMEOUT).keepaliveTimeout(TIMEOUT);

    // 是否启用https
    if (Stream.of(endpoints).anyMatch(endpoint -> endpoint.startsWith(HTTPS_PREFIX))) {
      SslContextBuilder sslContextBuilder = GrpcSslContexts.forClient();

      String trustCertCollectionFile = extraInfo.get(ETCDV3_TRUST_CERT_COLLECTION_FILE);
      if (isEmpty(trustCertCollectionFile)) {
        sslContextBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE);
      } else {
        sslContextBuilder.trustManager(new File(trustCertCollectionFile));
      }

      String keyFile = extraInfo.get(ETCDV3_KEY_FILE);
      String keyCertChainFile = extraInfo.get(ETCDV3_KEY_CERT_CHAIN_FILE);
      if (isEmpty(keyFile) || isEmpty(keyCertChainFile)) {
        sslContextBuilder
            // 设置客户端证书
            .keyManager(null, (File) null);
      } else {
        sslContextBuilder
            // 设置客户端证书
            .keyManager(new File(keyCertChainFile), new File(keyFile));
      }
      builder.sslContext(sslContextBuilder.build());
    }

    // 设置访问用户密码
    String userName = extraInfo.get(ETCDV3_USER);
    String password = extraInfo.get(ETCDV3_PASSWORD);
    if (!isEmpty(userName) && !isEmpty(password)) {
      builder
          .user(ByteSequence.from(userName.getBytes(UTF_8)))
          .password(ByteSequence.from(password.getBytes(UTF_8)));
    }
    return builder.build();
  }
}
