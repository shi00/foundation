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

import com.nulabinc.zxcvbn.Zxcvbn;
import org.passay.CharacterData;
import org.passay.*;

import java.io.Closeable;
import java.util.List;

import static org.passay.EnglishCharacterData.*;

/**
 * 随机密码生成工具
 *
 * <pre>
 * 密码生成规则如下：
 * 1.至少包含一个特殊字符，特殊字符：!"#$%&'()*+,-./:;<=>?@[\]^_`{|}~
 * 2.至少包含一个数字
 * 3.至少包含一个大写字母
 * 4.至少包含一个小写字母
 * 5.密码长度在8~32之间
 * </pre>
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-02-27 10:09
 */
public final class Passwords implements Closeable {
  /** 最小密码长度 */
  public static final int MIN_LENGTH = 8;

  /** 最大密码长度 */
  public static final int MAX_LENGTH = 32;

  /** 特殊字符 */
  private static final CharacterRule SPECIAL =
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
          },
          1);

  /** 小写字母 */
  private static final CharacterRule UPPER_CASE = new CharacterRule(UpperCase, 1);

  /** 小写字母 */
  private static final CharacterRule LOWER_CASE = new CharacterRule(LowerCase, 1);

  /** 数字 */
  private static final CharacterRule DIGITS = new CharacterRule(Digit, 1);

  /**
   * 密码强度评估器
   */
  private static final Zxcvbn ESTIMATOR = new Zxcvbn();

  /**
   * 密码生成规则集
   */
  private static final List<CharacterRule> CHARACTER_RULES =
      List.of(DIGITS, LOWER_CASE, UPPER_CASE, SPECIAL);

  private static final ThreadLocal<PasswordGenerator> PASSWORD_GENERATOR_THREAD_LOCAL =
      ThreadLocal.withInitial(PasswordGenerator::new);

  private static final ThreadLocal<PasswordValidator> PASSWORD_VALIDATOR_THREAD_LOCAL =
          ThreadLocal.withInitial(()->new PasswordValidator(CHARACTER_RULES));


  /** 密码强度枚举 */
  public enum Strength {
    /** 弱 */
    WEAK,
    /** 一般 */
    FAIR,
    /** 好 */
    GOOD,
    /** 强 */
    STRONG,
    /** 非常强 */
    VERY_STRONG
  }

  /** 密码校验结果 */
  public record Result(boolean isValid, Strength strength) {}

  /**
   *
   *
   * <pre>
   * 按预定规则生成随机密码，生成密码长度可指定。
   * 1.至少包含一个特殊字符，特殊字符：!"#$%&'()*+,-./:;<=>?@[\]^_`{|}~
   * 2.至少包含一个数字
   * 3.至少包含一个大写字母
   * 4.至少包含一个小写字母
   * </pre>
   *
   * @param passwordLength 生成密码长度，取值范围[8,32]
   * @return 随机密码
   */
  public static String generate(int passwordLength) {
    if (passwordLength < MIN_LENGTH || MAX_LENGTH < passwordLength) {
      throw new IllegalArgumentException(
          String.format(
              "passwordLength must be greater than or equal to %d and less than or equal to %d",
              MIN_LENGTH, MAX_LENGTH));
    }
    return PASSWORD_GENERATOR_THREAD_LOCAL.get().generatePassword(passwordLength, CHARACTER_RULES);
  }

  /**
   * 校验密码是否满足复杂度要求，如果密码满足复杂度要求则评估密码复杂度
   *
   * @param password 密码
   * @return 校验结果
   */
  public static Result validate(String password) {
    if (password == null || password.isEmpty()) {
      throw new IllegalArgumentException("password must not be null or empty.");
    }
    int passwordLength = password.length();
    if (passwordLength < MIN_LENGTH || MAX_LENGTH < passwordLength) {
      throw new IllegalArgumentException(
              String.format(
                      "passwordLength must be greater than or equal to %d and less than or equal to %d",
                      MIN_LENGTH, MAX_LENGTH));
    }
    RuleResult result = PASSWORD_VALIDATOR_THREAD_LOCAL.get().validate(new PasswordData(password));
    return new Result(result.isValid(), estimate(password));
  }

  private static Strength estimate(String password)
  {
    int score = ESTIMATOR.measure(password).getScore();
    return switch (score) {
      case 0 -> Strength.WEAK;
      case 1 -> Strength.FAIR;
      case 2 -> Strength.GOOD;
      case 3 -> Strength.STRONG;
      case 4 -> Strength.VERY_STRONG;
      default -> throw new IllegalStateException(String.format("Unknow socre:%d",score));
    };
  }

  @Override
  public void close() {
    PASSWORD_GENERATOR_THREAD_LOCAL.remove();
    PASSWORD_VALIDATOR_THREAD_LOCAL.remove();
  }
}
