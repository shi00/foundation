package com.silong.foundation.duuid.server;

import com.google.common.collect.ImmutableList;
import com.silong.foundation.crypto.digest.HmacToolkit;
import com.silong.foundation.duuid.server.model.Duuid;
import com.silong.foundation.springboot.starter.simpleauth.configure.properties.SimpleAuthProperties;
import io.etcd.jetcd.launcher.EtcdContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.*;
import java.util.*;

import static com.silong.foundation.springboot.starter.simpleauth.constants.AuthHeaders.*;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils.randomAlphabetic;

/**
 * 服务集成测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-11 20:49
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = DuuidServerApplication.class)
@TestPropertySource(
    locations = "classpath:application-test.properties",
    properties = {
      // 规避两个随机端口，无法获取第二个随机端口问题，此处指定管理端口
      "management.server.port=35671",
    })
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class DuuidServerApplicationTests {

  /** 测试用镜像 */
  public static final String QUAY_IO_COREOS_ETCD_V_3_5_0 = "quay.io/coreos/etcd:v3.5.0";

  public static final String APPLICATION_PROPERTIES = "application-test.properties";

  public static final String OUT_FILE =
      requireNonNull(
              DuuidServerApplicationTests.class
                  .getClassLoader()
                  .getResource(APPLICATION_PROPERTIES))
          .getFile();

  public static final File TEMPLATE_FILE =
      new File(OUT_FILE)
          .getParentFile()
          .getParentFile()
          .getParentFile()
          .toPath()
          .resolve("src")
          .resolve("test")
          .resolve("resources")
          .resolve(APPLICATION_PROPERTIES)
          .toFile();

  static EtcdContainer container;

  @LocalServerPort private int port;

  @Value("${management.server.port}")
  private int actuatorPort;

  @Autowired private TestRestTemplate restTemplate;

  @Autowired private SimpleAuthProperties properties;

  private String idGenEngdpoint;

  private String prometheusEngdpoint;

  private String openApiEngdpoint;

  private String swaggerUiEndpoint;

  private HttpHeaders headers;

  /**
   * 初始化etcd容器，并更新服务配置
   *
   * @throws IOException 异常
   */
  @BeforeAll
  static void etcdInit() throws IOException {
    container =
        new EtcdContainer(QUAY_IO_COREOS_ETCD_V_3_5_0, randomAlphabetic(10), ImmutableList.of());
    container.start();
    Properties applicationProperties = new Properties();
    try (Reader in = new FileReader(TEMPLATE_FILE)) {
      applicationProperties.load(in);
      applicationProperties.setProperty(
          "duuid.worker-id-provider.etcdv3.server-addresses",
          container.clientEndpoint().toString());
    }
    try (Writer out = new FileWriter(OUT_FILE)) {
      applicationProperties.store(out, "For Integration Testing");
    }
  }

  @AfterAll
  static void cleanUp() {
    if (container != null) {
      container.close();
    }
  }

  @BeforeEach
  void init() {
    idGenEngdpoint = String.format("http://localhost:%d/duuid", port);
    prometheusEngdpoint = String.format("http://localhost:%d/actuator/prometheus", actuatorPort);
    openApiEngdpoint =
        String.format("http://localhost:%d/actuator/openapi/duuid-server", actuatorPort);
    swaggerUiEndpoint = String.format("http://localhost:%d/actuator/swaggerui", actuatorPort);
    headers = new HttpHeaders();
    // set `content-type` header
    headers.setContentType(APPLICATION_JSON);
    // set `accept` header
    headers.setAccept(Arrays.asList(APPLICATION_JSON, TEXT_PLAIN));
  }

  private void buildHeaders(String identity) {
    long now = System.currentTimeMillis();
    headers.set(IDENTITY, identity);
    headers.set(TIMESTAMP, String.valueOf(now));
    String random = randomAlphabetic(64);
    headers.set(RANDOM, random);
    headers.set(
        SIGNATURE, HmacToolkit.hmacSha256(identity + now + random, properties.getWorkKey()));
  }

  @Test
  void test1() {
    buildHeaders("client");
    HttpEntity<Void> entity = new HttpEntity<>(headers);
    long id1 = restTemplate.postForObject(idGenEngdpoint, entity, Duuid.class).id();
    buildHeaders("client");
    entity = new HttpEntity<>(headers);
    long id2 = restTemplate.postForObject(idGenEngdpoint, entity, Duuid.class).id();
    assertTrue(id2 > id1);
  }

  @Test
  void test2() {
    LinkedList<Long> list = new LinkedList<>();
    for (int i = 0; i < 10000; i++) {
      buildHeaders("client");
      HttpEntity<Void> entity = new HttpEntity<>(headers);
      list.add(restTemplate.postForObject(idGenEngdpoint, entity, Duuid.class).id());
    }
    List<Long> back = new ArrayList<>(list);
    back.sort(Long::compare);
    assertEquals(list, back);
  }

  @Test
  void test3() {
    ResponseEntity<String> response =
        restTemplate.exchange(openApiEngdpoint, GET, null, String.class);
    assertEquals(OK, response.getStatusCode());
  }

  @Test
  void test4() {
    ResponseEntity<String> response =
        restTemplate.exchange(swaggerUiEndpoint, GET, null, String.class);
    assertEquals(OK, response.getStatusCode());
  }

  @Test
  void test5() {
    buildHeaders("prometheus");
    HttpEntity<Void> entity = new HttpEntity<>(headers);
    ResponseEntity<String> responseEntity =
        restTemplate.exchange(prometheusEngdpoint, GET, entity, String.class);
    System.out.println(responseEntity.getBody());
    assertFalse(responseEntity.getBody() == null || responseEntity.getBody().isEmpty());
  }
}
