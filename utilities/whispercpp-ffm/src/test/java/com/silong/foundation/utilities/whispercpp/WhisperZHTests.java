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
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import lombok.SneakyThrows;
import org.junit.jupiter.api.*;

/**
 * 单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-04-22 18:51
 */
public class WhisperZHTests {

  private static WhisperCpp whisperCpp;

  private long startTime;

  @BeforeAll
  static void init() {
    whisperCpp = WhisperCpp.getInstance(loadZHJsonFromClassPath());
  }

  @AfterAll
  static void cleanup() throws Exception {
    whisperCpp.close();
  }

  @BeforeEach
  void beforeTest() {
    startTime = System.currentTimeMillis();
  }

  @AfterEach
  void afterTest() {
    System.out.printf(
        "timeConsume: %dms%s", System.currentTimeMillis() - startTime, System.lineSeparator());
  }

  @SneakyThrows(IOException.class)
  private static WhisperConfig loadZHJsonFromClassPath() {
    try (InputStream inputStream = new FileInputStream("src/test/resources/application_zh.json")) {
      return JsonMapper.builder()
          .enable(ACCEPT_CASE_INSENSITIVE_ENUMS)
          .configure(FAIL_ON_UNKNOWN_PROPERTIES, true)
          .build()
          .readValue(inputStream, WhisperConfig.class);
    }
  }

  @Test
  public void testMultipleFormat() throws Exception {
    String[] text =
        whisperCpp.speech2Text(
            Paths.get(
                    ".",
                    "src",
                    "test",
                    "resources",
                    "男：你好，早上好。今天你看起来很有精神。女：你好呀，我今天睡得很好，所以精神很好。.wma")
                .toFile()
                .getCanonicalFile());
    String[] text1 =
        whisperCpp.speech2Text(
            Paths.get(
                    ".",
                    "src",
                    "test",
                    "resources",
                    "男：你好，早上好。今天你看起来很有精神。女：你好呀，我今天睡得很好，所以精神很好。.mp3")
                .toFile()
                .getCanonicalFile());

    assertArrayEquals(text, text1);
    String[] text2 =
        whisperCpp.speech2Text(
            Paths.get(
                    ".",
                    "src",
                    "test",
                    "resources",
                    "男：你好，早上好。今天你看起来很有精神。女：你好呀，我今天睡得很好，所以精神很好。.wav")
                .toFile()
                .getCanonicalFile());

    assertArrayEquals(text2, text1);

    String[] text3 =
        whisperCpp.speech2Text(
            Paths.get(
                    ".",
                    "src",
                    "test",
                    "resources",
                    "男：你好，早上好。今天你看起来很有精神。女：你好呀，我今天睡得很好，所以精神很好。.ogg")
                .toFile()
                .getCanonicalFile());

    assertArrayEquals(text2, text3);

    String[] text4 =
        whisperCpp.speech2Text(
            Paths.get(
                    ".",
                    "src",
                    "test",
                    "resources",
                    "男：你好，早上好。今天你看起来很有精神。女：你好呀，我今天睡得很好，所以精神很好。.m4a")
                .toFile()
                .getCanonicalFile());

    assertArrayEquals(text4, text3);

    String[] text5 =
        whisperCpp.speech2Text(
            Paths.get(
                    ".",
                    "src",
                    "test",
                    "resources",
                    "男：你好，早上好。今天你看起来很有精神。女：你好呀，我今天睡得很好，所以精神很好。.aac")
                .toFile()
                .getCanonicalFile());

    assertArrayEquals(text4, text5);

    String[] text6 =
        whisperCpp.speech2Text(
            Paths.get(
                    ".",
                    "src",
                    "test",
                    "resources",
                    "男：你好，早上好。今天你看起来很有精神。女：你好呀，我今天睡得很好，所以精神很好。.opus")
                .toFile()
                .getCanonicalFile());

    assertArrayEquals(text6, text5);
  }

  @Test
  public void testZH() throws Exception {
    String[] text =
        whisperCpp.speech2Text(
            Paths.get(
                    ".",
                    "src",
                    "test",
                    "resources",
                    "男：你好，早上好。今天你看起来很有精神。女：你好呀，我今天睡得很好，所以精神很好。.aac")
                .toFile()
                .getCanonicalFile());
    assertEquals("这个地方是观光名胜吗?", String.join("", text));
  }
}
