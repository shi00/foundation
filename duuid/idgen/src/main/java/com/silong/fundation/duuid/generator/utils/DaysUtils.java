package com.silong.fundation.duuid.generator.utils;

import static com.silong.fundation.duuid.generator.utils.Constants.EPOCH;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * 时间工具
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-03 11:54
 */
public interface DaysUtils {
  /**
   * 获取当前时间到时间起算点之差
   *
   * @return 时间差
   */
  static long calculateDeltaDays() {
    return DAYS.convert(currentTimeMillis(), MILLISECONDS) - EPOCH;
  }
}
