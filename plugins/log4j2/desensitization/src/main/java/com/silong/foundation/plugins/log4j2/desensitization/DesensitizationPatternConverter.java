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

import com.silong.foundation.plugins.log4j2.desensitization.process.DefaultSensitiveRecognizer;
import com.silong.foundation.plugins.log4j2.desensitization.process.SensitiveRecognizer;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;

import java.lang.reflect.InvocationTargetException;

/**
 * 日志内容脱敏过滤器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-21 20:35
 */
@Plugin(name = "DesensitizationPatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({"dsm"})
public class DesensitizationPatternConverter extends LogEventPatternConverter {

  /** 敏感信息识别器实现类 */
  private static final String DESENSITIVE_RECOGNIZER_CLASS_NAME =
      System.getProperty("log4j2.desensitization", DefaultSensitiveRecognizer.class.getName());

  private final SensitiveRecognizer recognizer;

  /** 默认构造方法 */
  protected DesensitizationPatternConverter() {
    super("dsm", "dsm");
    try {
      this.recognizer =
          (SensitiveRecognizer)
              Class.forName(DESENSITIVE_RECOGNIZER_CLASS_NAME)
                  .getDeclaredConstructor()
                  .newInstance();
    } catch (ClassNotFoundException
        | IllegalAccessException
        | NoSuchMethodException
        | InstantiationException
        | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Log4j 2 also requires pattern converter to have a static newInstance method (with String array
   * as paramter).
   *
   * @param options 参数列表
   * @return 日志脱敏转换器
   */
  public static DesensitizationPatternConverter newInstance(final String[] options) {
    return new DesensitizationPatternConverter();
  }

  @Override
  public void format(LogEvent event, StringBuilder toAppendTo) {
    toAppendTo.append(recognizer.replace(event.getMessage().getFormattedMessage()));
  }
}
