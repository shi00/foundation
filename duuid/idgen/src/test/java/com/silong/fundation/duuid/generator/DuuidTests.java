package com.silong.fundation.duuid.generator;

import com.silong.fundation.duuid.generator.impl.CircularQueueDuuidGenerator;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeout;

/**
 * 单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-01 20:37
 */
public class DuuidTests {

  CircularQueueDuuidGenerator duuidGenerator;

  @BeforeEach
  void init() {
    duuidGenerator = new CircularQueueDuuidGenerator(RandomUtils.nextInt(1, 8000000), true);
  }

  @AfterEach
  void cleanup() {
    duuidGenerator.finish();
  }

  @Test
  void test1() throws InterruptedException {
    List<Long> list = Collections.synchronizedList(new LinkedList<>());
    CountDownLatch latch = new CountDownLatch(100);
    for (int i = 0; i < 100; i++) {
      new Thread(
              () -> {
                for (int j = 0; j < 10000; j++) {
                  list.add(duuidGenerator.nextId());
                }
                latch.countDown();
              })
          .start();
    }
    latch.await();
    assertEquals(list.stream().distinct().count(), list.size());
  }

  @Test
  void test2() {
    assertTimeout(
        Duration.ofSeconds(1),
        () -> {
          CountDownLatch latch = new CountDownLatch(100);
          for (int i = 0; i < 100; i++) {
            new Thread(
                    () -> {
                      for (int j = 0; j < 3000; j++) {
                        duuidGenerator.nextId();
                      }
                      latch.countDown();
                    })
                .start();
          }
          latch.await();
        });
  }
}
