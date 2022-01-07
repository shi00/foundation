package com.silong.fundation.duuid.spi;

import io.etcd.jetcd.launcher.EtcdContainer;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.*;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.util.Collections;

import static com.silong.fundation.duuid.spi.Etcdv3WorkerIdAllocator.ETCDV3_ENDPOINTS;

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

  static Etcdv3WorkerIdAllocator allocator;

  EtcdContainer container;

  @BeforeAll
  static void init() {
    allocator = new Etcdv3WorkerIdAllocator();
  }

  @AfterEach
  void einit() {
    if (container != null) {
      container.close();
    }
  }

  @Test
  @DisplayName("SingleNode-Http-without-credentials")
  void test() {
    container =
        new EtcdContainer(QUAY_IO_COREOS_ETCD_V_3_5_0, "test-node", Collections.emptyList());
    container.start();
    WorkerInfo info =
        WorkerInfo.builder()
            .name(SystemUtils.getHostName())
            .extraInfo(ImmutableMap.of(ETCDV3_ENDPOINTS, container.clientEndpoint().toString()))
            .build();

    for (int i = 0; i < 100; i++) {
      int allocate = allocator.allocate(info);
      Assertions.assertEquals(i, allocate);
    }
  }
}
