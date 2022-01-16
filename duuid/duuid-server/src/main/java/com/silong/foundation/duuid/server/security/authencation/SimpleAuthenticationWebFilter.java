package com.silong.foundation.duuid.server.security.authencation;

import com.silong.foundation.duuid.server.configure.properties.DuuidServerProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;

/**
 * 简单鉴权过滤器，对请求头内信息进行签名认证
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-16 10:36
 */
@Slf4j
public class SimpleAuthenticationWebFilter extends AuthenticationWebFilter {

  /**
   * 构造方法
   *
   * @param properties 配置
   */
  public SimpleAuthenticationWebFilter(DuuidServerProperties properties) {
    super(new SimpleReactiveAuthenticationManager(properties));
    setAuthenticationSuccessHandler(SimpleServerAuthenticationSuccessHandler.getInstance());
    setAuthenticationFailureHandler(SimpleServerAuthenticationFailureHandler.getInstance());
    setServerAuthenticationConverter(new SimpleServerAuthenticationConverter(properties));
  }
}
