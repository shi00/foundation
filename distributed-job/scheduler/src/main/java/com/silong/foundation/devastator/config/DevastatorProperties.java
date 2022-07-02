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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 软件包配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-28 22:11
 */
@SuppressWarnings("URLCONNECTION_SSRF_FD")
public final class DevastatorProperties implements Serializable {

  @Serial private static final long serialVersionUID = -582920829207838110L;

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

  /**
   * 软件包信息 <br>
   * 软件版本号组合公式: major.minor.micro <br>
   * X = 0-31 for major versions <br>
   * Y = 0-31 for minor versions <br>
   * Z = 0-63 for micro versions <br>
   */
  @EqualsAndHashCode
  public static final class Version implements Serializable {

    @Serial private static final long serialVersionUID = -5038509713401088943L;

    /** 版本字符串正则表达式 */
    private static final Pattern VERSION_REGEXP = Pattern.compile("((\\d+)\\.(\\d+)\\.(\\d+).*)");

    private static final int MAJOR_SHIFT = 11;

    private static final int MINOR_SHIFT = 6;

    /** 1111100000000000 bit mask */
    private static final int MAJOR_MASK = 0x00f800;

    /** 11111000000 bit mask */
    private static final int MINOR_MASK = 0x0007c0;

    /** 111111 bit mask */
    private static final int MICRO_MASK = 0x00003f;

    /** 版本key */
    public static final String VERSION = "version";

    /** 版本 */
    private final short version;

    private Version(String version) {
      this.version = parseVerStr(version);
    }

    private static short parseVerStr(String ver) {
      try {
        Matcher versionMatcher = VERSION_REGEXP.matcher(ver);
        if (versionMatcher.find()) {
          short maj = Short.parseShort(versionMatcher.group(2));
          short min = Short.parseShort(versionMatcher.group(3));
          short mic = Short.parseShort(versionMatcher.group(4));
          return encode(maj, min, mic);
        }
      } catch (Throwable t) {
        throw new IllegalArgumentException(
            String.format("Failed to parse version '%s': %s.", ver, t));
      }
      throw new IllegalArgumentException(String.format("Failed to parse version '%s'.", ver));
    }

    private static short encode(int major, int minor, int micro) {
      return (short) ((major << MAJOR_SHIFT) + (minor << MINOR_SHIFT) + micro);
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
      for (short i : decode(version)) {
        joiner.add(String.valueOf(i));
      }
      return new Version(joiner.toString());
    }

    private static short[] decode(short version) {
      short major = (short) ((version & MAJOR_MASK) >> MAJOR_SHIFT);
      short minor = (short) ((version & MINOR_MASK) >> MINOR_SHIFT);
      short micro = (short) (version & MICRO_MASK);
      return new short[] {major, minor, micro};
    }

    @Override
    public String toString() {
      return String.valueOf(version);
    }
  }
}
