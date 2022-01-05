package com.silong.fundation.crypto.rsa;

import com.silong.fundation.crypto.utils.SecurityWrapper;
import com.silong.fundation.crypto.utils.ThreadLocalCipher;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.security.*;
import java.security.interfaces.RSAKey;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.crypto.Cipher.DECRYPT_MODE;
import static javax.crypto.Cipher.ENCRYPT_MODE;

/**
 * RSA非对称加解密工具
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2021-12-26 19:27
 */
public final class RsaToolkit {

  public static final String RSA = "RSA";

  public static final String RSA_ECB_PKCS_1_PADDING = "RSA/ECB/PKCS1Padding";

  private static final int BUF_SIZE = 8192;

  /** 禁止实例化 */
  private RsaToolkit() {}

  private static int getkeySize(Key key) {
    return ((RSAKey) key).getModulus().bitLength();
  }

  /**
   * 使用私钥加密文本
   *
   * @param key 私钥
   * @param plainText 明文文本
   * @return 加密文本
   */
  public static String encryptByPrivateKey(PrivateKey key, String plainText) {
    if (plainText == null || plainText.isEmpty()) {
      throw new IllegalArgumentException("plainText must not be null or empty.");
    }
    return SecurityWrapper.wrap(
        Base64.getEncoder().encodeToString(encryptByPrivateKey(key, plainText.getBytes(UTF_8))));
  }

  /**
   * 公钥解密文本
   *
   * @param key 公钥
   * @param cipherText 加密文本
   * @return 解密文本
   */
  public static String decryptByPublicKey(PublicKey key, String cipherText) {
    String unwrap = SecurityWrapper.unwrap(cipherText);
    byte[] decode = Base64.getDecoder().decode(unwrap);
    byte[] bytes = decryptByPublicKey(key, decode);
    return new String(bytes, UTF_8);
  }

  /**
   * 使用公钥加密文本
   *
   * @param key 公钥
   * @param plainText 明文文本
   * @return 加密文本
   */
  public static String encryptByPublicKey(PublicKey key, String plainText) {
    if (plainText == null || plainText.isEmpty()) {
      throw new IllegalArgumentException("plainText must not be null or empty.");
    }
    return SecurityWrapper.wrap(
        Base64.getEncoder().encodeToString(encryptByPublicKey(key, plainText.getBytes(UTF_8))));
  }

  /**
   * 私钥解密文本
   *
   * @param key 私钥
   * @param cipherText 加密文本
   * @return 解密文本
   */
  public static String decryptByPrivateKey(PrivateKey key, String cipherText) {
    String unwrap = SecurityWrapper.unwrap(cipherText);
    byte[] decode = Base64.getDecoder().decode(unwrap);
    byte[] bytes = decryptByPrivateKey(key, decode);
    return new String(bytes, UTF_8);
  }

  /**
   * 公钥加密数据
   *
   * @param key 公钥
   * @param plainBytes 明文数据
   * @return 加密数据
   */
  public static byte[] encryptByPublicKey(PublicKey key, byte[] plainBytes) {
    if (plainBytes == null || plainBytes.length == 0) {
      throw new IllegalArgumentException("plainBytes must not be null or empty.");
    }
    return encryptByPublicKey(key, plainBytes, 0, plainBytes.length);
  }

  /**
   * 公钥加密数据
   *
   * @param key 公钥
   * @param plainBytes 明文数据
   * @param offset 数据偏移位置
   * @param length 数据长度
   * @return 加密数据
   */
  public static byte[] encryptByPublicKey(
      PublicKey key, byte[] plainBytes, int offset, int length) {
    return ThreadLocalCipher.encrypt(
        plainBytes,
        offset,
        length,
        RSA_ECB_PKCS_1_PADDING,
        key,
        null,
        (cipher, bytes, ofs, len) ->
            segment(cipher, ENCRYPT_MODE, bytes, ofs, len, getkeySize(key)));
  }

