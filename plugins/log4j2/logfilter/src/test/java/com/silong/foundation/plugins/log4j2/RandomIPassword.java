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

import org.passay.CharacterData;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;

/**
 * 随机密码工具
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-23 03:18
 */
public interface RandomIPassword {
  CharacterRule DIGITS = new CharacterRule(EnglishCharacterData.Digit);
  CharacterRule UPPERCASE = new CharacterRule(EnglishCharacterData.UpperCase);
  CharacterRule LOWERCASE = new CharacterRule(EnglishCharacterData.LowerCase);
  CharacterRule SPECIAL =
      new CharacterRule(
          new CharacterData() {
            @Override
            public String getErrorCode() {
              return "INSUFFICIENT_SPECIAL";
            }

            @Override
            public String getCharacters() {
              return "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";
            }
          });

  static String generatePassword(int length) {
    PasswordGenerator passwordGenerator = new PasswordGenerator();
    return passwordGenerator.generatePassword(length, DIGITS, UPPERCASE, LOWERCASE, SPECIAL);
  }
}
