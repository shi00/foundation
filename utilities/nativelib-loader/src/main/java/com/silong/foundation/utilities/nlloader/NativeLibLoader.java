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

import static com.silong.foundation.utilities.nlloader.NativeLibsExtractor.extractNativeLibs;
import static com.silong.foundation.utilities.nlloader.PlatformDetector.*;
import static java.io.File.pathSeparator;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
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

  /** 动态库搜索路径列表 */
  private static final LinkedList<Path> DYNAMIC_LOAD_PATHS;

  static {
    Properties properties = new Properties();
    PLATFORM_DETECTOR.detect(properties, List.of());
    OS_NAME = properties.getProperty(DETECTED_NAME);
    OS_ARCH = properties.getProperty(DETECTED_ARCH);
    log.info("OS_NAME: {}", OS_NAME);
    log.info("OS_ARCH: {}", OS_ARCH);

    log.info("java.library.path: [");
    DYNAMIC_LOAD_PATHS =
        Arrays.stream(System.getProperty("java.library.path").split(pathSeparator))
            .map(File::new)
            .peek(f -> log.info(f.getAbsolutePath()))
            .map(File::toPath)
            .collect(Collectors.toCollection(LinkedList::new));
    log.info("]");

    assert !DYNAMIC_LOAD_PATHS.isEmpty() : "java.library.path is empty.";
  }

  /** 工具类，禁止实例化 */
  private NativeLibLoader() {}

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
  @SneakyThrows({java.net.URISyntaxException.class, IOException.class})
  public static void loadLibrary(String libName, String libDir) {
    if (libName == null || libName.isEmpty()) {
      throw new IllegalArgumentException("libName must not be null or empty.");
    }
    if (libDir == null) {
      throw new IllegalArgumentException("libDir must not be null.");
    }

    // 不能包含库格式，由运行环境决定
    if (Arrays.stream(PlatformLibFormat.values())
        .map(platformLibFormat -> platformLibFormat.libFormat)
        .anyMatch(libName::endsWith)) {
      throw new IllegalArgumentException(
          String.format("%s cannot contain library format suffix.", libName));
    }

    // 使用classloader从classpath中定位指定的本地库
    String libFormat = PlatformLibFormat.match(OS_NAME).libFormat;
    URL url =
        NativeLibLoader.class
            .getClassLoader()
            .getResource(
                libDir.isEmpty()
                    ? String.format("%s.%s", libName, libFormat)
                    : String.format("%s/%s.%s", libDir, libName, libFormat));
    if (url != null) {
      File file;
      boolean isJar = false;
      switch (url.getProtocol()) {
        case "file" -> {
          file = new File(url.toURI());
          extractNativeLibs(file.toPath().getParent(), DYNAMIC_LOAD_PATHS.getLast());
        }
        case "jar" -> {
          isJar = true;
          String jarUrl = url.getFile();
          // 提取 JAR 包路径和内部资源路径
          String[] parts = jarUrl.split("!/", 2);
          file = new File(new URL(parts[0]).toURI());
          extractNativeLibs(file.toPath(), DYNAMIC_LOAD_PATHS.getLast());
        }
        default ->
            throw new UnsupportedOperationException(String.format("Unsupported URL: %s", url));
      }

      System.loadLibrary(libName);
      log.info(
          "Successfully loaded {} from {}",
          libName,
          isJar ? file.getAbsolutePath() : file.getParentFile().getAbsolutePath());
    } else {
      log.error("Cannot find {} in the classpath.", String.format("%s.%s", libName, libFormat));
    }
  }
}
