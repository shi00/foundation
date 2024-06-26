/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package com.silong.foundation.utilities.nlloader;

import static com.silong.foundation.utilities.nlloader.PlatformDetector.*;
import static java.io.File.pathSeparator;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * 从classpath包含的jar包中加载指定的共享库，配合本地方法使用
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-13 22:55
 */
@Slf4j
public final class NativeLibLoader {

  /** 临时目录 */
  public static final Path TEMP_DIR = new File(System.getProperty("java.io.tmpdir")).toPath();

  /** 默认本地库在classpath中存放的目录 */
  private static final String DEFAULT_LIB_DIR = "/native";

  /** 操作系统名 */
  public static final String OS_NAME;

  /** 操作系统名 */
  public static final String OS_ARCH;

  static {
    Properties properties = new Properties();
    PLATFORM_DETECTOR.detect(properties, List.of());
    OS_NAME = properties.getProperty(DETECTED_NAME);
    OS_ARCH = properties.getProperty(DETECTED_ARCH);
    log.info("OS_NAME: {}", OS_NAME);
    log.info("OS_ARCH: {}", OS_ARCH);
  }

  /** 工具类，禁止实例化 */
  private NativeLibLoader() {}

  /**
   * 在用户临时目录下生成库文件
   *
   * @param originLib 类库名
   * @param libDir 共享库存放目录
   * @return 生成的临时类库文件标准路径
   * @throws IOException 异常
   */
  private static String generateTempLib(String originLib, String libDir) throws IOException {
    int index = originLib.lastIndexOf('.');
    String prefix = originLib.substring(0, index);
    String suffix = originLib.substring(index);
    Path tmpLib = TEMP_DIR.resolve(String.format("%s_%d%s", prefix, System.nanoTime(), suffix));
    String libPath = String.format("%s/%s/%s/%s", libDir, OS_NAME, OS_ARCH, originLib);
    try {
      return doInternalGenerate(originLib, libPath, tmpLib);
    } catch (IOException | NullPointerException e) {
      // fallback
      libPath = String.format("%s/%s/%s", libDir, OS_NAME, originLib);
      try {
        return doInternalGenerate(originLib, libPath, tmpLib);
      } catch (IOException | NullPointerException ex) {
        // fallback
        libPath = String.format("%s/%s", libDir, originLib);
        try {
          return doInternalGenerate(originLib, libPath, tmpLib);
        } catch (IOException | NullPointerException exc) {
          // fallback
          libPath = String.format("/%s", originLib);
          return doInternalGenerate(originLib, libPath, tmpLib);
        }
      }
    }
  }

  private static String doInternalGenerate(String originLib, String libPath, Path tmpLib)
      throws IOException {
    try (InputStream inputStream = NativeLibLoader.class.getResourceAsStream(libPath)) {
      Files.copy(
          requireNonNull(
              inputStream,
              String.format("Failed to load %s from %s in classpath.", originLib, libPath)),
          tmpLib,
          REPLACE_EXISTING);
      File tmpFile = tmpLib.toFile();
      tmpFile.deleteOnExit();
      log.info(String.format("Prepare to load %s from %s in classpath.", originLib, libPath));
      String canonicalPath = tmpFile.getCanonicalPath();
      log.info(String.format("Generate %s for loading.", canonicalPath));
      return canonicalPath;
    }
  }

  /**
   * 搜索classpath中jar包，加载指定共享库，按当前程序运行操作系统类型以及架构进行加载<br>
   * 例如：<br>
   * A.jar/Windows/64bit/xx.dll
   *
   * @param libName 库名，不带格式
   */
  public static void loadLibrary(String libName) {
    loadLibrary(libName, DEFAULT_LIB_DIR);
  }

  /**
   * 搜索classpath中jar包，加载指定共享库，按当前程序运行操作系统类型以及架构进行加载<br>
   * 例如：<br>
   * A.jar/Windows/64bit/xx.dll
   *
   * @param libName 库名，不带格式
   * @param libDir 共享库存放目录
   */
  @SneakyThrows(IOException.class)
  public static void loadLibrary(String libName, String libDir) {
    if (libName == null || libName.isEmpty()) {
      throw new IllegalArgumentException("libName must not be null or empty.");
    }
    if (libDir == null || libDir.isEmpty()) {
      throw new IllegalArgumentException("libDir must not be null or empty.");
    }

    // 不能包含库格式，由运行环境决定
    if (Arrays.stream(PlatformLibFormat.values())
        .map(platformLibFormat -> platformLibFormat.libFormat)
        .anyMatch(libName::endsWith)) {
      throw new IllegalArgumentException("libName cannot contain library format suffix.");
    }

    String originLib = String.format("%s.%s", libName, PlatformLibFormat.match(OS_NAME).libFormat);
    try {
      System.loadLibrary(libName); // 首先尝试从java.library.path加载
    } catch (UnsatisfiedLinkError t) {

      // 在classpath包含的目录中查找文件，如果找到则直接加载
      File libFile =
          Arrays.stream(System.getProperty("java.class.path").split(pathSeparator))
              .map(File::new)
              .filter(File::isDirectory)
              .map(f -> f.toPath().resolve(originLib).toFile())
              .filter(File::exists)
              .findAny()
              .orElse(null);
      if (libFile != null) {
        System.load(libFile.getCanonicalPath());
        return;
      }

      // 按需添加目录前缀
      if (!libDir.startsWith("/")) {
        libDir = "/" + libDir;
      }

      if (libDir.endsWith("/")) {
        libDir = libDir.substring(0, libDir.length() - 1);
      }

      // 尝试从jar包中查找库文件，然后加载
      System.load(generateTempLib(originLib, libDir));
    }

    log.info("Successfully loaded {}", originLib);
  }
}
