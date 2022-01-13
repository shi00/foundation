package com.silong.foundation.duuid.server.exception;

import com.silong.foundation.model.ErrorDetail;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * 全局异常
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-12 23:41
 */
public class GlobalException extends ResponseStatusException {

  /** 错误详情 */
  @Getter private final ErrorDetail errorDetail;

  /**
   * 构造方法
   *
   * @param status 错误码
   * @param errorDetail 错误详情
   */
  public GlobalException(@NonNull HttpStatus status, @NonNull ErrorDetail errorDetail) {
    super(status, errorDetail.toString());
    this.errorDetail = errorDetail;
  }

  /**
   * 构造方法
   *
   * @param status 错误码
   * @param errorDetail 错误详情
   * @param t 异常
   */
  public GlobalException(
      @NonNull HttpStatus status, @NonNull ErrorDetail errorDetail, @NonNull Throwable t) {
    super(status, errorDetail.toString(), t);
    this.errorDetail = errorDetail;
  }
}
