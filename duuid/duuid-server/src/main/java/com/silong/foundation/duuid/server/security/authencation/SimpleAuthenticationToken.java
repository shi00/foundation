package com.silong.foundation.duuid.server.security.authencation;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import static com.google.common.collect.Lists.newArrayList;

/**
 * 简介
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-16 11:30
 */
public class SimpleAuthenticationToken extends AbstractAuthenticationToken {

  /** 请求头签名信息 */
  @Getter private final String signature;

  /** 请求身份标识 */
  @Getter private final String identifier;

  /** 时间戳 */
  @Getter private final String timestamp;

  /** 随机内容 */
  @Getter private final String random;

  /**
   * 构造方法
   *
   * @param signature 请求头签名
   * @param identifier 请求身份标识
   * @param timestamp 请求时间戳
   * @param random 随机字符串
   * @param authority 授权信息
   */
  public SimpleAuthenticationToken(
      String signature,
      String identifier,
      String timestamp,
      String random,
      GrantedAuthority authority) {
    super(newArrayList(authority));
    this.signature = signature;
    this.identifier = identifier;
    this.timestamp = timestamp;
    this.random = random;
  }

  @Override
  public Object getCredentials() {
    return null;
  }

  @Override
  public Object getPrincipal() {
    return null;
  }
}
