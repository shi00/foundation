package com.silong.fundation.duuid.spi;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.ClientBuilder;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.kv.PutResponse;
import io.etcd.jetcd.options.PutOption;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
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

  /** etcd服务器端点地址列表 */
  public static final String ETCDV3_ENDPOINTS = "etcdv3.endpoints";

  /** etcd用户 */
  public static final String ETCDV3_USER = "etcdv3.user";

  /** etcd用户密码 */
  public static final String ETCDV3_PASSWORD = "etcdv3.password";

  private static final ByteSequence KEY = ByteSequence.from("duuid/worker-id".getBytes(UTF_8));
  private static final ByteSequence VALUE = ByteSequence.from("useless".getBytes(UTF_8));
  private static final PutOption PUT_OPTION =
      PutOption.newBuilder().withLeaseId(0).withPrevKV().build();

  /** ETCD v3客户端 */
  private KV kvClient;

  @Override
  public int allocate(WorkerInfo info) {
    try {
      // 初始化客户端
      initialize(info);
      PutResponse putResponse = kvClient.put(KEY, VALUE, PUT_OPTION).get();
      return putResponse.hasPrevKv() ? (int) putResponse.getPrevKv().getVersion() : 0;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private synchronized void initialize(WorkerInfo info) throws Exception {
    if (kvClient == null) {
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
      if (Stream.of(endpoints).anyMatch(endpoint -> endpoint.startsWith("https://"))) {
        builder.sslContext(SslContextBuilder.forClient().build());
      }

      // 设置访问用户密码
      if (extraInfo.containsKey(ETCDV3_USER) && extraInfo.containsKey(ETCDV3_PASSWORD)) {
        builder
            .user(ByteSequence.from(extraInfo.get(ETCDV3_USER).getBytes(UTF_8)))
            .password(ByteSequence.from(extraInfo.get(ETCDV3_PASSWORD).getBytes(UTF_8)));
      }

      kvClient = builder.build().getKVClient();
    }
  }
}
