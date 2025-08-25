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
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;

import java.util.Arrays;
import java.util.function.Supplier;
import lombok.Getter;

/**
 * 操作系统平台
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-13 23:05
 */
public enum PlatformLibFormat {

  /** Apple OS */
  MAC(() -> IS_OS_MAC || IS_OS_MAC_OSX, "dylib"),

  /** UNIX */
  UNIX(() -> IS_OS_UNIX && !IS_OS_MAC_OSX, "so"),

  /** Windows */
  WINDOWS(() -> IS_OS_WINDOWS, "dll");

  final Supplier<Boolean> detector;

  /** 共享库格式 */
  @Getter final String libFormat;

  PlatformLibFormat(Supplier<Boolean> detector, String libFormat) {
    this.detector = detector;
    this.libFormat = libFormat;
  }

  /**
   * 获取当前程序运行平台上的动态库格式
   *
   * @return 格式
   */
  public static PlatformLibFormat get() {
    return Arrays.stream(PlatformLibFormat.values())
        .filter(platformLibFormat -> platformLibFormat.detector.get())
        .findAny()
        .orElseThrow(
            () ->
                new UnsupportedOperationException(
                    "Unsupported operating system. Only supports Unix, Windows and MAC."));
  }
}
