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

package com.silong.foundation.dj.bonecrusher.configure;

import io.netty.handler.logging.LogLevel;
import java.util.Arrays;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * 类型转换器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-24 16:40
 */
@Slf4j
@Component
@ConfigurationPropertiesBinding
public class LogLevelConvertor implements Converter<String, LogLevel> {
  @Override
  public LogLevel convert(String source) {
    String upper = source.toUpperCase(Locale.ENGLISH);
    return Arrays.stream(LogLevel.values())
        .filter(logLevel -> logLevel.name().equals(upper))
        .findAny()
        .orElseGet(
            () -> {
              log.info("No valid log level configured, default log level enabled: INFO.");
              return LogLevel.INFO;
            });
  }
}
