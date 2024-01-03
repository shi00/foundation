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
  static final Path TEMP_DIR = new File(System.getProperty("java.io.tmpdir")).toPath();

  /** 操作系统名 */
  static final String OS_NAME;

  /** 操作系统名 */
  static final String OS_ARCH;

  static {
    Properties properties = new Properties();
    PLATFORM_DETECTOR.detect(properties, List.of());
    OS_NAME = properties.getProperty(DETECTED_NAME);
    OS_ARCH = properties.getProperty(DETECTED_ARCH);
    log.info(STR."OS_NAME: \{OS_NAME}");
    log.info(STR."OS_ARCH: \{OS_ARCH}");
  }

  /** 工具类，禁止实例化 */
  private NativeLibLoader() {}

  /**
   * 在用户临时目录下生成库文件
   *
   * @param originLib 类库名
   * @return 生成的临时类库文件标准路径
   * @throws IOException 异常
   */
  private static String generateTempLib(String originLib) throws IOException {
    int index = originLib.lastIndexOf('.');
    String prefix = originLib.substring(0, index - 1);
    String suffix = originLib.substring(index);
    Path tmpLib = TEMP_DIR.resolve(String.format("%s_%d%s", prefix, System.nanoTime(), suffix));
    String libPath = String.format("/native/%s/%s/%s", OS_NAME, OS_ARCH, originLib);
    try {
      return doInternalGenerate(originLib, libPath, tmpLib);
    } catch (IOException e) {
      // fallback
      libPath = String.format("/native/%s/%s", OS_NAME, originLib);
      try {
        return doInternalGenerate(originLib, libPath, tmpLib);
      } catch (IOException ex) {
        // fallback
        libPath = String.format("/native/%s", originLib);
        try {
          return doInternalGenerate(originLib, libPath, tmpLib);
        } catch (IOException exc) {
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
  @SneakyThrows(IOException.class)
  public static void loadLibrary(String libName) {
    if (libName == null || libName.isEmpty()) {
      throw new IllegalArgumentException("libName must not be null or empty.");
    }

    // 不能包含库格式，由运行环境决定
    if (Arrays.stream(PlatformLibFormat.values())
        .map(platformLibFormat -> platformLibFormat.libFormat)
        .anyMatch(libName::endsWith)) {
      throw new IllegalArgumentException("libName cannot contain library format suffix.");
    }

    // 加载临时生成的库文件
    String originLib = String.format("%s.%s", libName, PlatformLibFormat.match(OS_NAME).libFormat);
    System.load(generateTempLib(originLib));
    log.info(STR."Successfully loaded \{originLib}");
  }
}
