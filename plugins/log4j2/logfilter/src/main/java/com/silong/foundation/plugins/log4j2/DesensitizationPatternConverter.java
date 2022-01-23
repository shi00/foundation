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
package com.silong.foundation.plugins.log4j2;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;

/**
 * 日志内容脱敏过滤器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-21 20:35
 */
@Plugin(name = "DesensitizationPatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({"dm", "dsm"})
public class DesensitizationPatternConverter extends LogEventPatternConverter {

  private static final ComposeDesensitizer DESENSITIZER = BuildInDesensitizer.getInstance();

  /** 是否开启日志脱敏 */
  private static final boolean ENABLED =
      Boolean.parseBoolean(System.getProperty("log.desensitization", "true"));

  /** 构造方法 */
  protected DesensitizationPatternConverter() {
    super("dm", "dsm");
  }

  /**
   * Log4j 2 also requires pattern converter to have a static newInstance method (with String array
   * as paramter).
   *
   * @param options 参数列表
   * @return 日志脱敏转换器
   */
  public static DesensitizationPatternConverter newInstance(String[] options) {
    return new DesensitizationPatternConverter();
  }

  @Override
  public void format(LogEvent event, StringBuilder toAppendTo) {
    String logMessage = event.getMessage().getFormattedMessage();
    toAppendTo.append(ENABLED ? DESENSITIZER.desensitize(logMessage) : logMessage);
  }
}
