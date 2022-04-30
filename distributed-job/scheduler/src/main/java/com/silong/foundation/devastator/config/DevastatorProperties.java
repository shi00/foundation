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
import lombok.EqualsAndHashCode;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.io.Serializable;
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
public final class DevastatorProperties implements Serializable {

  @Serial private static final long serialVersionUID = 0L;

  /** 软件包配置 */
  private static final Properties DEVASTATOR;

  /** 配置文件路径 */
  public static final String DEVASTATOR_PROPERTIES = "META-INF/devastator.properties";

  /** 软件版本 */
  private static final Version VERSION;

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

    VERSION = getVersionInternal();
  }

  /** 禁止实例化 */
  private DevastatorProperties() {}

  /**
   * 全部属性
   *
   * @return 全属性
   */
  public static Properties properties() {
    return DEVASTATOR;
  }

  /**
   * 获取软件版本
   *
   * @return 版本
   */
  public static Version version() {
    return VERSION;
  }

  /**
   * 获取软件包版本号
   *
   * @return 软件版本号
   */
  private static Version getVersionInternal() {
    return new Version(DEVASTATOR.getProperty(Version.VERSION, "0.0.1"));
  }

  /** 软件版本 */
  @EqualsAndHashCode
  public static final class Version implements Serializable {

    @Serial private static final long serialVersionUID = 0L;

    /** 版本key */
    public static final String VERSION = "version";

    /** 版本 */
    private final short version;

    Version(String version) {
      this.version = org.jgroups.Version.parse(version);
    }

    /**
     * 获取软件包版本号
     *
     * @return 软件版本号
     */
    public short getVersion() {
      return version;
    }

    /**
     * 解析字符串版本号
     *
     * @param version 字符串版本号
     * @return 版本
     */
    public static Version parse(String version) {
      return new Version(version);
    }

    /**
     * 解析数字版本号
     *
     * @param version 数字版本号
     * @return 版本
     */
    public static Version parse(short version) {
      StringJoiner joiner = new StringJoiner(".");
      for (short i : org.jgroups.Version.decode(version)) {
        joiner.add(String.valueOf(i));
      }
      return new Version(joiner.toString());
    }

    @Override
    public String toString() {
      return String.valueOf(version);
    }
  }
}
