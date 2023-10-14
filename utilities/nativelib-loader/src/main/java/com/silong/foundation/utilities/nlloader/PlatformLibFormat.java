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

import java.util.Arrays;
import lombok.Getter;

/**
 * 操作系统平台
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-13 23:05
 */
@Getter
public enum PlatformLibFormat {

  /** Linux */
  LINUX("linux", "so"),

  /** Apple OS */
  OSX("osx", "dylib"),

  /** Windows */
  WINDOWS("windows", "dll");

  /** 操作系统民族 */
  final String osName;

  /** 共享库格式 */
  final String libFormat;

  PlatformLibFormat(String osName, String libFormat) {
    this.osName = osName;
    this.libFormat = libFormat;
  }

  /**
   * 根据操作系统名查找
   *
   * @param osName 操作系统名
   * @return true or false
   */
  public static PlatformLibFormat match(String osName) {
    return Arrays.stream(PlatformLibFormat.values())
        .filter(platformLibFormat -> platformLibFormat.osName.equals(osName))
        .findAny()
        .orElseThrow(
            () ->
                new UnsupportedOperationException(
                    "Only supports three operating systems: Linux, Windows and OSX."));
  }
}
