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
package com.silong.foundation.utilities.password;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-02-28 10:03
 */
public class PasswordsTests {

  @Test
  @DisplayName("password-generate-8")
  public void test1() {
    int length = Passwords.MIN_LENGTH;
    String password = Passwords.generate(length);
    Assertions.assertEquals(password.length(), length);
  }

  @Test
  @DisplayName("password-generate-32")
  public void test2() {
    int length = Passwords.MAX_LENGTH;
    String password = Passwords.generate(length);
    Assertions.assertEquals(password.length(), length);
  }

  @Test
  @DisplayName("password-generate-7")
  public void test3() {
    int length = Passwords.MIN_LENGTH - 1;
    Assertions.assertThrows(Exception.class, () -> Passwords.generate(length));
  }

  @Test
  @DisplayName("password-generate-43")
  public void test4() {
    int length = Passwords.MAX_LENGTH + 1;
    Assertions.assertThrows(Exception.class, () -> Passwords.generate(length));
  }

  @Test
  @DisplayName("password-generate-validator")
  public void test5() {
    for (int i = 0; i < 1000; i++) {
      int length = RandomUtils.nextInt(Passwords.MIN_LENGTH, Passwords.MAX_LENGTH + 1);
      String password = Passwords.generate(length);
      Passwords.Result result = Passwords.validate(password);
      Assertions.assertTrue(result.isValid() && result.strength().ordinal() > 1);
    }
  }
}
