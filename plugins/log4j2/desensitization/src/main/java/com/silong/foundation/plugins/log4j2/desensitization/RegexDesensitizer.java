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
package com.silong.foundation.plugins.log4j2.desensitization;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

import java.util.regex.Pattern;

/**
 * 正则表达式脱敏
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-21 22:03
 */
@Data
public class RegexDesensitizer implements Desensitizer {

  /** 正则表达式 */
  @EqualsAndHashCode.Exclude @ToString.Exclude private final Pattern pattern;

  /** 替换符 */
  private final String replacement;

  /** 正则表达式 */
  private final String regex;

  /**
   * 构造方法
   *
   * @param regex 正则表达式
   * @param replacement 敏感信息替换字符串
   */
  public RegexDesensitizer(@NonNull String regex, @NonNull String replacement) {
    this.pattern = Pattern.compile(regex);
    this.replacement = replacement;
    this.regex = regex;
  }

  /**
   * 构造方法
   *
   * @param regex 正则表达式
   */
  public RegexDesensitizer(String regex) {
    this(regex, DEFAULT_REPLACE_STR);
  }

  @Override
  public String desensitize(@NonNull String msg) {
    return pattern.matcher(msg).replaceAll(replacement);
  }

  @Override
  public String id() {
    return regex;
  }
}
