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

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 脱敏器单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-22 22:11
 */
public class DesensitizerTests {

  private static String email =
      "security:(?:[A-Za-z0-9+\\/]{4})*(?:[A-Za-z0-9+\\/]{4}|[A-Za-z0-9+\\/]{3}=|[A-Za-z0-9+\\/]{2}={2})";

  @Test
  public void test() {
    Matcher matcher =
        Pattern.compile(email)
            .matcher(
                "2131231security:"
                    + Base64.getEncoder().encodeToString(RandomUtils.nextBytes(10))
                    + "jdhsakj1231");
    System.out.println(matcher.find());
    String group = matcher.group(0);
    System.out.println(group);
  }
}
