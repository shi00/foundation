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
package com.silong.foundation.devastator.utils;

/**
 * 版本兼容性工具
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-07-02 10:28
 */
public interface VersionUtils {
  /**
   * 版本是否兼容，version1是否兼容version2
   *
   * @param version1 版本1
   * @param version2 版本2
   * @return true or false
   */
  static boolean isCompatible(String version1, String version2) {
    if (version1 == null || version2 == null) {
      throw new IllegalArgumentException("version1 or version2 must not be null.");
    }
    if (version1.equals(version2)) {
      return true;
    }
    return false;
  }
}
