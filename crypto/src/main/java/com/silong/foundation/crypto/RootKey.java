/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.silong.foundation.crypto;

import com.silong.foundation.crypto.aes.AesGcmToolkit;
import com.silong.foundation.crypto.aes.AesKeySize;
import com.silong.foundation.crypto.pbkdf2.Pbkdf2;
import com.silong.foundation.crypto.utils.ThreadLocalSecureRandom;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * 根密钥工具，根密钥用于加密工作密钥
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2021-12-26 09:21
 */
@Slf4j
public final class RootKey {

  /** 默认根密钥保存路径 */
  public static final List<String> DEFAULT_ROOT_KEY_PARTS =
      List.of("zoo/tiger", "zoo/north/penguin", "zoo/south/skunk", "zoo/west/peacock");

  /** 是否启用缓存 */
  public static final boolean ENABLED_CACHE =
      Boolean.parseBoolean(System.getProperty("rootkey.cache.enabled", "true"));

  private static final Map<String, byte[]> WK_CACHE = new ConcurrentHashMap<>();

  private static RootKey rootKey;

  /** 根密钥 */
  private final SecretKey key;

  /**
   * 构造方法
   *
   * @param key 根密钥
   */
  private RootKey(SecretKey key) {
    this.key = key;
  }

  /**
   * 使用根密钥加密工作密钥
   *
   * @param keyBytes 密钥
   * @return 加密结果
   */
  public String encryptWorkKey(byte[] keyBytes) {
    if (keyBytes == null || keyBytes.length == 0) {
      throw new IllegalArgumentException("keyBytes must not be null or empty.");
    }
    return AesGcmToolkit.encrypt(keyBytes, 0, keyBytes.length, this.key, AesGcmToolkit.randomIv());
  }

  /**
   * 使用根密钥加密工作密钥
   *
   * @param workKey 工作密钥明文
   * @return 加密后工作密钥
   */
  public String encryptWorkKey(String workKey) {
    if (workKey == null || workKey.isEmpty()) {
      throw new IllegalArgumentException("workKey must not be null or empty.");
    }
    try {
      byte[] bytes = Pbkdf2.generate(workKey.toCharArray(), AesKeySize.BITS_256.getBits());
      return AesGcmToolkit.encrypt(bytes, 0, bytes.length, this.key, AesGcmToolkit.randomIv());
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * 解密工作密钥
   *
   * @param workKey 工作密钥
   * @return 解密工作密钥
   */
  public byte[] decryptWorkKey(String workKey) {
    if (workKey == null || workKey.isEmpty()) {
      throw new IllegalArgumentException("workKey must not be null or empty.");
    }
    return ENABLED_CACHE
        ? WK_CACHE.computeIfAbsent(workKey, k -> AesGcmToolkit.decrypt(k, key))
        : AesGcmToolkit.decrypt(workKey, key);
  }

  /**
   * 获取根密钥实例
   *
   * @return 根密钥
   */
  public static RootKey getInstance() {
    return requireNonNull(rootKey, "RootKey must be initialized before use.");
  }

  /**
   * 从默认classpath路径加载根密钥<br>
   * 默认加载路径为：<br>
   * zoo/tiger<br>
   * zoo/north/penguin<br>
   * zoo/south/skunk<br>
   * zoo/west/peacock
   *
   * @return 根密钥
   */
  public static RootKey initialize() {
    return initialize(DEFAULT_ROOT_KEY_PARTS.toArray(new String[0]));
  }

  /**
   * 导出根密钥到指定文件位置
   *
   * @param rootKeyPartPaths 根密钥保存位置
   * @throws IOException IO异常
   */
  public static void export(File... rootKeyPartPaths) throws IOException {
    if (rootKeyPartPaths == null || rootKeyPartPaths.length != DEFAULT_ROOT_KEY_PARTS.size()) {
      throw new IllegalArgumentException(
          String.format("rootKeyPartPaths must be %d.", DEFAULT_ROOT_KEY_PARTS.size()));
    }

    String[] rootKeyParts = generate();
    for (int i = 0; i < rootKeyPartPaths.length; i++) {
      Path path = rootKeyPartPaths[i].toPath();
      File parentFile = rootKeyPartPaths[i].getParentFile();
      if (parentFile.mkdirs()) {
        log.info("{} was successfully created.", parentFile.getCanonicalPath());
      }
      Files.write(path, rootKeyParts[i].getBytes(UTF_8), CREATE, WRITE, TRUNCATE_EXISTING);
    }
  }

  /**
   * 加载给定classpath路径的根密钥，并初始化根密钥，获取其实例
   *
   * @param rootKeyParts 根密钥文件存放路径(classpath)
   * @return 根密钥
   */
  public static synchronized RootKey initialize(String... rootKeyParts) {
    if (rootKey != null) {
      return rootKey;
    }

    if (rootKeyParts == null || rootKeyParts.length != DEFAULT_ROOT_KEY_PARTS.size()) {
      throw new IllegalArgumentException(
          String.format("rootKeyParts must be %d.", DEFAULT_ROOT_KEY_PARTS.size()));
    }
    byte[][] keyParts =
        Arrays.stream(rootKeyParts)
            .map(RootKey::loadRootKeyPart)
            .sorted()
            .map(part -> Base64.getDecoder().decode(part))
            .toArray(byte[][]::new);
    byte[] salt = keyParts[0];
    char[] chars = new char[salt.length];
    for (int i = 0; i < salt.length; i++) {
      chars[i] = (char) (keyParts[1][i] ^ keyParts[2][i] ^ keyParts[3][i]);
    }
    try {
      byte[] key = Pbkdf2.generate(chars, salt, AesKeySize.BITS_256.getBits());
      return rootKey = new RootKey(new SecretKeySpec(key, AesGcmToolkit.AES));
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new RuntimeException(e);
    }
  }

  private static String loadRootKeyPart(String part) {
    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(
                requireNonNull(RootKey.class.getClassLoader().getResourceAsStream(part)), UTF_8))) {
      return reader.lines().map(String::trim).collect(joining());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * 生成根密钥
   *
   * @return 根密钥分组
   */
  public static String[] generate() {
    String[] parts = new String[DEFAULT_ROOT_KEY_PARTS.size()];
    byte[] array = new byte[AesKeySize.BITS_256.getBits()];
    Base64.Encoder encoder = Base64.getEncoder();
    SecureRandom secureRandom = ThreadLocalSecureRandom.get();
    for (int i = 0; i < parts.length; i++) {
      secureRandom.nextBytes(array);
      parts[i] = encoder.encodeToString(array);
    }
    Arrays.sort(parts);
    return parts;
  }
}
