package com.silong.foundation.plugins.log4j2;

/**
 * 日志脱敏接口
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-21 21:56
 */
public interface Desensitizer {

  /** 默认替换字符串 */
  String DEFAULT_REPLACE_STR = "******";

  /**
   * 对msg执行脱敏
   *
   * @param msg 日志消息
   * @return 脱敏结果
   */
  String desensitize(String msg);

  /**
   * 脱敏器实例唯一id
   *
   * @return 唯一id
   */
  default String id() {
    return getClass().getSimpleName();
  }
}
