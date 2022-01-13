package com.silong.foundation.duuid.server.model;

import lombok.Builder;
import lombok.Data;

/**
 * duuid结果
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-13 13:17
 */
@Data
@Builder
public class Duuid {
  /** 生产的duuid */
  private String id;
}
