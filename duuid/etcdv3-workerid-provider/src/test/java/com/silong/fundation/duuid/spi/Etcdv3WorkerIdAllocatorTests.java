package com.silong.fundation.duuid.spi;

import com.google.common.collect.ImmutableList;
import io.etcd.jetcd.Auth;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.launcher.EtcdContainer;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import static com.silong.fundation.duuid.spi.Etcdv3WorkerIdAllocator.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-07 21:33
 */
public class Etcdv3WorkerIdAllocatorTests {

  /** 测试用镜像 */
  public static final String QUAY_IO_COREOS_ETCD_V_3_5_0 = "quay.io/coreos/etcd:v3.5.0";

  static Etcdv3WorkerIdAllocator allocator = new Etcdv3WorkerIdAllocator();

  EtcdContainer container;

  @AfterEach
  void einit() {
    if (container != null) {
      container.close();
    }
  }

  @Test
  @DisplayName("SingleNode-Http-without-credentials")
  void test1() {
    container = new EtcdContainer(QUAY_IO_COREOS_ETCD_V_3_5_0, "test-node", ImmutableList.of());
    container.start();
    WorkerInfo info =
        WorkerInfo.builder()
            .name(SystemUtils.getHostName())
            .extraInfo(ImmutableMap.of(ETCDV3_ENDPOINTS, container.clientEndpoint().toString()))
            .build();
    long allocate = -1;
    int i = 0;
    for (; i < 100; i++) {
      allocate = allocator.allocate(info);
    }
    assertEquals(i - 1, allocate);
  }

  @Test
  @DisplayName("SingleNode-Http-with-credentials")
  void test2() {
    container = new EtcdContainer(QUAY_IO_COREOS_ETCD_V_3_5_0, "test-node", ImmutableList.of());
    container.start();
    String url = container.clientEndpoint().toString();
    Client client = Client.builder().endpoints(url).build();
    Auth authClient = client.getAuthClient();
    authClient.userAdd(ByteSequence.from("UserA", UTF_8), ByteSequence.from("123456", UTF_8));
    authClient.authEnable();
    WorkerInfo info =
        WorkerInfo.builder()
            .name(SystemUtils.getHostName())
            .extraInfo(
                ImmutableMap.of(
                    ETCDV3_ENDPOINTS, url, ETCDV3_USER, "UserA", ETCDV3_PASSWORD, "123456"))
            .build();
    for (int i = 0; i < 100; i++) {
      long allocate = allocator.allocate(info);
      assertEquals(i, allocate);
    }
  }
}
