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
import static java.io.File.pathSeparator;
import static org.apache.commons.lang3.SystemProperties.getJavaLibraryPath;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

/**
 * 从classpath包含的jar包中加载指定的共享库，配合本地方法使用
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-13 22:55
 */
@Slf4j
@SuppressFBWarnings(
    value = "PATH_TRAVERSAL_IN",
    justification = "Reference the path in the apache commons lang3")
public final class NativeLibLoader {

  /** 动态库写入路径 */
  public static final Path SELECTED_JAVA_LOAD_PATH =
      Arrays.stream(getJavaLibraryPath().split(pathSeparator))
          .filter(StringUtils::isNotEmpty)
          .map(File::new)
          .filter(f -> f.isDirectory() && f.canWrite())
          .peek(
              f ->
                  log.info(
                      "Choose directory: {} to write the temporary shared library.",
                      f.getAbsolutePath()))
          .reduce((first, second) -> second) // 取最后一个可写目录
          .orElseThrow(
              () ->
                  new IllegalStateException(
                      "No writable directory found in ${java.library.path}: "
                          + getJavaLibraryPath()))
          .toPath();

  /** 默认本地库在classpath中存放的目录 */
  private static final String DEFAULT_LIB_DIR = "";

  /** 工具类，禁止实例化 */
  private NativeLibLoader() {}

  /**
   * 操作系统检测到的分类器
   *
   * @return 分类器字符串，如 linux-x86_64、windows-x86_64、osx-arm64 等
   */
  public static String getOSDetectedClassifier() {
    // 操作系统类型
    String osType;
    if (SystemUtils.IS_OS_LINUX) {
      osType = "linux";
    } else if (SystemUtils.IS_OS_WINDOWS) {
      osType = "windows";
    } else if (SystemUtils.IS_OS_MAC) {
      osType = "osx";
    } else {
      throw new IllegalStateException("Unsupported OS: " + SystemUtils.OS_NAME);
    }

    // 系统架构（需手动映射标准化）
    String arch = SystemUtils.OS_ARCH.toLowerCase();
    if (arch.contains("amd64") || arch.contains("x86_64")) {
      arch = "x86_64";
    } else if (arch.contains("arm64") || arch.contains("aarch64")) {
      arch = "arm64";
    } else {
      throw new IllegalStateException("Unsupported OS-ARCH: " + arch);
    }

    return osType + "-" + arch;
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
  @SneakyThrows({URISyntaxException.class, IOException.class})
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
    String libFormat = PlatformLibFormat.get().libFormat;
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
          extractNativeLibs(file.toPath().getParent(), SELECTED_JAVA_LOAD_PATH);
        }
        case "jar" -> {
          isJar = true;
          String jarUrl = url.getFile();
          // 提取 JAR 包路径和内部资源路径
          String[] parts = jarUrl.split("!/", 2);
          file = new File(new URL(parts[0]).toURI());
          extractNativeLibs(file.toPath(), SELECTED_JAVA_LOAD_PATH);
        }
        default ->
            throw new UnsupportedOperationException(String.format("Unsupported URL: %s", url));
      }

      Path libPath = SELECTED_JAVA_LOAD_PATH.resolve(libName + "." + libFormat);
      if (!Files.exists(libPath)) {
        throw new DetectionException("Failed to extract native library: " + libPath);
      }
      System.load(libPath.toFile().getAbsolutePath());
      log.info(
          "Successfully loaded {} from {}",
          libName,
          isJar ? file.getAbsolutePath() : file.getParentFile().getAbsolutePath());
    } else {
      throw new DetectionException(
          String.format("Cannot find %s.%s in the classpath.", libName, libFormat));
    }
  }
}
