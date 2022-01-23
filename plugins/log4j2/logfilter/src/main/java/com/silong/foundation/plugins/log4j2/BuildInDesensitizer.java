package com.silong.foundation.plugins.log4j2;

import java.util.Arrays;

/**
 * 内置脱敏器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-21 23:17
 */
public enum BuildInDesensitizer implements Desensitizer {

  /** 中国一二代身份证号码 */
  CHINESE_ID_CARD_NUMBER(
      "([1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx])|"
          + "([1-9]\\d{5}\\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\\d{3})"),

  /**
   * 中国手机号
   *
   * <pre>
   *  电信：133,149,153,173,174,177,180,181,189,191,193,199
   *  移动：134,135,136,137,138,139,147,148,150,151,152,157,158,159,172,178,182,183,184,187,188,195,198
   *  联通：130,131,132,145,146,155,156,166,175,176,185,186,196
   *  广电：190,192,197
   *  电信虚拟：162,1700,1701,1702
   *  移动虚拟：165,1703,1705,1706
   *  联通虚拟：167,1704,1707,1708,1709,171
   * </pre>
   */
  CHINESE_PHONE_NUMBER("1(3\\d|4[5-9]|5[0-35-9]|6[567]|7[0-8]|8\\d|9[0-35-9])\\d{8}"),

  /** 国际电话号码 */
  INTERNATIONAL_PHONE_NUMBER(
      "(\\+\\d{1,3}( )?)?((\\(\\d{3}\\))|\\d{3})[- .]?\\d{3}[- .]?\\d{4}"
          + "|(\\+\\d{1,3}( )?)?(\\d{3}[ ]?){2}\\d{3}"
          + "|(\\+\\d{1,3}( )?)?(\\d{3}[ ]?)(\\d{2}[ ]?){2}\\d{2}"),

  /** IMEI */
  IMEI("\\d{15}(,\\d{15})*"),

  /**
   * 密码
   *
   * <pre>
   * (?=.*[0-9]) represents a digit must occur at least once.
   * (?=.*[a-z]) represents a lower case alphabet must occur at least once.
   * (?=.*[A-Z]) represents an upper case alphabet that must occur at least once.
   * (?=.*[!"#$%&'()*+,-./:;<=>?@[\]^_`{|}~] represents a special character that must occur at least once.
   * (?=\\S+$) white spaces don’t allowed in the entire string.
   * .{8, 20} represents at least 8 characters and at most 20 characters.
   * </pre>
   */
  PASSWORD(
      "(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!\"#\\$%&'\\(\\)\\*\\+,-\\./:;<=>\\?@\\[\\]^_`\\{\\|\\}~])(?=\\S+$).{8,20}"),

  /** 加密信息 */
  SECURITY_BASE64("security:(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?"),

  /** 电邮地址 */
  EMAIL(
      "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])");

  private final RegexDesensitizer desensitizer;

  /**
   * 构造方法
   *
   * @param regex 正则表达式
   */
  BuildInDesensitizer(String regex) {
    this.desensitizer = new RegexDesensitizer(regex);
  }

  @Override
  public String desensitize(String msg) {
    return desensitizer.desensitize(msg);
  }

  @Override
  public String id() {
    return desensitizer.id();
  }

  /**
   * 获取内置脱敏器构成的组合脱敏器
   *
   * @return 内置组合脱敏器
   */
  public static ComposeDesensitizer getInstance() {
    return new ComposeDesensitizer(
        Arrays.stream(values()).map(e -> e.desensitizer).toArray(Desensitizer[]::new));
  }
}
