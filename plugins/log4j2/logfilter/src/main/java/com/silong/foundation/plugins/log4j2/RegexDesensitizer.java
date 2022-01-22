package com.silong.foundation.plugins.log4j2;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

import java.util.regex.Pattern;

/**
 * 正则表达式脱敏
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-21 22:03
 */
@Data
public class RegexDesensitizer implements Desensitizer {

  /** 正则表达式 */
  @EqualsAndHashCode.Exclude @ToString.Exclude private final Pattern pattern;

  /** 替换符 */
  private final String replacement;

  /** 正则表达式 */
  private final String regex;

  /**
   * 构造方法
   *
   * @param regex 正则表达式
   * @param replacement 敏感信息替换字符串
   */
  public RegexDesensitizer(@NonNull String regex, @NonNull String replacement) {
    this.pattern = Pattern.compile(regex);
    this.replacement = replacement;
    this.regex = regex;
  }

  /**
   * 构造方法
   *
   * @param regex 正则表达式
   */
  public RegexDesensitizer(String regex) {
    this(regex, DEFAULT_REPLACE_STR);
  }

  @Override
  public String desensitize(@NonNull String msg) {
    return pattern.matcher(msg).replaceAll(replacement);
  }

  @Override
  public String id() {
    return regex;
  }
}