  /**
   * 私钥加密数据
   *
   * @param key 私钥
   * @param plainBytes 明文数据
   * @return 加密数据
   */
  public static byte[] encryptByPrivateKey(PrivateKey key, byte[] plainBytes) {
    if (plainBytes == null || plainBytes.length == 0) {
      throw new IllegalArgumentException("plainBytes must not be null or empty.");
    }
    return encryptByPrivateKey(key, plainBytes, 0, plainBytes.length);
  }

  /**
   * 私钥加密数据
   *
   * @param key 私钥
   * @param plainBytes 明文数据
   * @param offset 数据偏移位置
   * @param length 数据长度
   * @return 加密数据
   */
  public static byte[] encryptByPrivateKey(
      PrivateKey key, byte[] plainBytes, int offset, int length) {
    return ThreadLocalCipher.encrypt(
        plainBytes,
        offset,
        length,
        RSA_ECB_PKCS_1_PADDING,
        key,
        null,
        (cipher, bytes, ofs, len) ->
            segment(cipher, ENCRYPT_MODE, bytes, ofs, len, getkeySize(key)));
  }

  /**
   * 公钥解密数据
   *
   * @param key 公钥
   * @param cipherBytes 加密数据
   * @return 解密数据
   */
  public static byte[] decryptByPublicKey(PublicKey key, byte[] cipherBytes) {
    if (cipherBytes == null || cipherBytes.length == 0) {
      throw new IllegalArgumentException("cipherBytes must not be null or empty.");
    }
    return decryptByPublicKey(key, cipherBytes, 0, cipherBytes.length);
  }

  /**
   * 公钥解密数据
   *
   * @param key 公钥
   * @param cipherBytes 加密数据
   * @param offset 数据偏移位置
   * @param length 数据长度
   * @return 解密数据
   */
  public static byte[] decryptByPublicKey(
      PublicKey key, byte[] cipherBytes, int offset, int length) {
    return ThreadLocalCipher.decrypt(
        cipherBytes,
        offset,
        length,
        RSA_ECB_PKCS_1_PADDING,
        key,
        null,
        (cipher, bytes, ofs, len) ->
            segment(cipher, DECRYPT_MODE, bytes, ofs, len, getkeySize(key)));
  }

  /**
   * 私钥解密数据
   *
   * @param key 私钥
   * @param cipherBytes 加密数据
   * @return 解密数据
   */
  public static byte[] decryptByPrivateKey(PrivateKey key, byte[] cipherBytes) {
    if (cipherBytes == null || cipherBytes.length == 0) {
      throw new IllegalArgumentException("cipherBytes must not be null or empty.");
    }
    return decryptByPrivateKey(key, cipherBytes, 0, cipherBytes.length);
  }

  /**
   * 私钥解密数据
   *
   * @param key 私钥
   * @param cipherBytes 加密数据
   * @param offset 数据偏移位置
   * @param length 数据长度
   * @return 解密数据
   */
  public static byte[] decryptByPrivateKey(
      PrivateKey key, byte[] cipherBytes, int offset, int length) {
    return ThreadLocalCipher.decrypt(
        cipherBytes,
        offset,
        length,
        RSA_ECB_PKCS_1_PADDING,
        key,
        null,
        (cipher, bytes, ofs, len) ->
            segment(cipher, DECRYPT_MODE, bytes, ofs, len, getkeySize(key)));
  }

  private static byte[] segment(
      Cipher cipher, int opmode, byte[] bytes, int offset, int length, int keySize) {
    int maxBlock = keySize / Byte.SIZE;
    if (opmode == ENCRYPT_MODE) {
      maxBlock = maxBlock - 11;
    }

    try (ByteArrayOutputStream out = new ByteArrayOutputStream(BUF_SIZE)) {
      byte[] buff;
      int i = 0;
      while (length > offset) {
        if (length - offset > maxBlock) {
          buff = cipher.doFinal(bytes, offset, maxBlock);
        } else {
          buff = cipher.doFinal(bytes, offset, length - offset);
        }
        out.write(buff, 0, buff.length);
        offset = ++i * maxBlock;
      }
      return out.toByteArray();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

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
