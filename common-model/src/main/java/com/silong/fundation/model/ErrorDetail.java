package com.silong.fundation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 错误信息
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-03 11:10
 */
@Data
@Builder
@Accessors(fluent = true)
public class ErrorDetail {
  /** 错误码 */
  @JsonProperty("error_code")
  private final String errorCode;
  /** 错误描述 */
  @JsonProperty("error_code")
  private final String errorMessage;
}
