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
package com.silong.foundation.plugins.log4j2.desensitization.process;

import java.util.regex.Pattern;

/**
 * 基于正则表达式的敏感信息识别器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-02-28 14:43
 */
public class RegexSensitiveRecognizer implements SensitiveRecognizer {

  /** 正则表达式 */
  protected String regex;

  /** 表达式编译后模式 */
  protected Pattern pattern;

  /**
   * 构造方法
   *
   * @param regex 正则表达式
   */
  public RegexSensitiveRecognizer(String regex) {
    if (regex == null || regex.isEmpty()) {
      throw new IllegalArgumentException("regex must not be null or empty.");
    }
    this.regex = regex;
    this.pattern = Pattern.compile(regex);
  }

  @Override
  public String replace(String text) {
    if (text == null || text.isEmpty()) {
      throw new IllegalArgumentException("text must not be null or empty.");
    }
    return pattern.matcher(text).replaceAll(getMasker());
  }

  public String regex() {
    return regex;
  }

  public Pattern pattern() {
    return pattern;
  }
}
