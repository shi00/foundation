package com.silong.fundation.crypto;

import lombok.Getter;

/**
 * RSA密钥长度
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2021-12-26 10:57
 */
public enum RsaKeySize {
  /** 不推荐 */
  @Deprecated
  BITS_1024(1024),
  /** 不推荐 */
  @Deprecated
  BITS_192(2048),
  /** 推荐使用 */
  BITS_3072(3072),
  /** 更安全，但是性能会更慢 */
  BITS_4096(4096);

  @Getter private final int bits;

  /**
   * 构造方法
   *
   * @param bits 密钥长度
   */
  RsaKeySize(int bits) {
    this.bits = bits;
  }

  /**
   * 字节数
   *
   * @return 字节数
   */
  public int getBytes() {
    return bits / Byte.SIZE;
  }
}
