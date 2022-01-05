package com.silong.fundation.crypto.rsa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.LinkedList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.silong.fundation.crypto.rsa.RsaToolkit.RSA;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.*;

/**
 * RSA公私密钥对
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2021-12-27 21:59
 */
@Data
@Slf4j
@AllArgsConstructor
public final class RsaKeyPair {
  private static final String PRIVATE_KEY_HEADER = "-----BEGIN RSA PRIVATE KEY-----";
  private static final String PRIVATE_KEY_END = "-----END RSA PRIVATE KEY-----";
  private static final String PUBLIC_KEY_HEADER = "-----BEGIN RSA PUBLIC KEY-----";
  private static final String PUBLIC_KEY_END = "-----END RSA PUBLIC KEY-----";

  /** 公钥 */
  @ToString.Exclude private final PublicKey publicKey;
  /** 私钥 */
  @ToString.Exclude private final PrivateKey privateKey;

  /**
   * 导入公私钥对
   *
   * @param privateKeyFile 私钥文件
   * @param publicKeyFile 公钥文件
   * @return RSA公私钥对
   * @throws Exception 异常
   */
  public static RsaKeyPair importRsaKeyPair(String privateKeyFile, String publicKeyFile)
      throws Exception {
    return new RsaKeyPair(importRsaPublicKey(publicKeyFile), importRsaPrivateKey(privateKeyFile));
  }

  /**
   * 导入RSA公钥
   *
   * @param publicKeyFile 公钥文件
   * @return 公钥
   * @throws Exception 异常
   */
  public static PublicKey importRsaPublicKey(String publicKeyFile) throws Exception {
    byte[] bytes = readRsaKeyFile(publicKeyFile);
    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(bytes);
    KeyFactory keyFactory = KeyFactory.getInstance(RSA);
    return keyFactory.generatePublic(keySpec);
  }

  /**
   * 导入RSA私钥
   *
   * @param privateKeyFile 私钥文件
   * @return 私钥
   * @throws Exception 异常
   */
  public static PrivateKey importRsaPrivateKey(String privateKeyFile) throws Exception {
    byte[] bytes = readRsaKeyFile(privateKeyFile);
    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(bytes);
    KeyFactory keyFactory = KeyFactory.getInstance(RSA);
    return keyFactory.generatePrivate(keySpec);
  }

  private static byte[] readRsaKeyFile(String keyFile) throws IOException {
    if (keyFile == null || keyFile.isEmpty()) {
      throw new IllegalArgumentException("keyFile must not be null or empty.");
    }
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(new FileInputStream(keyFile)))) {
      return Base64.getDecoder()
          .decode(
              reader
                  .lines()
                  .filter(
                      line ->
                          !line.equals(PUBLIC_KEY_END)
                              && !line.equals(PUBLIC_KEY_HEADER)
                              && !line.equals(PRIVATE_KEY_END)
                              && !line.equals(PRIVATE_KEY_HEADER))
                  .map(String::trim)
                  .collect(Collectors.joining()));
    }
  }

  /**
   * 导出RSA公钥和私钥到指定目录
   *
   * @param privateKeyPath 私钥目录
   * @param publicKeyPath 公钥目录
   */
  public void export(String privateKeyPath, String publicKeyPath) throws IOException {
    if (privateKeyPath == null || privateKeyPath.isEmpty()) {
      throw new IllegalArgumentException("privateKeyPath must not be null or empty.");
    }
    if (publicKeyPath == null || publicKeyPath.isEmpty()) {
      throw new IllegalArgumentException("publicKeyPath must not be null or empty.");
    }

    File privateKeyFile = new File(privateKeyPath);
    File publicKeyFile = new File(publicKeyPath);
    File privateKeyFileParentFile = privateKeyFile.getParentFile();
    File publicKeyFileParentFile = publicKeyFile.getParentFile();

    if (!privateKeyFileParentFile.exists()) {
      if (privateKeyFileParentFile.mkdirs()) {
        log.info(
            "Directory {} was successfully created", privateKeyFileParentFile.getCanonicalPath());
      }
    }
    if (!publicKeyFileParentFile.exists()) {
      if (publicKeyFileParentFile.mkdirs()) {
        log.info(
            "Directory {} was successfully created", publicKeyFileParentFile.getCanonicalPath());
      }
    }

    output2File(publicKeyFile, this.publicKey.getEncoded(), false);
    output2File(privateKeyFile, this.privateKey.getEncoded(), true);
  }

  private void output2File(File file, byte[] bytes, boolean isPrivateKey) throws IOException {
    String str = Base64.getEncoder().encodeToString(bytes);
    LinkedList<String> lines =
        Stream.of(str.split("(?<=\\G.{64})")).collect(Collectors.toCollection(LinkedList::new));
    lines.add(0, isPrivateKey ? PRIVATE_KEY_HEADER : PUBLIC_KEY_HEADER);
    lines.add(isPrivateKey ? PRIVATE_KEY_END : PUBLIC_KEY_END);
    Files.write(file.toPath(), lines, UTF_8, WRITE, CREATE, TRUNCATE_EXISTING);
  }
}
