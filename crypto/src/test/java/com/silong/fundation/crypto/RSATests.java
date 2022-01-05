package com.silong.fundation.crypto;

import com.silong.fundation.crypto.rsa.RsaKeyPair;
import com.silong.fundation.crypto.rsa.RsaKeySize;
import com.silong.fundation.crypto.rsa.RsaToolkit;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.*;

import java.io.File;
import java.nio.file.Path;

import static com.silong.fundation.crypto.rsa.RsaToolkit.*;

/**
 * 单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2021-12-26 21:34
 */
public class RSATests {

  static RsaKeyPair keyPair;

  String plaintext;

  @BeforeAll
  static void init() throws Exception {
    Path dir = new File("target/test-classes").toPath();
    RsaKeyPair rsaKeyPair = RsaToolkit.generate(RsaKeySize.BITS_3072);
    String pubkey = dir.resolve("pubkey").toFile().getCanonicalPath();
    String prikey = dir.resolve("prikey").toFile().getCanonicalPath();
    rsaKeyPair.export(prikey, pubkey);
    keyPair = RsaKeyPair.importRsaKeyPair(prikey, pubkey);
  }

  @BeforeEach
  void initEatch() {
    plaintext = RandomStringUtils.random(RandomUtils.nextInt(1, Short.MAX_VALUE));
  }

  @Test
  @DisplayName("PrivateKey-encrypt|PublicKey-decrypt")
  void test1() {
    String encrypt = encryptByPrivateKey(keyPair.getPrivateKey(), plaintext);
    String decrypt = decryptByPublicKey(keyPair.getPublicKey(), encrypt);
    Assertions.assertEquals(plaintext, decrypt);
  }

  @Test
  @DisplayName("PublicKey-encrypt|PrivateKey-decrypt")
  void test2() {
    String encrypt = encryptByPublicKey(keyPair.getPublicKey(), plaintext);
    String decrypt = decryptByPrivateKey(keyPair.getPrivateKey(), encrypt);
    Assertions.assertEquals(plaintext, decrypt);
  }

  @Test
  @DisplayName("PrivateKey-encrypt|PublicKey-decrypt|long-str")
  void test3() {
    for (int i = 0; i < 30; i++) {
      plaintext = RandomStringUtils.random(RandomUtils.nextInt(Short.MAX_VALUE, Integer.MAX_VALUE));
      String encrypt = encryptByPrivateKey(keyPair.getPrivateKey(), plaintext);
      String decrypt = decryptByPublicKey(keyPair.getPublicKey(), encrypt);
      Assertions.assertEquals(plaintext, decrypt);
    }
  }

  @Test
  @DisplayName("PublicKey-encrypt|PrivateKey-decrypt|long-str")
  void test4() {
    for (int i = 0; i < 20; i++) {
      plaintext = RandomStringUtils.random(RandomUtils.nextInt(Short.MAX_VALUE, Integer.MAX_VALUE));
      String encrypt = encryptByPublicKey(keyPair.getPublicKey(), plaintext);
      String decrypt = decryptByPrivateKey(keyPair.getPrivateKey(), encrypt);
      Assertions.assertEquals(plaintext, decrypt);
    }
  }
}
