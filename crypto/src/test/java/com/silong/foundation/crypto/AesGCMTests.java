package com.silong.foundation.crypto;

import com.silong.foundation.crypto.aes.AesGcmToolkit;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * 单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2021-12-26 21:34
 */
public class AesGCMTests {

  static RootKey rootKey;

  String workKey;

  String plaintext;

  @BeforeAll
  static void init() throws IOException {
    Path dir = new File("target/test-classes").toPath();
    RootKey.export(
        RootKey.DEFAULT_ROOT_KEY_PARTS.stream()
            .map(s -> dir.resolve(s).toFile().getAbsolutePath())
            .toArray(String[]::new));
    rootKey = RootKey.initialize();
  }

  @BeforeEach
  void initEatch() {
    plaintext = RandomStringUtils.random(RandomUtils.nextInt(1, Short.SIZE));
    workKey = rootKey.encryptWorkKey(RandomStringUtils.random(RandomUtils.nextInt(1, Short.SIZE)));
  }

  @Test
  void test1() {
    String encrypt = AesGcmToolkit.encrypt(plaintext, workKey);
    String decrypt = AesGcmToolkit.decrypt(encrypt, workKey);
    Assertions.assertEquals(decrypt, plaintext);
  }
}
