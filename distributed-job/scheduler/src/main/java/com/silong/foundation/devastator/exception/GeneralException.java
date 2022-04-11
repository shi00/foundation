package com.silong.foundation.devastator.exception;

import java.io.Serial;

/**
 * 通用运行时异常
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-10 16:36
 */
public class GeneralException extends RuntimeException {

  @Serial private static final long serialVersionUID = 0L;

  /** 构造方法 */
  public GeneralException() {
    super();
  }

  /**
   * 构造方法
   *
   * @param message 异常消息
   */
  public GeneralException(String message) {
    super(message);
  }

  /**
   * 构造方法
   *
   * @param message 异常消息
   * @param cause 异常
   */
  public GeneralException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * 构造方法
   *
   * @param cause 异常
   */
  public GeneralException(Throwable cause) {
    super(cause);
  }
}
