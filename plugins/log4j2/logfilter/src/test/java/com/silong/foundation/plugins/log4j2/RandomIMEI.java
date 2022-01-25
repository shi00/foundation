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

/**
 * 随机IMEI工具
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-23 03:18
 */
public interface RandomIMEI {
  static String generateIMEI() {
    int pos;
    int[] str = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    int sum = 0;
    int final_digit;
    int t;
    int len_offset;
    int len = 15;
    String imei = "";

    String[] rbi =
        new String[] {
          "01", "10", "30", "33", "35", "44", "45", "49", "50", "51", "52", "53", "54", "86", "91",
          "98", "99"
        };
    String[] arr = rbi[(int) Math.floor(Math.random() * rbi.length)].split("");
    str[0] = Integer.parseInt(arr[0]);
    str[1] = Integer.parseInt(arr[1]);
    pos = 2;

    while (pos < len - 1) {
      str[pos++] = (int) (Math.floor(Math.random() * 10) % 10);
    }

    len_offset = (len + 1) % 2;
    for (pos = 0; pos < len - 1; pos++) {
      if ((pos + len_offset) % 2 != 0) {
        t = str[pos] * 2;
        if (t > 9) {
          t -= 9;
        }
        sum += t;
      } else {
        sum += str[pos];
      }
    }

    final_digit = (10 - (sum % 10)) % 10;
    str[len - 1] = final_digit;

    for (int d : str) {
      imei += String.valueOf(d);
    }

    return imei;
  }
}
