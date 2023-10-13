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

package com.silong.foundation.utilities.xxhash;

import static com.silong.foundation.utilities.xxhash.generated.Xxhash.*;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.SneakyThrows;

/**
 * 提供xxhash生成工具
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-09-16 14:52
 */
public final class XxHashUtils {

  /** 共享库名称 */
  private static final String LIB_XXHASH = "libxxhash";

  static {
    loadNativeLib();
  }

  @SneakyThrows
  private static void loadNativeLib() {
    // 仅支持64bit操作系统
    int osArch = Integer.parseInt(System.getProperty("sun.arch.data.model"));
    if (osArch != 64) {
      throw new UnsupportedOperationException("Only supported on 64bit operating systems.");
    }

    String osName = System.getProperty("os.name");
    if (osName.startsWith("Windows")) {
      System.load(generateTempLib(LIB_XXHASH + ".dll"));
    } else if (osName.startsWith("Linux")) {
      System.load(generateTempLib(LIB_XXHASH + ".so"));
    } else {
      throw new UnsupportedOperationException("Only supports Windows or Linux operating systems.");
    }
  }

  private static String generateTempLib(String originLib) throws IOException {
    long time = System.nanoTime();
    Path path = new File(System.getProperty("java.io.tmpdir")).toPath();
    int index = originLib.lastIndexOf('.');
    String prefix = originLib.substring(0, index - 1);
    String suffix = originLib.substring(index);
    String name = String.format("%s_%d%s", prefix, time, suffix);
    Path tmpLib = path.resolve(name);
    try (InputStream inputStream =
        XxHashUtils.class.getResourceAsStream(String.format("/%s", originLib))) {
      Files.copy(requireNonNull(inputStream), tmpLib, REPLACE_EXISTING);
      File file = tmpLib.toFile();
      file.deleteOnExit();
      return file.getCanonicalPath();
    }
  }

  @SneakyThrows
  private static void getDelete(Path path) {
    Files.delete(path);
  }

  /** 工具类禁止实例化 */
  private XxHashUtils() {}

  /**
   * 生成xxhash
   *
   * @param data 数据
   * @return hash码
   */
  public static long hash64(byte[] data) {
    if (data == null || data.length == 0) {
      return 0;
    }
    try (Arena arena = Arena.ofConfined()) {
      return XXH3_64bits_withSeed(arena.allocateArray(JAVA_BYTE, data), data.length, 0xcafebabeL);
    }
  }

  /**
   * 生成xxhash
   *
   * @param data 数据
   * @return hash码
   */
  public static long hash32(byte[] data) {
    if (data == null || data.length == 0) {
      return 0;
    }
    try (Arena arena = Arena.ofConfined()) {
      return XXH32(arena.allocateArray(JAVA_BYTE, data), data.length, 0xcafebabe);
    }
  }
}
