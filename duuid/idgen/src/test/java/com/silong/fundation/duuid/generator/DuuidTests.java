package com.silong.fundation.duuid.generator;

import com.silong.fundation.duuid.generator.impl.CircularQueueDuuidGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import static com.silong.fundation.duuid.generator.utils.Constants.DEFAULT_MAX_RANDOM_INCREMENT;
import static com.silong.fundation.duuid.generator.utils.Constants.DEFAULT_QUEUE_CAPACITY;
import static java.util.concurrent.TimeUnit.DAYS;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-01 20:37
 */
public class DuuidTests {

  CircularQueueDuuidGenerator duuidGenerator;

  long workerId;

  @AfterEach
  void cleanup() {
    if (duuidGenerator != null) {
      duuidGenerator.finish();
    }
  }

  private void test(int threadCount, int callCount, boolean enableRandomIncrement)
      throws Exception {
    duuidGenerator = new CircularQueueDuuidGenerator(() -> ++workerId, enableRandomIncrement);
    int initialSz = callCount * threadCount;
    Map<String, List<Long>> map =
        threadCount == 1 ? new HashMap<>(initialSz) : new ConcurrentHashMap<>(initialSz);
    CountDownLatch latch = new CountDownLatch(threadCount);
    CountDownLatch starter = new CountDownLatch(1);
    for (int i = 0; i < threadCount; i++) {
      new Thread(
              () -> {
                try {
                  starter.await();
                  List<Long> list = new ArrayList<>(callCount);
                  for (int j = 0; j < callCount; j++) {
                    list.add(duuidGenerator.nextId());
                  }
                  map.put(Thread.currentThread().getName(), list);
                  latch.countDown();
                } catch (Exception e) {
                  e.printStackTrace();
                }
              })
          .start();
    }
    starter.countDown();
    latch.await();

    assertEquals(initialSz, map.values().parallelStream().mapToInt(List::size).sum());

    boolean result =
        map.values().parallelStream()
            .map(
                ids -> {
                  for (int i = 0, j = i + 1; j < ids.size(); j++, i++) {
                    if (!(ids.get(j) - ids.get(i) > 0)) {
                      return false;
                    }
                  }
                  return true;
                })
            .reduce((b1, b2) -> b1 && b2)
            .orElse(false);

    if (!result) {
      fail("The list of duuid generated by the service is not monotonically incremented.");
    }
  }

  @Test
  @DisplayName("SPSC-randomIncrement-[thread:1]-33554432")
  void test1() throws Exception {
    test(1, 33554432, true);
  }

  @Test
  @DisplayName("SPSC-[inc:1]-[thread:1]-33554432")
  void test2() throws Exception {
    test(1, 33554432, false);
  }

  @Test
  @DisplayName("SPMC-[inc:1]-[thread:100]-33554500")
  void test3() throws Exception {
    int threadCount = 100;
    int callCount = 335545;
    test(threadCount, callCount, false);
  }

  @Test
  @DisplayName("SPMC-randomIncrement-[thread:100]-33554500")
  void test4() throws Exception {
    int threadCount = 100;
    int callCount = 335545;
    test(threadCount, callCount, true);
  }

  @Test
  @DisplayName("SPMC-randomIncrement-[thread:200]-33554500")
  void test5() throws Exception {
    int threadCount = 200;
    int callCount = 335545;
    test(threadCount, callCount, true);
  }

  @Test
  @DisplayName("SPMC-[inc:1]-[thread:200]-33554500")
  void test6() throws Exception {
    int threadCount = 200;
    int callCount = 335545;
    test(threadCount, callCount, false);
  }

  @Test
  @DisplayName("SPSC-[inc:1]-[thread:1]-1024-mocktime")
  void test7() throws InterruptedException {
    long now = System.currentTimeMillis();
    long tomorrow = now + DAYS.toMillis(1);
    AtomicLong count = new AtomicLong(0);
    duuidGenerator =
        new CircularQueueDuuidGenerator(
            () -> ++workerId,
            () -> count.getAndIncrement() <= DEFAULT_QUEUE_CAPACITY - 30 ? now : tomorrow,
            0,
            DEFAULT_QUEUE_CAPACITY,
            false,
            DEFAULT_MAX_RANDOM_INCREMENT);
    long id1 = duuidGenerator.nextId();
    Thread.sleep(1000L);
    long id2 = duuidGenerator.nextId();
    assertTrue(id2 > id1);
  }

  @Test
  @DisplayName("SPMC-[inc:1]-[thread:101]-1024-mocktime")
  void test8() throws InterruptedException {
    long now = System.currentTimeMillis();
    long tomorrow = now + DAYS.toMillis(1);
    AtomicLong count = new AtomicLong(0);
    duuidGenerator =
        new CircularQueueDuuidGenerator(
            () -> ++workerId,
            () -> count.getAndIncrement() <= DEFAULT_QUEUE_CAPACITY - 30 ? now : tomorrow,
            0,
            DEFAULT_QUEUE_CAPACITY,
            false,
            DEFAULT_MAX_RANDOM_INCREMENT);
    long id1 = duuidGenerator.nextId();
    CountDownLatch latch = new CountDownLatch(100);
    for (int i = 0; i < 100; i++) {
      new Thread(
              () -> {
                System.out.println(duuidGenerator.nextId());
                latch.countDown();
              })
          .start();
    }
    latch.await();
    long id2 = duuidGenerator.nextId();
    assertTrue(id2 > id1);
  }
}
