package com.silong.foundation.crypto.aes;

import lombok.Getter;

/**
 * AES密钥长度
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2021-12-26 10:57
 */
public enum AesKeySize {
  /** 推荐使用，256bit，32bytes */
  BITS_256(256),
  /** 不推荐使用，192bit 24bytes */
  @Deprecated
  BITS_192(192),
  /** 不推荐使用，128bit，16bytes */
  @Deprecated
  BITS_128(128);

  @Getter private final int bits;

  /**
   * 构造方法
   *
   * @param bits 密钥长度
   */
  AesKeySize(int bits) {
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
