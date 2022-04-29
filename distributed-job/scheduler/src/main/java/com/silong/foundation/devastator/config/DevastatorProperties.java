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
package com.silong.foundation.devastator.config;

import com.silong.foundation.devastator.exception.InitializationException;
import org.jgroups.Version;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.StringJoiner;

/**
 * 软件包信息 <br>
 * 软件版本号组合公式: major.minor.micro <br>
 * X = 0-31 for major versions <br>
 * Y = 0-31 for minor versions <br>
 * Z = 0-63 for micro versions <br>
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-28 22:11
 */
@SuppressWarnings("URLCONNECTION_SSRF_FD")
public final class DevastatorProperties {

  /** 软件包配置 */
  private static final Properties DEVASTATOR;

  /** 配置文件路径 */
  public static final String DEVASTATOR_PROPERTIES = "META-INF/devastator.properties";

  static {
    try (InputStream input =
        DevastatorProperties.class.getClassLoader().getResourceAsStream(DEVASTATOR_PROPERTIES)) {
      if (input == null) {
        throw new InitializationException(
            "Failed to open file input stream for " + DEVASTATOR_PROPERTIES);
      }
      DEVASTATOR = new Properties();
      DEVASTATOR.load(input);
    } catch (IOException e) {
      throw new InitializationException("Failed to load " + DEVASTATOR_PROPERTIES, e);
    }
  }

  /** 禁止实例化 */
  private DevastatorProperties() {}

  /**
   * 获取软件包版本号
   *
   * @return 软件版本号
   */
  public static short getVersionNumber() {
    return Version.parse(getVersionString());
  }

  /**
   * 解析字符串版本号为数字版本号
   *
   * @param version 字符串版本号
   * @return 数字版本号
   */
  public static short parse(String version) {
    return Version.parse(version);
  }

  /**
   * 解析数字版本号为字符串版本号
   *
   * @param version 数字版本号
   * @return 字符串版本号
   */
  public static String parse(short version) {
    StringJoiner joiner = new StringJoiner(".");
    for (short i : Version.decode(version)) {
      joiner.add(String.valueOf(i));
    }
    return joiner.toString();
  }

  /**
   * 获取软件包版本号
   *
   * @return 软件版本号
   */
  public static String getVersionString() {
    return DEVASTATOR.getProperty("version");
  }
}
