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

import static org.apache.commons.lang3.SystemUtils.*;

import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-01-03 11:43
 */
public class LoaderTests {

  private static final String LIB_PREFIX = "librocksdbjni";

  @Test
  void test1() {
    String lib = "";
    if (IS_OS_WINDOWS && OS_ARCH.contains("64")) {
      lib = LIB_PREFIX + "-win64";
    } else if (SystemUtils.IS_OS_LINUX) {
      if (OS_ARCH.contains("64")) {
        lib = LIB_PREFIX + "-linux64";
      } else {
        lib = LIB_PREFIX + "-linux32";
      }
    }
    String ll = lib;
    Assertions.assertDoesNotThrow(() -> NativeLibLoader.loadLibrary(ll));
  }

  @Test
  void test2() {
    if (IS_OS_WINDOWS)
      Assertions.assertDoesNotThrow(
          () -> NativeLibLoader.loadLibrary("libwhisper", "windows_native_libs"));
  }

  @Test
  void test3() {
    if (IS_OS_LINUX)
      Assertions.assertDoesNotThrow(
          () -> NativeLibLoader.loadLibrary("libwhisper", "linux_native_libs"));
  }
}
