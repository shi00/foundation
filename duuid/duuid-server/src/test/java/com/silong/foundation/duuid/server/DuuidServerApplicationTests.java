package com.silong.foundation.duuid.server;

import com.silong.foundation.duuid.server.model.Duuid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 服务集成测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-11 20:49
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-test.properties")
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class DuuidServerApplicationTests {

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate restTemplate;

  private String endpoint;

  @BeforeEach
  void init() {
    endpoint = String.format("http://localhost:%d/duuid", port);
  }

  @Test
  void test1() {
    long id1 = restTemplate.postForObject(endpoint, null, Duuid.class).id();
    long id2 = restTemplate.postForObject(endpoint, null, Duuid.class).id();
    assertTrue(id2 > id1);
  }
}
