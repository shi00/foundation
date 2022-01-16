package com.silong.foundation.duuid.server.security.authencation;

import com.silong.foundation.duuid.server.configure.properties.DuuidServerProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.www.NonceExpiredException;
import reactor.core.publisher.Mono;

import static com.silong.foundation.crypto.digest.HmacToolkit.hmacSha256;

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
  private final DuuidServerProperties properties;

  /**
   * 构造方法
   *
   * @param properties 配置
   */
  public SimpleReactiveAuthenticationManager(DuuidServerProperties properties) {
    this.properties = properties;
  }

  @Override
  public Mono<Authentication> authenticate(Authentication authentication) {
    SimpleAuthenticationToken token = (SimpleAuthenticationToken) authentication;
    String timestamp = token.getTimestamp();
    long now = System.currentTimeMillis();
    long occur = Long.parseLong(timestamp);
    int acceptableTimeDiffMills = properties.getAcceptableTimeDiffMills();
    if (Math.abs(now - occur) > acceptableTimeDiffMills) {
      throw new NonceExpiredException(
          String.format(
              "The time difference between the request client and the server exceeds %dms",
              acceptableTimeDiffMills));
    }

    String identifier = token.getIdentifier();
    String signature = token.getSignature();
    String random = token.getRandom();
    if (StringUtils.equals(
        signature, hmacSha256(identifier + timestamp + random, properties.getWorkKey()))) {
      authentication.setAuthenticated(true);
      return Mono.just(authentication);
    }

    throw new BadCredentialsException("The client request signature was tampered.");
  }
}
