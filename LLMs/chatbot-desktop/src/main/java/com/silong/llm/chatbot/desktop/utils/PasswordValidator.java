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

package com.silong.llm.chatbot.desktop.utils;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.passay.*;

/**
 * 密码校验工具
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-19 20:19
 */
@Slf4j
public class PasswordValidator {

  private static final org.passay.PasswordValidator INSTANCE = buildPasswordValidator();

  /** 禁止实例化 */
  private PasswordValidator() {}

  /**
   * 校验密码
   *
   * @param password 密码
   * @return true or false
   */
  public static boolean validate(String password) {
    RuleResult ruleResult = INSTANCE.validate(new PasswordData(password));
    boolean valid = ruleResult.isValid();
    if (!valid) {
      log.error("Password validation failed. {}", ruleResult);
    }
    return valid;
  }

  private static org.passay.PasswordValidator buildPasswordValidator() {
    List<Rule> rules = new ArrayList<>();
    rules.add(new LengthRule(8, 16)); // 长度8-16
    rules.add(
        new CharacterCharacteristicsRule(
            3,
            new CharacterRule(EnglishCharacterData.Alphabetical, 1), // 字母
            new CharacterRule(EnglishCharacterData.Digit, 1), // 数字
            new CharacterRule(EnglishCharacterData.Special, 1) // 符号
            ));
    rules.add(new WhitespaceRule()); // 禁止空格
    return new org.passay.PasswordValidator(rules);
  }
}
