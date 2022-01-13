package com.silong.foundation.duuid.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * duuid结果
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-13 13:17
 */
@Data
@AllArgsConstructor
public class Duuid {
  /** 生成的duuid */
  @JsonProperty private String id;
}
