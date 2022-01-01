package com.silong.fundation.duuid.generator;

import com.silong.fundation.duuid.generator.impl.CircularQueueDuuidGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-01 20:37
 */
public class DuuidTests {

  static DuuidGenerator duuidGenerator;

  @BeforeAll
  static void init() {
    duuidGenerator = new CircularQueueDuuidGenerator(1, false);
  }

  @Test
  void test1() {
    long id = duuidGenerator.nextId();
    Assertions.assertTrue(id > 0);
  }
}
