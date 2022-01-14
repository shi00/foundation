package com.silong.foundation.constants;

import com.silong.foundation.model.ErrorDetail;
import lombok.Getter;

/**
 * 通用错误信息
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-14 09:16
 */
public enum CommonErrorCode {

  /** 服务内部错误 */
  SERVICE_INTERNAL_ERROR("%s.error.0001", "An internal error occurred in the %s.");

  /** 错误码 */
  @Getter private final String code;

  /** 错误提示 */
  @Getter private final String message;

  CommonErrorCode(String code, String message) {
    this.code = code;
    this.message = message;
  }

  /**
   * 根据服务名生成错误详情信息，供返回
   *
   * @param serviceName 服务名
   * @return 错误详情
   */
  public ErrorDetail format(String serviceName) {
    return ErrorDetail.builder()
        .errorCode(String.format(code, serviceName))
        .errorMessage(String.format(message, serviceName))
        .build();
  }
}
