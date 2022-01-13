package com.silong.foundation.exception;

import com.silong.foundation.model.ErrorDetail;
import lombok.Getter;

/**
 * 服务异常
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-12 22:27
 */
public class ServiceException extends RuntimeException {
  /** 错误信息 */
  @Getter private final ErrorDetail errorDetail;

  /**
   * 构造方法
   *
   * @param errorDetail 错误详情
   */
  public ServiceException(ErrorDetail errorDetail) {
    super(errorDetail.toString());
    this.errorDetail = errorDetail;
  }
}
