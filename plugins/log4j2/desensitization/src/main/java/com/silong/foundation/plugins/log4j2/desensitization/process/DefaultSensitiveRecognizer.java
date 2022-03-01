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

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 默认正则表达式敏感信息识别器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-02-28 14:28
 */
public class DefaultSensitiveRecognizer implements SensitiveRecognizer {

  /** 默认正则表达式配置文件路径 */
  public static final String REGEX_PATH = "default-sensitive-regexs.properties";

  private final List<SensitiveRecognizer> regexSensitiveRecognizers;

  /** 默认构造方法 */
  public DefaultSensitiveRecognizer() {
    this.regexSensitiveRecognizers =
        loadConfiguration(REGEX_PATH).stream()
            .map(RegexSensitiveRecognizer::new)
            .collect(Collectors.toList());
  }

  /**
   * 从classpath加载正则表达式配置
   *
   * @param path 配置文件路径
   * @return 正则表达式列表
   */
  protected List<String> loadConfiguration(String path) {
    if (path == null || path.isEmpty()) {
      throw new IllegalArgumentException("path must not be null or empty.");
    }
    try (InputStream inputStream =
        Objects.requireNonNull(
            getClass().getClassLoader().getResourceAsStream(path),
            String.format("Failed to load config from %s", path))) {
      Properties properties = new Properties();
      properties.load(inputStream);
      return properties.values().stream()
          .map(String::valueOf)
          .filter(regex -> regex != null && !regex.isEmpty())
          .distinct()
          .collect(Collectors.toList());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private record Tuple(String replacedText, SensitiveRecognizer recognizer) {}

  @Override
  public String replace(String text) {
    if (text == null || text.isEmpty()) {
      throw new IllegalArgumentException("text must not be null or empty.");
    }
    String result;
    List<SensitiveRecognizer> recognizers = regexSensitiveRecognizers;
    LinkedList<Tuple> list = new LinkedList<>();
    while (true) {
      for (SensitiveRecognizer recognizer : recognizers) {
        String replaceAll = recognizer.replace(text);
        if (!replaceAll.equals(text)) {
          list.add(new Tuple(replaceAll, recognizer));
        }
      }
      if (list.isEmpty()) {
        result = text;
        break;
      }
      if (list.size() == 1) {
        result = list.getFirst().replacedText;
        break;
      }
      // 获取最长匹配结果作为其他识别器输入
      list.sort(Comparator.comparingInt(t -> t.replacedText.length()));
      text = list.removeFirst().replacedText;
      recognizers = list.stream().map(Tuple::recognizer).collect(Collectors.toList());
    }
    return result;
  }
}
