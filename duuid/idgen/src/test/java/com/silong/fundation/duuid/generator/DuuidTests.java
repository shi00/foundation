package com.silong.fundation.duuid.generator;

import com.silong.fundation.duuid.generator.impl.CircularQueueDuuidGenerator;
import org.apache.commons.lang3.RandomUtils;
import org.jctools.maps.ConcurrentAutoTable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.LongAdder;

import static com.silong.fundation.duuid.generator.utils.Constants.DEFAULT_WORK_ID_BITS;
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

  ConcurrentAutoTable count = new ConcurrentAutoTable();

  @BeforeEach
  void init() {
    duuidGenerator =
        new CircularQueueDuuidGenerator(
            RandomUtils.nextInt(0, (int) (~(-1L << DEFAULT_WORK_ID_BITS))), true);
    count.set(0);
  }

  @AfterEach
  void cleanup() {
    duuidGenerator.stop();
  }

  @Test
  void test1() throws InterruptedException {
    Deque<Long> deque = new ConcurrentLinkedDeque<>();
    int threadCount = 100;
    int callCount = 50000;
    CountDownLatch latch = new CountDownLatch(threadCount);
    CountDownLatch starter = new CountDownLatch(1);
    for (int i = 0; i < threadCount; i++) {
      new Thread(
              () -> {
                try {
                  starter.await();
                  for (int j = 0; j < callCount; j++) {
                    deque.add(duuidGenerator.nextId());
                  }
                  latch.countDown();
                } catch (Exception e) {
                  e.printStackTrace();
                }
              })
          .start();
    }
    starter.countDown();
    latch.await();

    assertEquals(callCount * threadCount, deque.size());
    assertEquals(deque.size(), deque.stream().distinct().count());
  }

  private void count(long id) {
    count.increment();
  }

  @Test
  void test2() {
    int threadCount = 100;
    int callCount = 10000;
    CountDownLatch latch = new CountDownLatch(threadCount);
    CountDownLatch latch1 = new CountDownLatch(1);
    assertTimeout(
        Duration.ofSeconds(1),
        () -> {
          for (int i = 0; i < threadCount; i++) {
            new Thread(
                    () -> {
                      try {
                        latch1.await();
                        for (int j = 0; j < callCount; j++) {
                          count(duuidGenerator.nextId());
                        }
                        latch.countDown();
                      } catch (Exception e) {
                        e.printStackTrace();
                      }
                    })
                .start();
          }
          latch1.countDown();
          latch.await();
        });

    assertEquals(threadCount * callCount, count.get());
  }
}
