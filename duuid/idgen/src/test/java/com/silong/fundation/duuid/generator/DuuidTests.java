package com.silong.fundation.duuid.generator;

import com.silong.fundation.duuid.generator.impl.CircularQueueDuuidGenerator;
import org.apache.commons.lang3.RandomUtils;
import org.jctools.maps.ConcurrentAutoTable;
import org.jctools.maps.NonBlockingHashMapLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import static com.silong.fundation.duuid.generator.utils.Constants.DEFAULT_WORK_ID_BITS;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-01 20:37
 */
public class DuuidTests {

  int maxWorkerId = (int) (~(-1L << DEFAULT_WORK_ID_BITS)) + 1;
  ConcurrentAutoTable count = new ConcurrentAutoTable();
  CircularQueueDuuidGenerator duuidGenerator;

  @AfterEach
  void cleanup() {
    if (duuidGenerator != null) {
      duuidGenerator.stop();
    }
  }

  @Test
  @DisplayName("SPSC-randomIncrement-33554432")
  void test1() {
    duuidGenerator = new CircularQueueDuuidGenerator(RandomUtils.nextInt(0, maxWorkerId), true);
    int length = 33554432;
    long[] array = new long[length];
    for (int i = 0; i < length; i++) {
      array[i] = duuidGenerator.nextId();
    }
    long[] clone = new long[length];
    System.arraycopy(array, 0, clone, 0, length);
    Arrays.sort(array);
    assertArrayEquals(clone, array);
  }

  @Test
  @DisplayName("SPSC-[inc:1]-33554432")
  void test2() {
    duuidGenerator = new CircularQueueDuuidGenerator(RandomUtils.nextInt(0, maxWorkerId), false);
    int length = 33554432;
    long[] array = new long[length];
    for (int i = 0; i < length; i++) {
      array[i] = duuidGenerator.nextId();
    }
    long[] clone = new long[length];
    System.arraycopy(array, 0, clone, 0, length);
    Arrays.sort(array);
    assertArrayEquals(clone, array);
  }

  @Test
  @DisplayName("SPMC-[inc:1]-[thread:100]-10000000")
  void test3() throws InterruptedException {
    int threadCount = 100;
    int callCount = 100000;
    duuidGenerator = new CircularQueueDuuidGenerator(RandomUtils.nextInt(0, maxWorkerId), false);
    NonBlockingHashMapLong<Boolean> map = new NonBlockingHashMapLong<>(callCount * threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);
    CountDownLatch starter = new CountDownLatch(1);
    for (int i = 0; i < threadCount; i++) {
      new Thread(
              () -> {
                try {
                  starter.await();
                  for (int j = 0; j < callCount; j++) {
                    map.put(duuidGenerator.nextId(), Boolean.TRUE);
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

    assertEquals(threadCount * callCount, map.size());
  }

  @Test
  @DisplayName("SPMC-randomIncrement-[thread:100]-10000000")
  void test4() throws InterruptedException {
    int threadCount = 100;
    int callCount = 100000;
    duuidGenerator = new CircularQueueDuuidGenerator(RandomUtils.nextInt(0, maxWorkerId), true);
    NonBlockingHashMapLong<Boolean> map = new NonBlockingHashMapLong<>(callCount * threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);
    CountDownLatch starter = new CountDownLatch(1);
    for (int i = 0; i < threadCount; i++) {
      new Thread(
              () -> {
                try {
                  starter.await();
                  for (int j = 0; j < callCount; j++) {
                    map.put(duuidGenerator.nextId(), Boolean.TRUE);
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

    assertEquals(threadCount * callCount, map.size());
  }

  private void count(long id) {
    count.increment();
  }

  @Test
  @DisplayName("SPMC-randomIncrement-[thread:200]-20000000")
  void test5() {
    int threadCount = 200;
    int callCount = 10000;
    duuidGenerator = new CircularQueueDuuidGenerator(RandomUtils.nextInt(0, maxWorkerId), true);
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
