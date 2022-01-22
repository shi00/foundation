package com.silong.foundation.plugins.log4j2;

/**
 * 日志脱敏接口
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-21 21:56
 */
public interface Desensitizer {

  /**
   * 对msg执行脱敏
   *
   * @param msg 日志消息
   * @return 脱敏结果
   */
  String desensitize(String msg);
}
