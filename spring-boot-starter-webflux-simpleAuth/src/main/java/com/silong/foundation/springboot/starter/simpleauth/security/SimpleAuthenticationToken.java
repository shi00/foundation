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
package com.silong.foundation.springboot.starter.simpleauth.security;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;

/**
 * 简介
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-16 11:30
 */
@Getter
@Setter
@Accessors(fluent = true)
public class SimpleAuthenticationToken extends AbstractAuthenticationToken {

  /** 请求头签名信息 */
  private String signature;

  /** 请求身份标识 */
  private String identifier;

  /** 时间戳 */
  private String timestamp;

  /** 随机内容 */
  private String random;

  /**
   * 构造方法
   *
   * @param authorities 授权列表
   */
  public SimpleAuthenticationToken(Collection<SimpleGrantedAuthority> authorities) {
    super(authorities);
  }

  /**
   * 构造方法
   *
   * @param signature 请求头签名
   * @param identifier 请求身份标识
   * @param timestamp 请求时间戳
   * @param random 随机字符串
   * @param authorities 授权列表
   */
  public SimpleAuthenticationToken(
      String signature,
      String identifier,
      String timestamp,
      String random,
      Collection<SimpleGrantedAuthority> authorities) {
    super(authorities);
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
