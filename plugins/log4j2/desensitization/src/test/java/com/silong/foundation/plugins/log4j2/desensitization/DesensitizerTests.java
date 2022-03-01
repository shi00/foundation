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

import com.github.javafaker.Faker;
import com.silong.foundation.plugins.log4j2.desensitization.process.DefaultSensitiveRecognizer;
import com.silong.foundation.plugins.log4j2.desensitization.process.SensitiveRecognizer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 脱敏器单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-22 22:11
 */
public class DesensitizerTests {

  private static final SensitiveRecognizer RECOGNIZER = new DefaultSensitiveRecognizer();

  private static final Faker FAKER = new Faker();

  @Test
  @DisplayName("email")
  public void test1() {
    String emailAddress = FAKER.internet().emailAddress();
    String replace = RECOGNIZER.replace(emailAddress);
    Assertions.assertEquals(RECOGNIZER.getMasker(), replace);
  }
}
