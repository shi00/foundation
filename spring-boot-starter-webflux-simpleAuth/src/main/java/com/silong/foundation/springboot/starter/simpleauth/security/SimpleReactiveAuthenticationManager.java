package com.silong.foundation.springboot.starter.simpleauth.security;

import com.silong.foundation.springboot.starter.simpleauth.configure.properties.SimpleAuthProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

import static com.silong.foundation.crypto.digest.HmacToolkit.hmacSha256;
import static com.silong.foundation.springboot.starter.simpleauth.constants.AuthHeaders.*;
import static org.apache.commons.lang3.StringUtils.isAnyEmpty;

/**
 * 鉴权管理器，对请求头签名进行鉴权
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-16 11:46
 */
@Slf4j
public class SimpleReactiveAuthenticationManager implements ReactiveAuthenticationManager {

  /** 配置信息 */
  private final SimpleAuthProperties properties;

  /**
   * 构造方法
   *
   * @param properties 配置
   */
  public SimpleReactiveAuthenticationManager(SimpleAuthProperties properties) {
    this.properties = properties;
  }

  @Override
  public Mono<Authentication> authenticate(Authentication authentication) {
    if (authentication.isAuthenticated() || authentication.getAuthorities().isEmpty()) {
      return Mono.just(authentication);
    }

    SimpleAuthenticationToken token = (SimpleAuthenticationToken) authentication;
    String identifier = token.identifier();
    String signature = token.signature();
    String random = token.random();
    String timestamp = token.timestamp();
    if (isAnyEmpty(identifier, signature, random, timestamp)) {
      throw new BadCredentialsException(
          String.format(
              "The request header must contain the following contents [%s, %s, %s, %s]",
              SIGNATURE, TIMESTAMP, RANDOM, IDENTITY));
    }

    long now = System.currentTimeMillis();
    long occur = Long.parseLong(timestamp);
    int acceptableTimeDiffMills = properties.getAcceptableTimeDiffMills();
    if (Math.abs(now - occur) > acceptableTimeDiffMills) {
      throw new BadCredentialsException(
          String.format(
              "The time difference between the request client and the server exceeds %dms",
              acceptableTimeDiffMills));
    }

    if (StringUtils.equals(
        signature, hmacSha256(identifier + timestamp + random, properties.getWorkKey()))) {
      authentication.setAuthenticated(true);
      return Mono.just(authentication);
    }

    throw new BadCredentialsException("The client request signature was tampered.");
  }
}
