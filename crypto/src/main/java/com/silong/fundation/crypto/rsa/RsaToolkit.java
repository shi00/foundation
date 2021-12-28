package com.silong.fundation.crypto.rsa;

import java.security.*;

/**
 * RSA非对称加解密工具
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2021-12-26 19:27
 */
public final class RsaToolkit {

  public static final String RSA = "RSA";

  /** 禁止实例化 */
  private RsaToolkit() {}

  /**
   * 生成指定长度密钥对
   *
   * @param size 密钥长度
   * @return 密钥对
   */
  public static RsaKeyPair generate(RsaKeySize size) {
    if (size == null) {
      throw new IllegalArgumentException("size must not be null.");
    }

    try {
      // Get an instance of the RSA key generator
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA);
      keyPairGenerator.initialize(size.getBits());

      // Generate the KeyPair
      KeyPair keyPair = keyPairGenerator.generateKeyPair();

      // Get the public and private key
      PublicKey publicKey = keyPair.getPublic();
      PrivateKey privateKey = keyPair.getPrivate();
      return new RsaKeyPair(publicKey, privateKey);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }
}
