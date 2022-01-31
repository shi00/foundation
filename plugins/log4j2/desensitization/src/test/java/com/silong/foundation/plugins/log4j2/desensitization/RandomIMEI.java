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

import java.security.SecureRandom;
import java.util.Random;

import static java.lang.Math.abs;
import static java.lang.String.format;
import static java.util.Locale.ENGLISH;

/**
 * 随机IMEI工具
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-23 03:18
 */
public class RandomIMEI {

  private static final Random GENERATOR = new SecureRandom();

  private static final String[] IMEI_REPORTING_BODY_IDS = {
    "01", "10", "30", "33", "35", "44", "45", "49", "50", "51", "52", "53", "54", "86", "91", "98",
    "99"
  };

  private static int sumDigits(int number) {
    int a = 0;
    while (number > 0) {
      a = a + number % 10;
      number = number / 10;
    }
    return a;
  }

  public static String generateIMEI() {
    String first14 =
        format(
            "%s%.12s",
            IMEI_REPORTING_BODY_IDS[GENERATOR.nextInt(IMEI_REPORTING_BODY_IDS.length)],
            format(ENGLISH, "%012d", abs(GENERATOR.nextLong())));

    int sum = 0;

    for (int i = 0; i < first14.length(); i++) {
      int c = Character.digit(first14.charAt(i), 10);
      sum += (i % 2 == 0 ? c : sumDigits(c * 2));
    }

    int finalDigit = (10 - (sum % 10)) % 10;

    return first14 + finalDigit;
  }
}
