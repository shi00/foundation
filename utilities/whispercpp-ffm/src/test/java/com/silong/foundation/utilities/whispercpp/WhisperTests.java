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

package com.silong.foundation.utilities.whispercpp;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS;
import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-04-22 18:51
 */
public class WhisperTests {

  private final Whisper whisper = Whisper.getInstance(loadJsonFromClassPath());

  @SneakyThrows(IOException.class)
  private static WhisperConfig loadJsonFromClassPath() {
    try (InputStream inputStream =
        requireNonNull(
            WhisperTests.class.getClassLoader().getResourceAsStream("application.json"))) {
      return JsonMapper.builder()
          .enable(ACCEPT_CASE_INSENSITIVE_ENUMS)
          .configure(FAIL_ON_UNKNOWN_PROPERTIES, true)
          .build()
          .readValue(inputStream, WhisperConfig.class);
    }
  }

  @Test
  public void test1() throws IOException {
    String text =
        whisper.speech2Text(
            Paths.get(".", "src", "test", "resources", "Have-you-seen-one-of-these.wav")
                .toFile()
                .getCanonicalFile());
    Assertions.assertEquals(" Have you seen one of these?", text);
  }
}
