package com.silong.fundation.crypto.utils;

import java.security.SecureRandom;

import static java.util.Objects.requireNonNull;

/**
 * 安全随机数线程局部变量
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2021-12-26 09:27
 */
public final class ThreadLocalSecureRandom {

  private static final ThreadLocal<SecureRandom> TLSR = ThreadLocal.withInitial(SecureRandom::new);

  /** 工具类，禁止实例化 */
  private ThreadLocalSecureRandom() {}

  /**
   * 获取安全随机数生成器
   *
   * @return 随机数生成器
   */
  public static SecureRandom get() {
    return TLSR.get();
  }

  /**
   * 生成指定长度字节数组
   *
   * @param size 数组长度
   * @return 数组
   * @throws IllegalArgumentException size < 0
   */
  public static byte[] random(int size) {
    if (size < 0) {
      throw new IllegalArgumentException("size must be greater than or equals to 0.");
    }
    byte[] array = new byte[size];
    TLSR.get().nextBytes(array);
    return array;
  }

  /**
   * 生成随机字节数组
   *
   * @param array 字节数组
   * @return array
   * @throws NullPointerException array == null
   */
  public static byte[] random(byte[] array) {
    TLSR.get().nextBytes(requireNonNull(array, "array must not be null."));
    return array;
  }
}
