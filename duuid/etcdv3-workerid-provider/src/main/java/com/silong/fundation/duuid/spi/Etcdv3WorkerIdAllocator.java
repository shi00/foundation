package com.silong.fundation.duuid.spi;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.ClientBuilder;
import io.etcd.jetcd.kv.PutResponse;
import io.etcd.jetcd.options.PutOption;
import io.grpc.netty.GrpcSslContexts;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

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

  @Override
  public long allocate(WorkerInfo info) {
    try (Client client = getClient(info)) {
      PutResponse putResponse =
          client
              .getKVClient()
              .put(
                  KEY,
                  ByteSequence.from(
                      String.format(
                          "%s---%s",
                          info.getName(),
                          new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())),
                      UTF_8),
                  PUT_OPTION)
              .get();
      return putResponse.hasPrevKv() ? (int) putResponse.getPrevKv().getVersion() : 0;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Client getClient(WorkerInfo info) throws Exception {
    Map<String, String> extraInfo;
    if (info == null || (extraInfo = info.getExtraInfo()) == null) {
      throw new IllegalArgumentException("info and info.extraInfo must not be null.");
    }
    if (!extraInfo.containsKey(ETCDV3_ENDPOINTS)) {
      throw new IllegalArgumentException(
          String.format("must contain %s in info.extraInfo.", ETCDV3_ENDPOINTS));
    }
    String[] endpoints =
        Arrays.stream(extraInfo.get(ETCDV3_ENDPOINTS).split(","))
            .filter(endpoint -> !endpoint.trim().isEmpty())
            .toArray(String[]::new);

    ClientBuilder builder = Client.builder().endpoints(endpoints);

    // 是否启用https
    if (Stream.of(endpoints).anyMatch(endpoint -> endpoint.startsWith(HTTPS_PREFIX))) {
      SslContextBuilder sslContextBuilder = GrpcSslContexts.forClient();

      if (extraInfo.containsKey(ETCDV3_TRUST_CERT_COLLECTION_FILE)) {
        sslContextBuilder.trustManager(new File(extraInfo.get(ETCDV3_TRUST_CERT_COLLECTION_FILE)));
      } else {
        sslContextBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE);
      }

      if (extraInfo.containsKey(ETCDV3_KEY_FILE)
          && extraInfo.containsKey(ETCDV3_KEY_CERT_CHAIN_FILE)) {
        sslContextBuilder
            // 设置客户端证书
            .keyManager(
            new File(extraInfo.get(ETCDV3_KEY_CERT_CHAIN_FILE)),
            new File(extraInfo.get(ETCDV3_KEY_FILE)));
      } else {
        sslContextBuilder
            // 设置客户端证书
            .keyManager(null, (File) null);
      }
      builder.sslContext(sslContextBuilder.build());
    }

    // 设置访问用户密码
    if (extraInfo.containsKey(ETCDV3_USER) && extraInfo.containsKey(ETCDV3_PASSWORD)) {
      builder
          .user(ByteSequence.from(extraInfo.get(ETCDV3_USER).getBytes(UTF_8)))
          .password(ByteSequence.from(extraInfo.get(ETCDV3_PASSWORD).getBytes(UTF_8)));
    }
    return builder.build();
  }
}
