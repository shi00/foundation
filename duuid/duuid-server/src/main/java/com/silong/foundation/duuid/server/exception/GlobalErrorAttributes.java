package com.silong.foundation.duuid.server.exception;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.util.Map;

/**
 * 全局异常属性
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-13 00:05
 */
@Component
public class GlobalErrorAttributes extends DefaultErrorAttributes {

  static final String EXCEPTION_KEY = "exception";

  @Override
  public Map<String, Object> getErrorAttributes(
      ServerRequest request, ErrorAttributeOptions options) {
    Map<String, Object> map = super.getErrorAttributes(request, options);
    map.put(EXCEPTION_KEY, getError(request));
    return map;
  }
}
