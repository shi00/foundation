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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 链式敏感信息识别器，可以组合多个识别器按顺序进行敏感信息识别
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-03-01 08:01
 */
public class ChainSensitiveRecognizer implements SensitiveRecognizer {

  private final List<SensitiveRecognizer> recognizers;

  /**
   * 构造方法
   *
   * @param recognizers 识别器列表
   */
  public ChainSensitiveRecognizer(SensitiveRecognizer... recognizers) {
    this.recognizers =
        Arrays.stream(Objects.requireNonNull(recognizers, "recognizers must not be null."))
            .toList();
  }

  @Override
  public String replace(String text) {
    for (SensitiveRecognizer recognizer : recognizers) {
      text = recognizer.replace(text);
    }
    return text;
  }
}
