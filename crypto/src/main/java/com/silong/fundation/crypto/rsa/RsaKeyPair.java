package com.silong.fundation.crypto.rsa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * RSA公私密钥对
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2021-12-27 21:59
 */
@Data
@AllArgsConstructor
public final class RsaKeyPair {
  /** 公钥 */
  @ToString.Exclude private final PublicKey publicKey;
  /** 私钥 */
  @ToString.Exclude private final PrivateKey privateKey;
}
