package com.silong.foundation.springboot.starter.simpleauth.constants;

/**
 * 鉴权请求头名称常量
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-18 17:04
 */
public interface AuthHeaders {
  /** 身份标识 */
  String IDENTITY = "Identity";
  /** 随机内容 */
  String RANDOM = "Random";
  /** 时间戳 */
  String TIMESTAMP = "Timestamp";
  /** 签名 */
  String SIGNATURE = "Signature";
}
