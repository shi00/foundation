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

  CharacterRule digits = new CharacterRule(EnglishCharacterData.Digit);
  CharacterRule uppercase = new CharacterRule(EnglishCharacterData.UpperCase);
  CharacterRule lowercase = new CharacterRule(EnglishCharacterData.LowerCase);
  CharacterRule special =
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
    return passwordGenerator.generatePassword(length, digits, uppercase, lowercase, special);
  }
}
